package ai.antop.index.batch

import ai.antop.index.config.AppProperties
import ai.antop.index.domain.Site
import ai.antop.index.repository.SiteRepository
import ai.antop.index.service.NginxConfParser
import ai.antop.index.service.ScreenshotService
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime

@Component
class SiteBatchJob(
    private val appProperties: AppProperties,
    private val nginxConfParser: NginxConfParser,
    private val screenshotService: ScreenshotService,
    private val siteRepository: SiteRepository,
) {
    private val logger = LoggerFactory.getLogger(SiteBatchJob::class.java)

    @Scheduled(initialDelay = 1_000, fixedDelay = 3_600_000)
    fun run() {
        logger.info("Starting site batch job")

        val configFile = File(appProperties.nginxConfPath)
        if (!configFile.exists()) {
            logger.warn("nginx.conf not found: ${appProperties.nginxConfPath}")
            return
        }

        val paths = nginxConfParser.parseUrls(configFile.readText())
        val urls = paths.map { "${appProperties.baseUrl}/$it" }
        logger.info("Found ${urls.size} URLs in nginx.conf")

        val existingUrls = siteRepository.findAllUrls()
        val toDisable = existingUrls - urls.toSet()
        if (toDisable.isNotEmpty()) {
            siteRepository.disableByUrls(toDisable, LocalDateTime.now())
            logger.info("Disabled ${toDisable.size} sites")
        }

        for (url in urls) {
            try {
                processUrl(url)
            } catch (e: Exception) {
                logger.error("Failed to process $url", e)
            }
        }

        logger.info("Site batch job completed")
    }

    private fun processUrl(url: String) {
        val name = fetchTitle(url)
        val thumbnailUuid = screenshotService.takeScreenshot(url)
        val now = LocalDateTime.now()
        val existing = siteRepository.findByUrl(url)
        val site =
            existing?.copy(
                name = name,
                thumbnailUuid = thumbnailUuid ?: existing.thumbnailUuid,
                enabled = true,
                updatedAt = now,
            )
                ?: Site(url = url, name = name, thumbnailUuid = thumbnailUuid, createdAt = now, updatedAt = now)
        siteRepository.save(site)
        logger.info("Processed: $url (name=$name)")
    }

    private fun fetchTitle(url: String): String? =
        try {
            Jsoup
                .connect(url)
                .timeout(10_000)
                .get()
                .title()
                .takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            logger.warn("Failed to fetch title from $url: ${e.message}")
            null
        }
}

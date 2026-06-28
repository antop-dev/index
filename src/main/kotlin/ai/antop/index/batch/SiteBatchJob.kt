package ai.antop.index.batch

import ai.antop.index.config.AppProperties
import ai.antop.index.domain.Site
import ai.antop.index.repository.SiteRepository
import ai.antop.index.service.NginxConfParser
import ai.antop.index.service.ScreenshotService
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
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

        val locations = nginxConfParser.parseLocations(configFile.readText())
        val urls =
            locations.map {
                UriComponentsBuilder
                    .fromUriString(appProperties.baseUrl)
                    .path(it)
                    .toUriString()
                    .trimEnd('/')
            }
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
        val (name, description, icon) = fetchSiteInfo(url)
        val thumbnailUuid = screenshotService.takeScreenshot(url)
        val now = LocalDateTime.now()
        val existing = siteRepository.findByUrl(url)
        if (existing != null && thumbnailUuid != null && existing.thumbnailUuid != null) {
            val oldFile = File(appProperties.thumbnailsDir, "${existing.thumbnailUuid}.png")
            try {
                if (!oldFile.delete()) {
                    logger.warn("Failed to delete old thumbnail: ${oldFile.path}")
                }
            } catch (e: Exception) {
                logger.warn("Failed to delete old thumbnail: ${oldFile.path}", e)
            }
        }
        if (existing != null) { // 기존 url이 있으면 update
            existing.name = name
            existing.description = description
            existing.icon = icon
            thumbnailUuid?.let { existing.thumbnailUuid = it }
            existing.enabled = true
            existing.updatedAt = now
        } else { // 없으면 insert
            val site =
                Site(
                    url = url,
                    name = name,
                    description = description,
                    icon = icon,
                    thumbnailUuid = thumbnailUuid,
                    createdAt = now,
                    updatedAt = now,
                )
            siteRepository.save(site)
        }
        logger.info("Processed: $url (name=$name, icon=$icon)")
    }

    private data class SiteInfo(
        val name: String?,
        val description: String?,
        val icon: String?,
    )

    private fun fetchSiteInfo(url: String): SiteInfo =
        try {
            val doc =
                Jsoup
                    .connect(url)
                    .timeout(3_000)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "ko,en-US;q=0.9,en;q=0.8")
                    .get()
            val name = doc.title().takeIf { it.isNotBlank() }
            val description = doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
            val icon =
                doc
                    .select("link[rel~=(?i)^(shortcut )?icon$]")
                    .firstOrNull()
                    ?.absUrl("href")
                    ?.ifBlank { null }
                    ?: doc
                        .select("link[rel=apple-touch-icon]")
                        .firstOrNull()
                        ?.absUrl("href")
                        ?.ifBlank { null }
            SiteInfo(name, description, icon)
        } catch (e: Exception) {
            logger.warn("Failed to fetch site info from $url: ${e.message}")
            SiteInfo(null, null, null)
        }
}

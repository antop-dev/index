package ai.antop.index.service

import ai.antop.index.config.AppProperties
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class ScreenshotService(
    appProperties: AppProperties,
) {
    private val dir: Path = Path.of(appProperties.thumbnailsDir).also { Files.createDirectories(it) }

    fun takeScreenshot(url: String): String? {
        val uuid = UUID.randomUUID().toString()
        val outputPath = dir.resolve("$uuid.png")
        logger.info { "Taking screenshot: url=$url, output=$outputPath" }
        return try {
            Playwright.create().use { playwright ->
                playwright
                    .chromium()
                    .launch(
                        BrowserType.LaunchOptions().setArgs(
                            listOf("--no-sandbox", "--disable-setuid-sandbox", "--lang=ko-KR"),
                        ),
                    ).use { browser ->
                        browser
                            .newContext(
                                Browser
                                    .NewContextOptions()
                                    .setLocale("ko-KR")
                                    .setExtraHTTPHeaders(mapOf(HttpHeaders.ACCEPT_LANGUAGE to "ko,en-US;q=0.9,en;q=0.8")),
                            ).use { context ->
                                context.newPage().use { page ->
                                    logger.debug { "Navigating to $url" }
                                    val response = page.navigate(url, Page.NavigateOptions().setTimeout(30_000.0))
                                    logger.debug { "Navigation complete: status=${response?.status()}" }

                                    logger.debug { "Waiting for network idle" }
                                    page.waitForLoadState(LoadState.NETWORKIDLE)
                                    logger.debug { "Network idle reached" }

                                    page.screenshot(Page.ScreenshotOptions().setPath(outputPath))
                                    logger.info { "Screenshot saved: $outputPath" }
                                }
                            }
                    }
            }
            uuid
        } catch (e: Exception) {
            logger.error(e) { "Failed to take screenshot of $url" }
            null
        }
    }
}

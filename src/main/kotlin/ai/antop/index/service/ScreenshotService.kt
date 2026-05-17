package ai.antop.index.service

import ai.antop.index.config.AppProperties
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Service
class ScreenshotService(
    appProperties: AppProperties,
) {
    private val logger = LoggerFactory.getLogger(ScreenshotService::class.java)
    private val dir: Path = Path.of(appProperties.thumbnailsDir).also { Files.createDirectories(it) }

    fun takeScreenshot(url: String): String? =
        try {
            val uuid = UUID.randomUUID().toString()
            Playwright.create().use { playwright ->
                playwright
                    .chromium()
                    .launch(
                        BrowserType.LaunchOptions().setArgs(listOf("--no-sandbox", "--disable-setuid-sandbox")),
                    ).use { browser ->
                        browser.newPage().use { page ->
                            page.navigate(url, Page.NavigateOptions().setTimeout(30_000.0))
                            page.waitForLoadState(LoadState.NETWORKIDLE)
                            page.screenshot(Page.ScreenshotOptions().setPath(dir.resolve("$uuid.png")))
                        }
                    }
            }
            uuid
        } catch (e: Exception) {
            logger.error("Failed to take screenshot of $url: ${e.message}")
            null
        }
}

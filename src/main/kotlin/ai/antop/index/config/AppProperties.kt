package ai.antop.index.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val nginxConfPath: String = "/etc/nginx/nginx.conf",
    val thumbnailsDir: String = "./thumbnails",
    val baseUrl: String = "https://p.antop.ai",
)

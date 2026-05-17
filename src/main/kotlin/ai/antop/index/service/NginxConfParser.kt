package ai.antop.index.service

import org.springframework.stereotype.Component

@Component
class NginxConfParser {
    private val regex = Regex("""location\s+(\S+)\s*\{""")

    fun parseUrls(content: String): List<String> =
        regex
            .findAll(content)
            .map { it.groupValues[1] }
            .filter { it != "/" }
            .toList()
}

package ai.antop.index.service

import org.springframework.stereotype.Component

@Component
class NginxConfParser {
    private val regex = Regex("""location\s+(\S+)\s*\{""")

    fun parseLocations(content: String): List<String> =
        regex
            .findAll(content)
            .map { it.groupValues[1] }
            .filter { it.isBlank() || it != "/" }
            .toList()
}

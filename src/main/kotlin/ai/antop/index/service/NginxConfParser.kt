package ai.antop.index.service

import org.springframework.stereotype.Component

@Component
class NginxConfParser {
    private val regex = Regex("""location\s+(\S+)\s*\{""")

    fun parseLocations(content: String): List<String> {
        val lines = content.lines()
        return lines.indices.mapNotNull { i ->
            val match = regex.find(lines[i]) ?: return@mapNotNull null
            val prevLine = if (i > 0) lines[i - 1] else ""
            if ("*hide*" in prevLine) null else match.groupValues[1]
        }.filter { it.isBlank() || it != "/" }
    }
}

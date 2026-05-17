package ai.antop.index.controller

import ai.antop.index.config.AppProperties
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.nio.file.Path
import java.util.UUID

@Controller
class ThumbnailController(
    private val appProperties: AppProperties,
) {
    @GetMapping("/thumbnail/{uuid}.png")
    @ResponseBody
    fun thumbnail(
        @PathVariable uuid: String,
    ): ResponseEntity<ByteArray> {
        try {
            UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.notFound().build()
        }

        val file = Path.of(appProperties.thumbnailsDir).resolve("$uuid.png").toFile()
        if (!file.exists()) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(file.readBytes())
    }
}

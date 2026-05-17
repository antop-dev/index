package ai.antop.index.controller

import ai.antop.index.repository.SiteRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController(
    private val siteRepository: SiteRepository,
) {
    @GetMapping("/")
    fun index(model: Model): String {
        model.addAttribute("sites", siteRepository.findAllByEnabledTrueOrderByName())
        return "index"
    }
}

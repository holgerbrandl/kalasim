package org.kalasim.webui

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

/**
 * @author Holger Brandl
 */

@Controller
class IndexController {

    val host = "localhost"

    @RequestMapping("/")
    fun index(model: Model): String {
//        model.addAttribute("grafana", "http://$host:3000/d/eFkjY7FGk")
        model.addAttribute("config", "config.html")
        model.addAttribute("docs", "docs.html")
        return "index"
    }


}
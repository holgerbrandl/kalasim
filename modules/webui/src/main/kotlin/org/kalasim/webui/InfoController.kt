package org.kalasim.webui

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Holger Brandl
 */
@Controller
class InfoController {

    private val initDate: String;

    init {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        initDate = simpleDateFormat.format(Date()).toString();
    }

    @RequestMapping("/info", "/info.html")
    fun info(model: Model): String {


        val info = mapOf(
                Pair("Version:", "0.3.3"),
        )

        model.addAttribute("info", info.toList())

        return "info"
    }
}
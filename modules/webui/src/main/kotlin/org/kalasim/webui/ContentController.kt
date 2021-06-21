package org.kalasim.webui

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam


@Controller
@RequestMapping("content")
class ContentController {
    @RequestMapping("")
    fun loadContent(model: Model): String {
        model.addAttribute("simControl", SimControl(1.3, 10))
        return "website"
    }

    @get:RequestMapping("content1")
    val content1: String
        get() = "content :: content1"

    @get:RequestMapping("content2")
    val content2: String
        get() = "content :: content2"

    @RequestMapping("content3")
    fun content3(model: Model): String {
        model.addAttribute("counter", System.nanoTime().toString())

        return "content :: content3"
    }


    @RequestMapping("content4")
    fun content4(model: Model): String {
        model.addAttribute("counter", System.nanoTime().toString())
        model.addAttribute("simState", SimState(13.3, listOf(ComponentState("foo"), ComponentState("bar"))))

        return "content :: content4"
    }

//    https://frontbackend.com/thymeleaf/spring-boot-bootstrap-thymeleaf-slider
//    @GetMapping
//    fun main(model: Model): String {
//        model.addAttribute("simControl", SimControl(1.3, 10))
//        return "index"
//    }

    @PostMapping
    fun save(simControl: SimControl?, model: Model): String {
        model.addAttribute("simControl", simControl)
        return "saved"
    }

    @GetMapping("speedChange")
    fun speedChange(@RequestParam speed: Double?) {
        println("new sim-speed is $speed")
    }
}

data class ComponentState(val name: String)
data class SimState(val now:Double, val states: List<ComponentState>)
data class SimControl(val speed: Double, val numGenerations: Int)

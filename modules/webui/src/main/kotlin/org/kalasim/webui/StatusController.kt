package org.kalasim.webui

import org.kalasim.ClockSync
import org.kalasim.Component
import org.kalasim.ComponentState
import org.koin.core.component.get
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam


@Controller
@RequestMapping("status", "status.html")
class StatusController(val simProvider: SimProvider) {

    @RequestMapping
    fun loadContent(model: Model): String {
        model.addAttribute("simControl", SimControl(1.3, 10))
        return "status"
    }

//    @get:RequestMapping("content1")
//    val content1: String
//        get() = "status_fragments :: content1"
//
//    @get:RequestMapping("content2")
//    val content2: String
//        get() = "status_fragments :: content2"

    @RequestMapping("sim_time")
    fun content3(model: Model): String {
        model.addAttribute("counter", simProvider.sim.now)

        return "status_fragments :: sim_time"
    }


    @RequestMapping("comp_state")
    fun content4(model: Model): String {
//        model.addAttribute("counter", System.nanoTime().toString())

        val simState: SimState = simProvider.currentState
        model.addAttribute("simState", simState)
        model.addAttribute("sim", simProvider.sim)

        return "status_fragments :: comp_state"
    }


    @PostMapping
    fun save(simControl: SimControl?, model: Model): String {
        model.addAttribute("simControl", simControl)
        return "saved"
    }

    @GetMapping("speedChange")
    fun speedChange(@RequestParam speed: Double?) {
        val clockSync = simProvider.sim.get<ClockSync>()
        clockSync.speedUp
        println("new sim-speed is $speed")
    }
}

data class NamedState(val name: String, val state: ComponentState)
data class SimState(val now: Double, val states: List<NamedState>)
data class SimControl(val speed: Double, val numGenerations: Int)

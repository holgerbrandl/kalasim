package org.kalasim.webui

import org.kalasim.ClockSync
import org.kalasim.demo.MM1Queue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.io.File
import java.time.Duration
import javax.annotation.PostConstruct


/**
 * @author Holger Brandl
 */
@SpringBootApplication(scanBasePackages = ["org.kalasim.webui"])
open class SimUI

fun main(args: Array<String>) {

    val sim = MM1Queue()
    SimInterface(sim)

    sim.apply {
        ClockSync(tickDuration = Duration.ofSeconds(1))
        run(Double.MAX_VALUE)
    }

}

class SimInterface(val sim: MM1Queue) {

    init {
        SpringApplicationBuilder().sources(SimUI::class.java).properties(mapOf("server.port" to 8080))
//            .lazyInitialization(true)
            .initializers(ApplicationContextInitializer { applicationContext: ConfigurableApplicationContext? ->
//                https://stackoverflow.com/questions/4540713/add-bean-programmatically-to-spring-web-app-context
                val beanFactory = (applicationContext as ConfigurableApplicationContext).beanFactory
                beanFactory.registerSingleton(MM1Queue::class.java.getCanonicalName(), sim)
            })
            .run()
    }
}

@Component
class SimProvider(@Suppress("SpringJavaInjectionPointsAutowiringInspection") val sim: MM1Queue) {

//    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
//    @Autowired
//    ? = null

    val currentState: SimState
//        get() = SimState(13.3, listOf(ComponentState("foo"), ComponentState("bar")))
        get() = SimState(13.3, sim.queue.map{ NamedState(it.name, it.status) }.toList())

    // https://stackoverflow.com/questions/23513045/how-to-check-if-a-process-is-running-inside-docker-container
    private val isDockerized = File("/.dockerenv").isFile

    @PostConstruct
    fun postConstruct() {
        println("sim is $sim")
    }
}

//open class SimWrapper(val sim: MM1Queue)

package org.kalasim

import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

class ClockSync(
    tickDuration: Duration,
//    val origin: Instant = Instant.now(),
    val speedUp: Double = 1.0,
    syncsPerTick: Number = 1,
    koin: Koin = GlobalContext.get()
) : Component(koin = koin) {


    val tickMs = tickDuration.toMillis().toDouble()
    val holdTime = 1.0 / syncsPerTick.toDouble()

    //https://stackoverflow.com/questions/32437550/whats-the-difference-between-instant-and-localdatetime
    lateinit var simStart: Instant

    override fun process() = sequence {
//        if (!this@ClockSync::simStart.isInitialized) {
        simStart = Instant.now()
//        }

        while (true) {
            //wait until we have caught up with wall clock
            val simTimeSinceStart = Duration.between(simStart, simStart.plusMillis((tickMs * env.now).roundToLong()));
            val wallTimeSinceStart = Duration.between(simStart, Instant.now())

            println("sim $simTimeSinceStart wall $wallTimeSinceStart")
            // simulation is faster if value is larger
            if (simTimeSinceStart > wallTimeSinceStart) {
                // if so wait accordingly
                val sleepDuration = simTimeSinceStart - wallTimeSinceStart
                Thread.sleep(sleepDuration.toMillis())
            }

            // wait for until the next sync event
            hold(holdTime)
        }
    }
}

fun main() {
    val timeBefore = System.currentTimeMillis()

    createSimulation(true) {
        object : Component() {
            override fun process() = sequence<Component> {
                hold(1)
                printTrace("awake again")
            }
        }

        ClockSync(Duration.ofSeconds(1))

        run(10)
    }

    println("time passed ${System.currentTimeMillis() - timeBefore})")
}
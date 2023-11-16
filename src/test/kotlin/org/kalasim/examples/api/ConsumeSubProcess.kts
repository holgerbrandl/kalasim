import org.kalasim.*
import kotlin.time.Duration.Companion.minutes

createSimulation {
    enableComponentLogger()

    object : Component() {

        override fun process() = sequence {
            hold(1.minute)
            // to consume the sub-process we use yieldAll
            yieldAll(subProcess())
            // it will continue here after the sub-process has been consumed
            hold(2.minutes)
        }

        fun subProcess(): Sequence<Component> = sequence {
            hold(3.minutes)
        }
    }

    run()
}

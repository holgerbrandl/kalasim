import org.kalasim.*

createSimulation {
    enableComponentLogger()

    object : Component() {

        override fun process() = sequence {
            hold(1)
            // to consume the sub-process we use yieldAll
            yieldAll(subProcess())
            // it will continue here after the sub-process has been consumed
            hold(1)
        }

        fun subProcess(): Sequence<Component> = sequence {
            hold(1)
        }
    }

    run()
}

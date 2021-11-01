import org.kalasim.Component
import org.kalasim.createSimulation
import org.kalasim.plot.letsplot.display

createSimulation(enableTickMetrics = true) {

    object : Component() {
        override fun process() = sequence {
            while(true) {
                // create some artificial non-linear compute load
                if(now.value < 7)
                    Thread.sleep((now.value * 100).toLong())
                else {
                    Thread.sleep(100)
                }

                hold(1)
            }
        }
    }

    run(10)

    tickMetrics.display().show()
}

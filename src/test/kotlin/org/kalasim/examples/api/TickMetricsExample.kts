import org.kalasim.*
import org.kalasim.plot.letsplot.display

createSimulation {
    enableTickMetrics()

    object : Component() {
        override fun process() = sequence {
            while(true) {
                // create some artificial non-linear compute load
                if(nowTT.value < 7)
                    Thread.sleep((nowTT.value * 100).toLong())
                else {
                    Thread.sleep(100)
                }

                hold(1.minutes)
            }
        }
    }

    run(10.hours)

    tickMetrics.display().show()
}

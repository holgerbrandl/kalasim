//ResourceSelection.kts
import org.kalasim.*
import org.kalasim.misc.repeat

createSimulation {
    val resources = List(3) { Resource() }

    // we could also mimic round-robin with infinite stream
    val resIter = resources.repeat().iterator()

    object : Component() {
        override fun process() = sequence {
            val r = resIter.next()
            request(r) {
                hold(10)
            }
        }
    }
}
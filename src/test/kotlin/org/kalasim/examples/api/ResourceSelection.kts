//ResourceSelection.kts
import org.kalasim.*
import org.kalasim.ResourceSelectionPolicy.ShortestQueue

createSimulation {
    enableComponentLogger()

    val doctors = List(3) { Resource() }

    class Patient : Component() {
        override fun process() = sequence {
            val requiredQuantity = 3

            val selected = selectResource(
                doctors,
                quantity = requiredQuantity,
                policy = ShortestQueue
            )

            request(selected withQuantity requiredQuantity) {
                hold(10)
            }
        }
    }

    ComponentGenerator(exponential(1).minutes) { Patient() }
    run(100)
}
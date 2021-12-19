//ResourceSelection.kts
import org.kalasim.*
import org.kalasim.ResourceSelectionPolicy.SHORTEST_QUEUE

createSimulation(false) {
    val doctors = List(3) { Resource() }

    class Patient : Component() {
        override fun process() = sequence {
            val requiredQuantity = 3

            val selected = selectResource(
                doctors,
                quantity = requiredQuantity,
                policy = SHORTEST_QUEUE
            )

            request(selected withQuantity requiredQuantity) {
                hold(10)
            }
        }
    }

    ComponentGenerator(exponential(1)) { Patient() }
    run(100)
}
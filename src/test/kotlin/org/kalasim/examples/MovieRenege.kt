//MovieRenege.kt
import org.kalasim.*

fun main() {

    val RANDOM_SEED = 158
    val TICKETS = 50  // Number of tickets per movie
    val SIM_TIME = 120.0  // Simulate until

    data class Movie(val name: String)

    val MOVIES = listOf("Julia Unchained", "Kill Process", "Pulp Implementation").map { Movie(it) }

    createSimulation(true, randomSeed = RANDOM_SEED) {

        // note: it's not really needed to model the theater (because it has no process), but we follow the julia model here
        val theater = object {
            val tickets =
                MOVIES.map { it to DepletableResource("room ${MOVIES.indexOf(it)}", capacity = TICKETS) }.toMap()
            val numReneged = MOVIES.map { it to 0 }.toMap().toMutableMap()
            val counter = Resource("counter", capacity = 1)
        }

        class Cineast(val movie: Movie, val numTickets: Int) : Component() {
            override fun process() = sequence {
                request(theater.counter) {
                    request(theater.tickets[movie]!! withQuantity numTickets, failAt = 0.tickTime)
                    if (failed) {
                        theater.numReneged.merge(movie, 1, Int::plus)
                    }
                }
            }
        }

        ComponentGenerator(iat = exponential(0.5)) {
            Cineast(MOVIES.random(), discreteUniform(1, 6).sample())
        }

        run(SIM_TIME)

        MOVIES.forEach { movie ->
            val numLeftQueue = theater.numReneged[movie]!!
            val soldOutSince = theater.tickets[movie]!!.occupancyMonitor.stepFun()
                // find the first time when tickets were sold out
                .first { it.second == 1.0 }.first.roundAny(2)

            println("Movie ${movie.name} sold out $soldOutSince minutes after ticket counter opening.")
            println("$numLeftQueue walked away after film was sold out.")
        }

//        // Visualize ticket sales
//        val plotData = theater.tickets.values.flatMap {
//            it.occupancyMonitor.stepFun().map { sf -> Triple(it.name, sf.first, sf.second) }
//        }
//
//        plotData.asDataFrame().plot(x = "second", y = "third")
//            .geomStep().facetWrap("first").title("Theater Occupancy")
//            .xLabel("Time (min)").yLabel("Occupancy")
    }
}
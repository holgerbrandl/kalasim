//CarWash.kt
import org.kalasim.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 *  A carwash has a limited number of washing machines and defines
 * a washing processes that takes some (random) time.
 *
 * Car processes arrive at the carwash at a random time. If one washing
 * machine is available, they start the washing process and wait for it
 * to finish. If not, they wait until they an use one.
 */
fun main() {

    val RANDOM_SEED = 42
    val NUM_MACHINES = 2  // Number of machines in the carwash
    val WASHTIME = 5.minutes      // Minutes it takes to clean a car
    val T_INTER = 7.minutes       // Create a car every ~7 minutes
    val T_INTER_SD = 2.minutes       // variance of inter-arrival
    val SIM_TIME = 20.days     // Simulation time

    class Car : Component() {
        override fun process() = sequence {
            val carWash = get<Resource>()
            request(carWash)
            hold(WASHTIME)
            release(carWash)
        }
    }


    val env = createSimulation(randomSeed = RANDOM_SEED) {
        dependency { Resource("carwash", NUM_MACHINES) }

        enableComponentLogger()

        //Create 4 initial cars
        repeat(3) { Car() }
        // Create more cars while the simulation is running
        ComponentGenerator(iat = uniform(T_INTER - T_INTER_SD, T_INTER + T_INTER_SD)) { Car() }
    }


    println("Carwash\n======\n")
    println("Check out http://youtu.be/fXXmeP9TvBg while simulating ... ;-)")

    // Start the simulation
    env.run(SIM_TIME)
}

//MachineShop.kt
import org.kalasim.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


val RANDOM_SEED: Int = 42
val PT_MEAN = 10.minutes // Avg. processing time in minutes
val PT_SIGMA = 2.minutes // Sigma of processing time
val MTTF = 300.minutes // Mean time to failure in minutes
val REPAIR_TIME = 30.minutes // Time it takes to repair a machine in minutes
val JOB_DURATION = 30.minutes // Duration of other jobs in minutes
val NUM_MACHINES: Int = 10   // Number of machines in the machine shop
val SIM_TIME = 28.days  // Simulation time

fun main() {

    class Machine : Component() {
        var madeParts: Int = 0
            private set

        val timePerPart: DurationDistribution = normal(PT_MEAN, PT_SIGMA)

        override fun process(): Sequence<Component> = sequence {
            while (true) {
                // start working on a new part
                log("building a new part")
                hold(timePerPart())
                log("finished building part")
                madeParts++
            }
        }
    }


    /** Break the machine every now and then. */
    class MachineWear(val machine: Machine, val repairMan: Resource) : Component(
        process = MachineWear::breakMachine
    ) {


        fun breakMachine(): Sequence<Component> = sequence {
            val timeToFailure = exponential(MTTF)

            while (true) {
                hold(timeToFailure())

                // handle the rare case that the model
                if (machine.isInterrupted) continue

                machine.interrupt()

                request(repairMan)
                hold(REPAIR_TIME)

                require(!isBumped(repairMan)) { "productive tools must not be bumped" }

                release(repairMan)

                machine.resume()
                require(!machine.isInterrupted) { "machine must not be interrupted at end of wear cycle" }
            }
        }
    }


    createSimulation(randomSeed = RANDOM_SEED) {
        enableComponentLogger()

        val repairMan = Resource("mechanic", preemptive = true)


        // create N machines and wear components
        val tools = (1..NUM_MACHINES).map {
            Machine().also { MachineWear(it, repairMan) }
        }

        // define the other jobs as object expression
        // https://kotlinlang.org/docs/reference/object-declarations.html#object-expressions
        object : Component("side jobs") {
            override fun process() = sequence {
                while (true) {
                    request(ResourceRequest(repairMan, priority = Priority(-1)))
                    hold(JOB_DURATION)

                    if (isBumped(repairMan)) {
                        log("other job was bumped")
                        continue
                    }

                    release(repairMan)
                }
            }
        }

        // Run simulation
        run(1000)
        run(SIM_TIME)

        // Analysis

        tools.forEach { println("${it.name} made ${it.madeParts} parts.") }
    }
}
//MachineShop.kt
import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.*


val RANDOM_SEED: Int = 42
val PT_MEAN: Double = 10.0 // Avg. processing time in minutes
val PT_SIGMA: Double = 2.0 // Sigma of processing time
val MTTF: Double = 300.0 // Mean time to failure in minutes
val REPAIR_TIME: Double = 30.0 // Time it takes to repair a machine in minutes
val JOB_DURATION: Double = 30.0 // Duration of other jobs in minutes
val NUM_MACHINES: Int = 10   // Number of machines in the machine shop
val WEEKS: Int = 4   // Simulation time in weeks
val SIM_TIME: Number = WEEKS * 7 * 24 * 60  // Simulation time in minutes

fun main() {

    class Machine : Component() {
        var madeParts: Int = 0
            private set

        val timePerPart: RealDistribution = normal(PT_MEAN, PT_SIGMA)

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


    createSimulation(true, randomSeed = RANDOM_SEED) {

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
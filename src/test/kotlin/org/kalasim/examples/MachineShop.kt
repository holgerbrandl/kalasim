//MachineShop.kt
import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.*


val RANDOM_SEED: Int = 42
val PT_MEAN: Double = 10.0 // Avg. processing time in minutes
val PT_SIGMA: Double = 2.0 // Sigma of processing time
val MTTF: Double = 300.0 // Mean time to failure in minutes
val BREAK_MEAN: Double = 1 / MTTF  // Param. for expovariate distribution
val REPAIR_TIME: Double = 30.0 // Time it takes to repair a machine in minutes
val JOB_DURATION: Double = 30.0 // Duration of other jobs in minutes
val NUM_MACHINES: Int = 10   // Number of machines in the machine shop
val WEEKS: Int = 4   // Simulation time in weeks
val SIM_TIME: Number = WEEKS * 7 * 24 * 60  // Simulation time in minutes

fun main() {

    class Machine : Component() {
        var madeParts: Int = 0
            private set

        val timePerPart: RealDistribution = normal(PT_MEAN, PT_SIGMA, env.rg)

        override fun process(): Sequence<Component> = sequence {
            while (true) {
                // start working on a new part
                printTrace("building a new part")
                yield(hold(timePerPart()))
                printTrace("finished building part")

            }
        }
    }


    /** Break the machine every now and then. */
    class MachineWear(val machine: Machine, val repairMan: Resource) : Component(
        process = MachineWear::breakMachine
    ) {

        val timeToFailure = exponential(BREAK_MEAN)

        fun breakMachine(): Sequence<Component> = sequence {

            while (true) {
                yield(hold(timeToFailure()))

//                if (!machine.isInterrrupted) {
                machine.interrupt()

                yield(request(repairMan))
                yield(hold(REPAIR_TIME))
                release(repairMan)

                machine.resume()
//                }
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
                    yield(request(ResourceRequest(repairMan, priority = 2)))
                    yield(hold(JOB_DURATION))

                    if (isBumped(repairMan)) {
                        printTrace("other job was bumped")
                        continue
                    }

                    release(repairMan)
                }
            }
        }

        // Run simulation
        run(SIM_TIME)

        // Analysis

        tools.forEach { println("${it.name} made ${it.madeParts} parts.") }
    }
}
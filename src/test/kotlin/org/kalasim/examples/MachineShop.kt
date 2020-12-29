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

    class Machine(repairMan: Resource) : Component() {
        var madeParts: Int = 0

        val timePerPart: RealDistribution = normal(PT_MEAN, PT_SIGMA, env.rg)


        override fun process(): Sequence<Component> = sequence {
            while (true) {
                // start workin on a new part
                val start = env.now

                yield(hold(timePerPart()))
//             if(isInterrrupted){
//                 val remainin
//             }


            }
        }
    }

    class OtherJobs(repairMan: Resource) : Component()

    createSimulation(true, randomSeed = RANDOM_SEED) {
        val timeToFailure = exponential(BREAK_MEAN)

        val repairMan = Resource(preemptive = true)


        // register all components of the simulation
        val tools = (1..NUM_MACHINES).map { Machine(repairMan) }

        OtherJobs(repairMan)

        // Run simulation
        run(SIM_TIME)

        // Analysis
        tools.forEach { println("${it.name} made ${it.madeParts} parts.") }
    }
}
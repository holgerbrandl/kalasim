package org.kalasim.examples.hospital

import org.kalasim.*
import org.kalasim.examples.hospital.PatientStatus.DeceasedWhileWaiting
import org.kalasim.examples.hospital.PatientStatus.Waiting
import org.kalasim.examples.hospital.Severity.*
import org.kalasim.monitors.LevelMonitor
import org.kalasim.monitors.NumericLevelMonitor
import org.kalasim.plot.letsplot.display
import org.koin.core.component.get
import org.koin.core.qualifier.named
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random


enum class InjuryType {
    AnimalBites, Bruises, Burns, Dislocations, Fractures, SprainsAndStrains, Cuts, Scratches
}

/** Severity as defined in https://en.wikipedia.org/wiki/Emergency_Severity_Index. */
enum class Severity {
    /** Stable, with no resources anticipated except oral or topical medications, or prescriptions  */
    NonUrgent,

    /** Stable, with only one type of resource anticipated (such as only an X-ray, or only sutures)  */
    LessUrgent,

    /** Stable, with multiple types of resources needed to investigate or treat (such as lab tests plus X-ray imaging)  */
    Urgent,

    /** High risk of deterioration, or signs of a time-critical problem  */
    Emergent,

    /** Immediate, life-saving intervention required without delay  */
    Resuscitation,
}

enum class PatientStatus {
    Waiting, InSurgery, Released, DeceasedWhileWaiting, DeceasedInSurgery;

    val deceased: Boolean
        get() = this != DeceasedInSurgery && this != DeceasedWhileWaiting
}

data class Patient(
    val type: InjuryType,
    val severity: State<Severity>,
    val patientStatus: State<PatientStatus>
) : Component() {


    override fun process() = sequence {
        suspend fun SequenceScope<Component>.holdExponential(meanTime: Number) {
            hold(exponential(meanTime).sample())
        }

        while (!patientStatus.value.deceased) {
            when (severity.value) {
                NonUrgent -> passivate()
                LessUrgent -> {
                    holdExponential(10)
                    updatePatient(Urgent)
                }
                Urgent -> {
                    holdExponential(5)
                    updatePatient(Emergent)
                }
                Emergent -> {
                    holdExponential(2)
                    updatePatient(Resuscitation)
                }
                Resuscitation -> {
                    holdExponential(1)

                    // patient did not receive treatment in time and did not make it
                    // a dead patient is removed from the queue
                    patientStatus.value = DeceasedWhileWaiting
                    get<HeadNurse>().waitingLine.remove(this@Patient)
                }
            }
        }
    }

    private fun updatePatient(newSeverity: Severity) {
        this.severity.value = newSeverity

        // to adjust the queue position, we need to remove and readd it to the queue
        get<HeadNurse>().waitingLine.updateOrderOf(this@Patient)
    }
}

/** Main actors. Rooms activly pull new work which is dispatched by the head-nurse. */
class Room(name: String, var setup: State<InjuryType>) : Component(name) {
    val headNurse by lazy { get<HeadNurse>() }

    override fun process() = sequence {
        val patient = get<HeadNurse>().nextOne()

        if (patient == null) {
            cancel(); return@sequence
        }

        // check setup state of room
        val injuryType = patient.type

        if (setup.value != injuryType) {
            hold(setupTimes[injuryType]!!, description = "preparing room ${this@Room} for $injuryType")
        }

        // find a qualified surgeon
        val doctors = get<EmergencyRoom>().doctors
            .filter { it.qualification.contains(injuryType) }

        // perform surgery one a qualified doctor becomes available
        request(doctors, oneOf = true) { doctor ->
            // will be in range [1, inf]
            val stressFactor = sqrt(headNurse.waitingLine.size.toDouble())
            val severityFactor = patient.severity.value.ordinal.toDouble().pow(0.6)

            // surgery time is a weighted by business in the ER and severity of the patient
            val surgeryTime = stressFactor * severityFactor * nonUrgentSurgeryTimes[injuryType]!!
            hold(surgeryTime, description = "Surgery of patient ${patient} in room ${this@Room} by doctor ${doctor}")

            // was it successful? This depends on the severity of the injury
            val isDeceased = surgerySuccessProbability[patient.severity.value]!! > env.random.nextDouble()
            if (isDeceased) {
                patient.patientStatus.value = PatientStatus.Released
            } else {
                patient.patientStatus.value = PatientStatus.DeceasedInSurgery
            }

            log("surgery of ${patient} completed ${if (isDeceased) "with" else "without"} success")
        }
    }
}


val setupTimes = InjuryType.values().map { it to Random.nextInt(10, 30) }.toMap()

val nonUrgentSurgeryTimes =
    InjuryType.values().zip(Random(1).run { List(InjuryType.values().size) { this.nextDouble(0.1, 0.5) } }).toMap()

val surgerySuccessProbability = Severity.values().zip(listOf(1.0, 1.0, 1.0, 0.8, 0.7)).toMap()


class HeadNurse(val er: EmergencyRoom) {

    // todo also here having sorted queue is causing almost more problems than solving
    val waitingLine = ComponentQueue<Patient>(comparator = compareBy { it.severity.value })


    fun register(patient: Patient) {
        waitingLine.add(patient)

        // if there is an idle room activate it
        er.rooms.find { it.isData }?.activate()
    }

    fun nextOne(): Patient? {
        // simple fifo
        return if (waitingLine.size > 0) waitingLine.poll() else null
    }
}

class Doctor(name: String, val qualification: List<InjuryType>) : Resource(name)

data class EmergencyRoom(val rooms: List<Room>, val doctors: List<Doctor>)

fun main() {
    val sim = createSimulation(true) {

        fun sampleQualification() =
            InjuryType.values().toList().shuffled(random).take(random.nextInt(3, 5))

        val doctors = List(3) {
            Doctor("Dr $it", sampleQualification())
        }

        val er = dependency {
            EmergencyRoom(
                rooms = List(1) { Room("room $it", State(InjuryType.values().random(random))) },
                doctors = doctors
            )
        }

        val nurse = dependency { HeadNurse(er) }

        // Add additional metrics
        dependency(named("deceased")) { NumericLevelMonitor("deceased") }
        dependency(named("treated")) { NumericLevelMonitor("treated") }

        val typeDist = enumerated(InjuryType.values())
        val sevDist = enumerated(Severity.values().zip(listOf(0.05, 0.1, 0.2, 0.3, 0.45)).toMap())

        ComponentGenerator(iat = exponential(0.5)) {
            val patient = Patient(typeDist.sample(), State(sevDist.sample()), State(Waiting))

            nurse.register(patient)

        }
    }

    // run for a week
    sim.run(24 * 7)

    // analysis
    sim.get<NumericLevelMonitor>(named("treated")).display("Treated Patients")
    sim.get<NumericLevelMonitor>(named("treated")).display("Deceased Patients")

    sim.get<HeadNurse>().waitingLine.queueLengthMonitor.display()
    sim.get<HeadNurse>().waitingLine.lengthOfStayMonitor.display()

    // visualize room setup as gant chart
}

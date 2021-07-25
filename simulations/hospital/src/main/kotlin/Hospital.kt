package org.kalasim.examples.hospital

import org.kalasim.*
import org.kalasim.examples.hospital.PatientStatus.DeceasedWhileWaiting
import org.kalasim.examples.hospital.PatientStatus.Waiting
import org.kalasim.examples.hospital.Severity.*
import org.kalasim.monitors.NumericLevelMonitor
import org.kalasim.plot.kravis.display
import org.koin.core.component.get
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
                    holdExponential(8)
                    updatePatient(Urgent)
                }
                Urgent -> {
                    holdExponential(4)
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
                    val get = get<EmergencyRoom>()
                    get.deceasedMonitor.inc()
                    get.waitingLine.remove(this@Patient)
                }
            }
        }
    }

    private fun updatePatient(newSeverity: Severity) {
        this.severity.value = newSeverity

        // to adjust the queue position, we need to remove and readd it to the queue
        get<EmergencyRoom>().waitingLine.updateOrderOf(this@Patient)
    }
}


/** Main actors. Rooms activly pull new work which is dispatched by the head-nurse. */
class Room(name: String, var setup: State<InjuryType>) : Component(name) {

    override fun process() = sequence {
        while (true) {
            val patient = get<HeadNurse>().nextOne(this@Room)

            if (patient == null) {
                cancel(); return@sequence
            }

            patient.patientStatus.value = PatientStatus.InSurgery


            // check setup state of room
            val injuryType = patient.type

            if (setup.value != injuryType) {
                hold(setupTimes[injuryType]!!, description = "preparing room ${this@Room} for $injuryType")
                setup.value = injuryType
            }

            // find a qualified surgeon
            val doctors = get<EmergencyRoom>().doctors
                .filter { it.qualification.contains(injuryType) }

            // perform surgery one a qualified doctor becomes available
            request(doctors, oneOf = true) { doctor ->
                // will be in range [1, inf]
                val stressFactor = sqrt(get<EmergencyRoom>().waitingLine.size.toDouble())
                val severityFactor = patient.severity.value.ordinal.toDouble().pow(0.6)

                // surgery time is a weighted by business in the ER and severity of the patient
                val surgeryTime = stressFactor * severityFactor * nonUrgentSurgeryTimes[injuryType]!!
                hold(
                    surgeryTime,
                    description = "Surgery of patient ${patient} in room ${this@Room} by doctor ${doctor}"
                )

                // was it successful? This depends on the severity of the injury
                val isDeceased = surgerySuccessProbability[patient.severity.value]!! > env.random.nextDouble()
                if (isDeceased) {
                    get<EmergencyRoom>().treatedMonitor.inc()
                    patient.patientStatus.value = PatientStatus.Released
                } else {
                    get<EmergencyRoom>().deceasedMonitor.inc()
                    patient.patientStatus.value = PatientStatus.DeceasedInSurgery
                }

                log("surgery of ${patient} completed ${if (isDeceased) "with" else "without"} success")
            }
        }
    }
}


val setupTimes = InjuryType.values().map { it to Random.nextInt(15, 30).toDouble() / 60.0 }.toMap()

val nonUrgentSurgeryTimes =
    InjuryType.values().zip(Random(1).run { List(InjuryType.values().size) { this.nextDouble(0.2, 0.5) } }).toMap()

val surgerySuccessProbability = Severity.values().zip(listOf(1.0, 1.0, 1.0, 0.8, 0.7)).toMap()


class HeadNurse(val er: EmergencyRoom) {

    fun nextOne(room: Room): Patient? {
        // simple fifo
        return if (er.waitingLine.size > 0) er.waitingLine.poll() else null
    }
}

class Doctor(name: String, val qualification: List<InjuryType>) : Resource(name)




class EmergencyRoom : Environment(true) {

    // todo also here having sorted queue is causing almost more problems than solving
    val waitingLine = ComponentQueue<Patient>(comparator = compareBy { it.severity.value }, name = "ER Waiting Area")

    private fun sampleQualification() =
        InjuryType.values().toList().shuffled(random).take(random.nextInt(3, 6))

    val doctors = List(4) {
        Doctor("Dr $it", sampleQualification())
    }

    init {
        // add it self as dependency
        dependency { this@EmergencyRoom }

        // make sure that there is a doctor for each typo of injury
        require(
            doctors.flatMap { it.qualification }.toSet() == InjuryType.values().toSet()
        ) { "lack of staff qualification" }
    }

    val rooms = List(2) { Room("room $it", State(InjuryType.values().random(random), name = "Setup of room $it")) }

    // Add additional metrics
    val deceasedMonitor = NumericLevelMonitor("deceased patients")
    val treatedMonitor = NumericLevelMonitor("treated patients")
    val incomingMonitor =  NumericLevelMonitor("incoming patients")

    val nurse = dependency { HeadNurse(this@EmergencyRoom) }


    // incoming patients
    init {
        val typeDist = enumerated(InjuryType.values())
        val sevDist = enumerated(values().zip(listOf(0.05, 0.1, 0.2, 0.3, 0.45)).toMap())

        ComponentGenerator(iat = exponential(0.1), total=800) {
            val patient = Patient(typeDist.sample(), State(sevDist.sample()), State(Waiting))

            // todo this is not pretty; How to model time-dependent iat?
            // reduce new patients during the night
            val isDay = (now.value % 24) in 8.0..18.0
            if (isDay || random.nextDouble() > 0.95) {
                register(patient)
            }else{
                println("skipping patient (out-of-office")
            }

            patient
        }
    }

    fun register(patient: Patient) {
        incomingMonitor.inc()
        waitingLine.add(patient)

        // if there is an idle room activate it
        rooms.find { it.isData }?.activate()
    }
}

fun main() {
    val sim = EmergencyRoom().apply {

        // run for a week
        run(24 * 6)

        // analysis
        incomingMonitor.display("Incoming Patients")
        treatedMonitor.display("Treated Patients")
        deceasedMonitor.display("Deceased Patients")

        get<EmergencyRoom>().apply {
            rooms[0].setup.valueMonitor.display().show()
            rooms[1].setup.valueMonitor.display().show()

            rooms[1].statusMonitor.display().show()
        }

        waitingLine.queueLengthMonitor.display().show()
        waitingLine.lengthOfStayMonitor.display().show()

        // visualize room setup as gant chart
    }
}
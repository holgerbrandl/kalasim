import PatientStatus.*
import Severity.*
import org.kalasim.*
import org.kalasim.monitors.MetricTimeline
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
        // disabled because just needed for component-queue
//        get<EmergencyRoom>().waitingLine.updateOrderOf(this@Patient)
    }
}


/** Main actors. Rooms activly pull new work which is dispatched by the head-nurse. */
class Room(name: String, var setup: State<InjuryType>) : Component(name) {

    override fun process() = sequence {
        while (true) {
            val er = get<EmergencyRoom>()
            val patient = er.nurse.nextOne(er, this@Room)

            if (patient == null) {
                cancel(); return@sequence
            }

            // pick him/her up from the waiting area
            er.waitingLine.remove(patient)
            patient.patientStatus.value = InSurgery

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
                val stressFactor = sqrt(1 + get<EmergencyRoom>().waitingLine.size.toDouble())

                // surgery time is a weighted by business in the ER and severity of the patient
                val severityWeightedSurgeryTime = patient.severityWeightedSurgeryTime

                val surgeryTime = stressFactor * severityWeightedSurgeryTime
                hold(
                    surgeryTime,
                    description = "Surgery of patient ${patient} in room ${this@Room} by doctor ${doctor}"
                )

                // was it successful? This depends on the severity of the injury
                val isDeceased = surgerySuccessProbability[patient.severity.value]!! > env.random.nextDouble()
                if (isDeceased) {
                    get<EmergencyRoom>().treatedMonitor.inc()
                    patient.patientStatus.value = Released
                } else {
                    get<EmergencyRoom>().deceasedMonitor.inc()
                    patient.patientStatus.value = DeceasedInSurgery
                }

                log("surgery of ${patient} completed ${if (isDeceased) "with" else "without"} success")
            }
        }
    }
}


val Patient.severityWeightedSurgeryTime: Double
    get() {
        val severityFactor = severity.value.ordinal.toDouble().pow(0.6)

        return severityFactor * nonUrgentSurgeryTimes[type]!!
    }

val setupTimes = InjuryType.values().map { it to Random.nextInt(5, 10).toDouble() / 60.0 }.toMap()

val nonUrgentSurgeryTimes =
    InjuryType.values().zip(Random(1).run { List(InjuryType.values().size) { nextDouble(0.1, 0.4) } }).toMap()

val surgerySuccessProbability = Severity.values().zip(listOf(1.0, 1.0, 1.0, 0.9, 0.8)).toMap()


/** Observations */
class ErMetrics {

}

fun interface HeadNurse {
    fun nextOne(er: EmergencyRoom, room: Room): Patient?
}


val bySeverity = compareBy<Patient> { it.severity.value }
val bySurgeryTime = compareBy<Patient> { it.severityWeightedSurgeryTime }


class FifoNurse : HeadNurse {

    override fun nextOne(er: EmergencyRoom, room: Room): Patient? {

        // simple fifo
        return if (er.waitingLine.size > 0) er.waitingLine.poll() else null
    }
}

val RefittingAvoidanceNurse = HeadNurse { er, room -> // simple fifo
    val sameTypePatients = er.waitingLine.filter { it.type == room.setup.value }

    val firstBySeverity = sameTypePatients.sortedWith(bySeverity).firstOrNull()

    // if we need to setup we setup to whats most needed in total count
//    if(er.waitingLine.isEmpty()) return@HeadNurse null
//    val maxSeverity = er.waitingLine.groupingBy { it.severity.value }.eachCount().maxByOrNull { it.value }!!
//    return er.waitingLine.filter{it.severity.value ==maxSeverity.key}.sortedWith (bySeverity).firstOrNull()

    // or if no same type injuries are present, we could use the most severe patient
    firstBySeverity ?: er.waitingLine.sortedWith(bySeverity).firstOrNull()
}

val SetupAvoidanceNoMatterWhatNurse = HeadNurse { er, room -> // simple fifo
    val sameTypePatients = er.waitingLine.filter { it.type == room.setup.value }

    val firstBySeverity = sameTypePatients.sortedWith(bySeverity).firstOrNull()

    // if we need to setup we setup to whats most needed in total count
    if (er.waitingLine.isEmpty()) return@HeadNurse null

    val maxSeverity = er.waitingLine.groupingBy { it.severity.value }.eachCount().maxByOrNull { it.value }!!
    er.waitingLine.filter { it.severity.value == maxSeverity.key }.sortedWith(bySeverity).firstOrNull()
}


val urgencyNurse = HeadNurse { er, room -> // simple fifo
    er.waitingLine.sortedWith(bySeverity).firstOrNull()
}

val ShortestTreatmentTimeNurse = HeadNurse { er, room ->
    er.waitingLine.sortedWith(bySurgeryTime).firstOrNull()
}

class Doctor(name: String, val qualification: List<InjuryType>) : Resource(name)


class EmergencyRoom(
//    nurse: HeadNurse = FifoNurse()
    nurse: HeadNurse = FifoNurse(),
    disableMetrics: Boolean = true
) : Environment(true) {

    val waitingAreaSize = 300

    init {
        if (disableMetrics) trackingPolicyFactory.disableAll()
    }

    // todo also here having sorted queue is causing almost more problems than solving
//    val waitingLine = ComponentQueue(comparator = compareBy <Patient>{ it.severity.value }, name = "ER Waiting Area")
//    val waitingLine = ComponentQueue(comparator = compareBy <Patient>{ it.severity.value }.thenBy { it.type }, name = "ER Waiting Area")
    val waitingLine = ComponentList<Patient>(name = "ER Waiting Area Queue")

    private fun sampleQualification() =
        InjuryType.values().toList().shuffled(random).take(random.nextInt(3, 6))

    val doctors = List(6) {
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

    val rooms = List(4) { Room("room $it", State(InjuryType.values().random(random), name = "Setup of room $it")) }

    // Add additional metrics
    val deceasedMonitor = MetricTimeline("deceased patients")
    val treatedMonitor = MetricTimeline("treated patients")
    val incomingMonitor = MetricTimeline("incoming patients")

    init {
        if (disableMetrics) {
            deceasedMonitor.enabled = false
            treatedMonitor.enabled = false
            incomingMonitor.enabled = false
        }
    }

    val nurse = nurse


    // incoming patients
    init {
        val typeDist = enumerated(InjuryType.values())
        val sevDist = enumerated(Severity.values().zip(listOf(0.05, 0.1, 0.2, 0.3, 0.45)).toMap())

        val cg = ComponentGenerator(
            iat = exponential(0.2),
//            total = 800,
            storeRefs = false
        ) {
            val patient = Patient(typeDist.sample(), State(sevDist.sample()), State(Waiting))

            // todo this is not pretty; How to model time-dependent iat?
            // reduce new patients during the night
            val isDay = (now.value % 24) in 8.0..18.0
            if ((isDay || random.nextDouble() > 0.9) && waitingLine.size <= waitingAreaSize) {
                register(patient)
            } else {
//                println("skipping patient (out-of-office")
            }

            patient
        }

        dependency { cg }
    }

    fun register(patient: Patient) {
        incomingMonitor.inc()
        waitingLine.add(patient)

        // if there is an idle room activate it
        rooms.find { it.isData }?.activate()
    }
}


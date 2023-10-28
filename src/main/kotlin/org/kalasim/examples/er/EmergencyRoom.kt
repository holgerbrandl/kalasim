package org.kalasim.examples.er

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.kalasim.*
import org.kalasim.examples.er.PatientStatus.*
import org.kalasim.examples.er.Severity.*
import org.kalasim.misc.Faker
import org.kalasim.monitors.IntTimeline
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

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
    val fullName: String,
    val patientId: Long,
    val type: InjuryType,
    val severity: State<Severity>,
    val patientStatus: State<PatientStatus>
) : Component("$patientId $fullName") {


    override fun process() = sequence {
        suspend fun SequenceScope<Component>.holdExponential(meanTime: Duration) {
            hold(exponential(meanTime).sample())
        }

        while(!patientStatus.value.deceased) {
            when(severity.value) {
                NonUrgent -> passivate()
                LessUrgent -> {
                    holdExponential(8.hours)
                    updatePatient(Urgent)
                }

                Urgent -> {
                    holdExponential(4.hours)
                    updatePatient(Emergent)
                }

                Emergent -> {
                    holdExponential(2.hours)
                    updatePatient(Resuscitation)
                }

                Resuscitation -> {
                    holdExponential(1.hours)

                    // Unfortunately, the patient did not receive treatment in time and did not make it
                    patientStatus.value = DeceasedWhileWaiting
                    val get = get<EmergencyRoom>()

                    // Dead patients is removed from the queue
                    get.deceasedMonitor.inc()
                    get.waitingLine.remove(this@Patient)
                }
            }
        }
    }

    private fun updatePatient(newSeverity: Severity) {
        this.severity.value = newSeverity

        // to adjust the queue position, we need to remove and read it to the queue
        // disabled because just needed for component-queue
//        get<EmergencyRoom>().waitingLine.updateOrderOf(this@Patient)
    }
}


/** Main actors. Rooms actively pull new work which is dispatched by the head-nurse. */
class Room(name: String, var setup: State<InjuryType>) : Component(name) {

    override fun repeatedProcess() = sequence {
        val er = get<EmergencyRoom>()
        val patient = er.nurse.nextOne(er, this@Room)

        if(patient == null) {
            cancel(); return@sequence
        }

        // pick him/her up from the waiting area
        er.waitingLine.remove(patient)
        patient.patientStatus.value = InSurgery

        // check setup state of room
        val injuryType = patient.type

        if(setup.value != injuryType) {
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

            val surgeryTime =  severityWeightedSurgeryTime * stressFactor
            hold(
                surgeryTime,
                description = "Surgery of patient $patient in room ${this@Room} by doctor $doctor"
            )

            // was it successful? This depends on the severity of the injury
            val treatmentSuccessful = surgerySuccessProbability[patient.severity.value]!! > env.random.nextDouble()
            if(treatmentSuccessful) {
                get<EmergencyRoom>().treatedMonitor.inc()
                patient.patientStatus.value = Released
            } else {
                get<EmergencyRoom>().deceasedMonitor.inc()
                patient.patientStatus.value = DeceasedInSurgery
            }

            log("surgery of $patient completed ${if(treatmentSuccessful) "with" else "without"} success")
        }
    }
}


val Patient.severityWeightedSurgeryTime: Duration
    get() {
        val severityFactor = severity.value.ordinal.toDouble().pow(0.6)

        return nonUrgentSurgeryTimes[type]!! * severityFactor
    }

val setupTimes = with(Random(1)) {
    InjuryType.values().associateWith { nextInt(5, 10).minutes }
}

val nonUrgentSurgeryTimes = with(Random(1)) {
    InjuryType.values().associateWith { nextDouble(0.1, 0.4).hours }
}

val surgerySuccessProbability = Severity.values().zip(listOf(1.0, 1.0, 1.0, 0.9, 0.8)).toMap()


/** Observations */
//class ErMetrics

fun interface HeadNurse {
    fun nextOne(er: EmergencyRoom, room: Room): Patient?
}


val bySeverity = compareBy<Patient> { it.severity.value }
val bySurgeryTime = compareBy<Patient> { it.severityWeightedSurgeryTime }


class FifoNurse : HeadNurse {

    override fun nextOne(er: EmergencyRoom, room: Room): Patient? {

        // simple fifo
        return if(er.waitingLine.size > 0) er.waitingLine.poll() else null
    }
}

val RefittingAvoidanceNurse = HeadNurse { er, room -> // simple fifo
    val sameTypePatients = er.waitingLine.filter { it.type == room.setup.value }

    val firstBySeverity = sameTypePatients.sortedWith(bySeverity).firstOrNull()

    // if we need to setup we setup to whats most needed in total count
//    if(er.waitingLine.isEmpty()) return@HeadNurse null
//    val maxSeverity = er.waitingLine.groupingBy { it.severity.value }.eachCount().maxByOrNull { it.value }!!
//    return er.waitingLine.filter{it.severity.value ==maxSeverity.key}.sortedWith (org.kalasim.examples.er.getBySeverity).firstOrNull()

    // or if no same type injuries are present, we could use the most severe patient
    firstBySeverity ?: er.waitingLine.sortedWith(bySeverity).firstOrNull()
}

// todo add considerate-nurse

@Suppress("unused")
val SetupAvoidanceNoMatterWhatNurse = HeadNurse { er, _ -> // simple fifo
//    val sameTypePatients = er.waitingLine.filter { it.type == room.setup.value }
//    val firstBySeverity = sameTypePatients.sortedWith(bySeverity).firstOrNull()

    // if we need to setup we setup to whats most needed in total count
    if(er.waitingLine.isEmpty()) return@HeadNurse null

    val maxSeverity = er.waitingLine.groupingBy { it.severity.value }.eachCount().maxByOrNull { it.value }!!
    er.waitingLine.filter { it.severity.value == maxSeverity.key }.sortedWith(bySeverity).firstOrNull()
}


@Suppress("unused")
val UrgencyNurse = HeadNurse { er, _ -> // simple fifo
    er.waitingLine.sortedWith(bySeverity).firstOrNull()
}

@Suppress("unused")
val ShortestTreatmentTimeNurse = HeadNurse { er, _ ->
    er.waitingLine.sortedWith(bySurgeryTime).firstOrNull()
}

class Doctor(name: String, val qualification: List<InjuryType>) : Resource(name)


class EmergencyRoom(
//    nurse: HeadNurse = FifoNurse()
    /** The number of physicians in the ER */
    val numPhysicians: Int = 6,
    /** The number of qualifications per physician. */
    val physicianQualRange: IntRange = 2..4,
    /** The number of rooms in the ER. Each room is assigned a random injury-type.*/
    val numRooms: Int = 4,
    /** The execution planning policy of the ER. */
    val nurse: HeadNurse = FifoNurse(),
    enableComponentLogger: Boolean = false,
    enableTickMetrics: Boolean = false,
) : Environment(
    enableComponentLogger = enableComponentLogger,
    enableTickMetrics = enableTickMetrics,
    tickDurationUnit = DurationUnit.HOURS
) {

    // todo this should be opt-in anyway https://github.com/holgerbrandl/kalasim/issues/66
    init{
        trackingPolicyFactory.disableAll()
    }
    val waitingAreaSize = 300

    // todo also here having sorted queue is causing almost more problems than solving
//    val waitingLine = ComponentQueue(comparator = compareBy <Patient>{ it.severity.value }, name = "ER Waiting Area")
//    val waitingLine = ComponentQueue(comparator = compareBy <Patient>{ it.severity.value }.thenBy { it.type }, name = "ER Waiting Area")
    val waitingLine = ComponentList<Patient>(name = "ER Waiting Area Queue")

    private fun sampleQualification(numQuals: Int) =
        InjuryType.values().toList().shuffled(random).take(numQuals)

    val faker = Faker(random)

    val doctors = run {
        val qualDist = discreteUniform(physicianQualRange)

        List(numPhysicians) {
            Doctor("Dr. ${faker.makeName().last}", sampleQualification(qualDist())) }
    }

    init {
        // add it self as dependency
        dependency { this@EmergencyRoom }

        // make sure that there is a doctor for each typo of injury
        require(
            doctors.flatMap { it.qualification }.toSet() == InjuryType.values().toSet()
        ) { "lack of staff qualification" }
    }

    val rooms =
        List(numRooms) { Room("room $it", State(InjuryType.values().random(random), name = "Setup of room $it")) }

    // Add additional metrics
    val deceasedMonitor = IntTimeline("deceased patients")
    val treatedMonitor = IntTimeline("treated patients")
    val incomingMonitor = IntTimeline("incoming patients")

//    init {
//        if(!enableTickMetrics) {
//            deceasedMonitor.enabled = false
//            treatedMonitor.enabled = false
//            incomingMonitor.enabled = false
//        }
//    }


    // incoming patients
    init {
        val typeDist = enumerated(InjuryType.values())
        val sevDist = enumerated(Severity.values().zip(listOf(0.05, 0.1, 0.2, 0.3, 0.45)).toMap())

        val cg = ComponentGenerator(
            iat = exponential(0.2).hours,
//            total = 800,
            keepHistory = true
        ) {
            val name = faker.makeName()
            val patient = Patient(
                name.fullName,
                it.toLong(),
                typeDist.sample(),
                State(sevDist.sample()),
                State(Waiting)
            )

            // todo this is not pretty; How to model time-dependent iat?
            // reduce new patients during the night
            val isDay = (now.toLocalDateTime(TimeZone.UTC).hour % 24) in 8..18
            if((isDay || random.nextDouble() > 0.9) && waitingLine.size <= waitingAreaSize) {
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


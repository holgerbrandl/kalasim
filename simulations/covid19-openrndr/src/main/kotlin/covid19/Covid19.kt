package covid19

import org.kalasim.*
import org.kalasim.monitors.MetricTimeline
import java.util.*


var CONTACT_INFECTION_PROBABILITY = 0.9
val MIN_CONTACT_DISTANCE = 5

data class Position(val x: Double, val y: Double) {

    fun distance(other: Position): Double {
        val deltaX: Double = x - other.x
        val deltaY: Double = y - other.y

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY)
    }
}


class Infection(val person: Person) : Component() {
    val convalescenceTime = normal(12, 2).clip(0)

    init {
        person.sick = true
        get<MetricTimeline>().inc()
    }

    override fun process() = sequence {
        // wait for symptoms
//        hold(normal(4,2)())
//
//        sequence<Component>{
//            person.let{
//                passivate()
//            }
//        }.toList()

        hold(convalescenceTime().run { if (this < 0) 0 else this })
        person.sick = false
        person.immune = true
        get<MetricTimeline>().dec()

        // undo quarantaine
//        person.activate()
    }
}

class Person(var position: Position, var sick: Boolean = false, var immune : Boolean = false): Component(){

    val personTracker = get<PersonTracker>()

    override fun process() = sequence {
        while (true) {
            //move around
            position = personTracker.increment(position)

            // if in contact check for risk for infection
            if(!immune && personTracker.hasSickContact(this@Person)){
                if(CONTACT_INFECTION_PROBABILITY > Random().nextDouble()) {
                    Infection(this@Person)


                    // go into quarantine until cured?
//                    passivate()
                }
            }

            log(PersonStatusEvent(now, this@Person))

            // 100 actions per day
            hold(uniform(0, 0.08).sample())
        }
    }
}

class PersonStatusEvent(now:TickTime, person: Person):Event(now) {
    val person = person.name
    val position = person.position.copy()
    val sick = person.sick
    val immune = person.immune
}

class PersonTracker(environment: Environment) {
    val startLocDist = environment.uniform(0.0, 100.0)
    val incrementDist = environment.normal(0.3, 0.1)
    val direction = environment.enumerated(mapOf(-1.0 to 0.5, 1.0 to 0.5))


    val population = mutableListOf<Person>()

    fun startPosition(): Position = Position(startLocDist.sample(), startLocDist.sample())

    fun createPerson() = Person(startPosition()).also { population.add(it) }

    fun increment(p: Position): Position =
        Position(p.x + incrementDist()* direction.sample(), p.y + incrementDist()*direction.sample()).withinCityLimits()

    fun Position.withinCityLimits() = Position(x.coerceIn(0.0, 100.0), y.coerceIn(0.0, 100.0))


    fun hasSickContact(person: Person): Boolean = population
        .filter { it.sick }
        .filter { it != person }
        .any {
            person.position.distance(it.position) < MIN_CONTACT_DISTANCE
        }
}

class Covid19 : Environment() {
    init {
        dependency { PersonTracker(this) }
        dependency { MetricTimeline("sick_monitor") }

        eventLog()

        val populationManager = get<PersonTracker>()

        // use a fixed population
        repeat(100) { populationManager.createPerson() }
        // add a few sick ones
        repeat(5) { populationManager.createPerson().also { Infection(it) } }

//        run(100)

        // mutate virus
        CONTACT_INFECTION_PROBABILITY = 0.8
    }
}



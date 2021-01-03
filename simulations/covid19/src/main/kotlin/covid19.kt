import org.apache.commons.math3.random.JDKRandomGenerator
import org.kalasim.*
import org.koin.core.component.get
import org.koin.core.qualifier.StringQualifier
import java.util.*

data class Position(val x: Double, val y: Double){

    fun distance(other: Position): Double {
        val deltaX: Double = x - other.x
        val deltaY: Double = y - other.y

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY)
    }
}

// todo calculate infection inzidenz
val CONTACT_INFECTION_PROBABILITY = 0.1

class Infection(val person: Person):Component(){
    val cureProb = uniform(7,14)

    init{
        person.sick = true
        get<NumericLevelMonitor>().inc()
    }

    override fun process() = sequence<Component> {
        hold(cureProb())
        person.sick = false
        get<NumericLevelMonitor>().dec()
    }
}

class Person(var position: Position, var sick: Boolean = false): Component(){

    val personTracker = get<PersonTracker>()

    override fun process() = sequence<Component> {
        while(true){
            //move around
            position = personTracker.increment(position)

            // if in contact check for risk for infection
            if(personTracker.hasSickContact(this@Person)){
                if(Random().nextDouble() > CONTACT_INFECTION_PROBABILITY) {
                    Infection(this@Person)
                }
            }
        }
    }
}

class PersonTracker {
    val startLocDist = uniform(0, 100, JDKRandomGenerator(42))
    val incrementDist = normal(0.3, 0.1)

    private val MIN_CONTACT_DISTANCE = 1

    val population = mutableListOf<Person>()

    fun startPosition(): Position  = Position(startLocDist.sample(), startLocDist.sample())

    fun createPerson() = Person(startPosition()).also { population + it }

    fun increment(p: Position): Position  =
        Position(p.x + incrementDist(), p.x + incrementDist()).withinCityLimits()

    fun Position.withinCityLimits() = Position(x.coerceIn(0.0, 100.0), y.coerceIn(0.0, 100.0))


    fun hasSickContact(person: Person): Boolean = population
        .filter{ it.sick }
        .filter{ it != person}
        .any{ person.position.distance(it.position) < MIN_CONTACT_DISTANCE }
}


fun main() {

    declareDependencies {
        add{ PersonTracker() }
        add{ NumericLevelMonitor("sick_monitor") }
    }.createSimulation{

        val populationManager = get<PersonTracker>()

        // use a fixed population
        repeat(100){ populationManager.createPerson() }

        run(100)

        // setup level monitor for sick persons
        get<NumericLevelMonitor>().printHistogram()

    }
}



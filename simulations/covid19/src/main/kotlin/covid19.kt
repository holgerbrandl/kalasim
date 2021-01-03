import org.apache.commons.math3.random.JDKRandomGenerator
import org.kalasim.*
import org.kalasim.misc.display
import org.koin.core.component.get
import java.util.*


var CONTACT_INFECTION_PROBABILITY = 0.3
const val MIN_CONTACT_DISTANCE = 5


data class Position(val x: Double, val y: Double){

    fun distance(other: Position): Double {
        val deltaX: Double = x - other.x
        val deltaY: Double = y - other.y

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY)
    }
}


class Infection(val person: Person):Component(){
    val cureProb = normal(12,4)

    init{
        person.sick = true
        get<NumericLevelMonitor>().inc()
    }

    override fun process() = sequence<Component> {
        // wait for symptoms
//        hold(normal(4,2)())
//
//        sequence<Component>{
//            person.let{
//                passivate()
//            }
//        }.toList()

        hold(cureProb())
        person.sick = false
        get<NumericLevelMonitor>().dec()

        // undo quarantaine
//        person.activate()
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
                if(CONTACT_INFECTION_PROBABILITY > Random().nextDouble()) {
                    Infection(this@Person)


                    // go into quarantine until cured?
//                    passivate()
                }
            }


            hold(uniform(1,2).sample())
        }
    }
}

class PersonTracker {
    val startLocDist = uniform(0, 100, JDKRandomGenerator(42))
    val incrementDist = normal(0.3, 0.1)


    val population = mutableListOf<Person>()

    fun startPosition(): Position  = Position(startLocDist.sample(), startLocDist.sample())

    fun createPerson() = Person(startPosition()).also { population.add(it) }

    fun increment(p: Position): Position  =
        Position(p.x + incrementDist(), p.x + incrementDist()).withinCityLimits()

    fun Position.withinCityLimits() = Position(x.coerceIn(0.0, 100.0), y.coerceIn(0.0, 100.0))


    fun hasSickContact(person: Person): Boolean = population
        .filter{ it.sick }
        .filter{ it != person}
        .any{
            person.position.distance(it.position) < MIN_CONTACT_DISTANCE
        }
}


fun main() {

    declareDependencies {
        add{ PersonTracker() }
        add{ NumericLevelMonitor("sick_monitor") }
    }.createSimulation(){

        val populationManager = get<PersonTracker>()

        // use a fixed population
        repeat(100){ populationManager.createPerson() }
        // add a few sick ones
        repeat(10){ populationManager.createPerson().also { Infection(it) }}

        run(100)

        // mutate virus
        CONTACT_INFECTION_PROBABILITY = 0.8
        run(100)

        // setup level monitor for sick persons
        get<NumericLevelMonitor>().printHistogram()
        get<NumericLevelMonitor>().display()

    }
}

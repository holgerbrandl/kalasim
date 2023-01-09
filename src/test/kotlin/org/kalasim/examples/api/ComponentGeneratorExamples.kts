//ComponentGeneratorExamples.kts
import org.kalasim.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


createSimulation{

    // example 1
    // we can schedule with a probabilist inter-arrival distribution
    data class Customer(val id: Int)

    ComponentGenerator(uniform(5.minutes, 2.hours)){
            customerNo ->  Customer(customerNo)
    }

    // we can also schedule with a fixed rate
    // here we create 3 strings with fixed inter-arrival duration
    ComponentGenerator(3.minutes, total =3 ){ it }

    // example 2
    // we define a component with simplistic process definition
    class Car() :Component(){
        override fun process() = sequence {
            hold(3.hours, description="driving")
        }
    }

    ComponentGenerator(exponential(3.minutes), until = now + 3.days){
        Car() // when creating a component it will be automatically scheduled next
    }

    // example 3 no-longer recommend:
    // inter-arrival distribution without duration unit
    ComponentGenerator(uniform(3, 4)){  Customer(it) }
}

import org.kalasim.Component
import org.kalasim.Event
import org.kalasim.createSimulation

createSimulation {
    addEventListener{ println(it)}

    class MyEvent(msg:String) : Event(now())

    object: Component(){
//        override fun process() = sequence<Component> {
//            log(MyEvent("something magical happened"))
//        }
    }
}

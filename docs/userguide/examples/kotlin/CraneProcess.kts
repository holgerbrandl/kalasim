import org.kalasim.Component
import org.kalasim.FunPointer

class Crane(process: FunPointer? = Component::process) : Component(process=process){
    fun unload() = sequence<Component>{
        // yield ...
    }
}

val crane1 = Crane()
crane1.activate(process=Crane::unload)


// conceptually, the API supports also process definition at instantiation.
Crane(process=Crane::unload)
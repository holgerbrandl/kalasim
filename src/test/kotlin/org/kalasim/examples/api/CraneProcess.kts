//CraneProcess.kts
import org.kalasim.Component
import org.kalasim.ProcessPointer
import org.kalasim.createSimulation

class Crane(process: ProcessPointer? = Component::process) : Component(process = process) {
    fun unload() = sequence<Component> {
        // yield ...
    }
}

createSimulation {
    val crane1 = Crane()
    crane1.activate(process = Crane::unload)

    // conceptually, the API supports also process definition at instantiation.
    Crane(process = Crane::unload)
}


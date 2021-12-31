//CraneProcess.kts
import org.kalasim.*

class Crane(
    process: ProcessPointer? = Component::process
) : Component(process = Crane::load) {
    fun unload() = sequence<Component> {
        // hold, request, wait ...
    }

    fun load() = sequence<Component> {
        // hold, request, wait ...
    }
}

createSimulation {
    val crane1 = Crane() // load will be activated be default

    val crane2 = Crane(process = Crane::load) // force unloading at start

    val crane3 = Crane(process = Crane::unload) // force unloading at start
    crane3.activate(process = Crane::load) // activate other process
}


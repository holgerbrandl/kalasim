// ResourceGroups.kts
import org.kalasim.Component
import org.kalasim.Resource
import kotlin.time.Duration.Companion.minutes

val drMeier = Resource()
val drSchreier = Resource()

val doctors: List<Resource> = listOf(drMeier, drSchreier)

object : Component() {
    override fun process() = sequence {
        request(doctors, oneOf = true) {
            hold(5.minutes, "first aid")
        }

        // the patient needs brain surgery, only Dr Meier can do that
        request(drMeier) {
            hold(10.minutes, "brain surgery")
        }
    }
}
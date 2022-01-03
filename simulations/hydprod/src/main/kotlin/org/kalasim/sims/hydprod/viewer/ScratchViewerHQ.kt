import org.openrndr.Program
import org.openrndr.application
import org.openrndr.extra.olive.Olive

fun main() = application {
    configure {
        width = 768
        height = 576
    }
    program {
        extend(Olive<Program>())
    }
}
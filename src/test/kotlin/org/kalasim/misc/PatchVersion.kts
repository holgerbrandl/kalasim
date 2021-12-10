import java.io.File

val newVersion = args[0]
//val newVersion = 0.5

val setupMD = File("docs/userguide/docs/setup.md")
val transformedSetup: String =
    setupMD.readLines()
        .joinToString(System.lineSeparator()) {
            val prefix = """    implementation "com.github.holgerbrandl:kalasim:"""
            if (it.startsWith(prefix)) {
                """    implementation "com.github.holgerbrandl:kalasim:$newVersion""""
            } else {
                it
            }
        }

setupMD.writeText(transformedSetup)

println("patched $setupMD to new version $newVersion")



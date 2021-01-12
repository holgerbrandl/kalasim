import java.io.File

val newVersion = args[0]
//val newVersion = 0.5

val setupMD = File("docs/userguide/docs/setup.md")
val transformedSetup: String = setupMD.readLines().map{
    val prefix: String = """    implementation "org.kalasim:kalasim:"""
    if(it.startsWith(prefix)){
        """    implementation "org.kalasim:kalasim:${newVersion}""""
    }else{
        it
    }
}.joinToString(System.lineSeparator())

setupMD.writeText(transformedSetup)

println("patched ${setupMD} to new version ${newVersion}")



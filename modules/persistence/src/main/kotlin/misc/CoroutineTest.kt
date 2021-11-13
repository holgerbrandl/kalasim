package misc

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import kotlinx.coroutines.runBlocking
import simpleproc.buildProcKryo
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// https://gist.github.com/Restioson/fb5b92e16eaff3d9267024282cf1ed72
fun main(args: Array<String>) = runBlocking { //

    if (true) {
//        launch {
        println("Launching")
            SerialisationHelper.test()
            println("Finished")
//        }
    }

    else {
        val coro = SerialisationHelper.deserialiseCoro()
        coro.resume(Unit)
    }

    while (true) {}

}

object SerialisationHelper {

    val kryo = buildProcKryo()
//    val kryo = Kryo().apply {
//        instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())
//    }

    suspend fun test(): Unit = suspendCoroutine {
        println("Serialising")
        testSerialise(it)
    }

    fun <T: Any> serialiseCoro(coro: Continuation<T>) {
        val output = Output(FileOutputStream("coro.bin"))
        kryo.writeClassAndObject(output, coro)
        output.close()
    }

    fun deserialiseCoro(): Continuation<Any> {

        println("Deserialising")

        val fis = Input(FileInputStream("coro.bin"))

        val coro = kryo.readClassAndObject(fis) as Continuation<Any>
        fis.close()

//        coro::class.java.getDeclaredField("result").apply {
//            isAccessible = true
//            set(coro, COROUTINE_SUSPENDED)
//        }

        return coro
    }

    fun <T: Any> testSerialise(coro: Continuation<T>) {
        serialiseCoro(coro)
    }

}
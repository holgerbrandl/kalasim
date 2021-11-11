import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import org.kalasim.createSimulation
import org.objenesis.strategy.StdInstantiatorStrategy
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap


fun main() {

    createSimulation { }
//        DependencyContext.startKoin{}

    val pq = PriorityQueue(compareBy<String> { it.length })

    val saveFile = File("file.bin")
    val kryo = buildKryoPQ()
    val output: Output = Output(FileOutputStream(saveFile))
    kryo.writeClassAndObject(output, pq)
    output.close()


    val input = Input(FileInputStream(saveFile));
    val restored = kryo.readClassAndObject(input) as PriorityQueue<String>

    println("name is ${restored}")
}


fun buildKryoPQ(): Kryo {

    val kryo = Kryo()

    kryo.setOptimizedGenerics(false);
    kryo.setReferences(true)

    kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

    kryo.register(ConcurrentHashMap::class.java)
//    kryo.addDefaultSerializer(PriorityQueue::class.java,  CustomPriorityQueueSerializer())
    kryo.isRegistrationRequired = false


    return kryo
}

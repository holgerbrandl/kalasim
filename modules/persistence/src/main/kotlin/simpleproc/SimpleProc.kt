package simpleproc

import buildKryo
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.ClosureSerializer
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import org.objenesis.strategy.StdInstantiatorStrategy
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.Serializable
import java.lang.invoke.SerializedLambda
import java.util.NoSuchElementException

class SimpleProcess(seq: Sequence<String>) : Serializable  {

    val iterator = seq.iterator()

     fun call() {
        try {
            iterator.next()
        } catch(e: NoSuchElementException) {
            if(e.message!=null) e.printStackTrace()
        }
    }
}


object GenProcessSim {
    @JvmStatic
    fun main(args: Array<String>) {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
//        val sim = MM1Queue().apply { run(10) }
        val sim = SimpleProcess(sequence {
            yield("foo")
            yield("bar")
        })

        sim.iterator.next()

        val kryo = buildProcKryo()

        val saveFile = File("file.bin")

        val output = Output(FileOutputStream(saveFile))
        kryo.writeClassAndObject(output, sim)
        output.close()

        val input = Input(FileInputStream(saveFile));
        val restored = kryo.readClassAndObject(input) as SimpleProcess

        // analysis
        println(restored.iterator.next())
        println(restored)
//    sim.testSim()
    }
}


fun buildProcKryo(): Kryo {

    val kryo = Kryo()

    kryo.setOptimizedGenerics(false);
    kryo.setReferences(true)

    kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

//    kryo.register(ConcurrentHashMap::class.java)
//    kryo.addDefaultSerializer(PriorityQueue::class.java,  CustomPriorityQueueSerializer())
    kryo.isRegistrationRequired = false


    kryo.register(SerializedLambda::class.java)
    kryo.register(ClosureSerializer.Closure::class.java, ClosureSerializer())

    return kryo
}
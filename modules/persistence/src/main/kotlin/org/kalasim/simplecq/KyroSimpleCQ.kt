package org.kalasim.simplecq

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import org.objenesis.strategy.StdInstantiatorStrategy
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

data class SimpleCQElement<C>(val component: C)

class SimpleComponentQueue<C>(
    val q: Queue<SimpleCQElement<C>> = PriorityQueue { o1, o2 ->
        compareValuesBy(
            o1,
            o2
        ) { it.component.toString().length }
    },
)

fun main() {
    val pq = SimpleComponentQueue<String>()

    val kryo = Kryo()

    kryo.setOptimizedGenerics(false);
    kryo.setReferences(true)
    kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())
    kryo.isRegistrationRequired = false

    val saveFile = File("file.bin")
    val output = Output(FileOutputStream(saveFile))
    kryo.writeClassAndObject(output, pq)
    output.close()

    val input = Input(FileInputStream(saveFile));
    val restored = kryo.readClassAndObject(input) as SimpleComponentQueue<String>
}

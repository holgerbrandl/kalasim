import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.FieldSerializer
import org.kalasim.*
import org.kalasim.demo.MM1Queue
import org.kalasim.misc.DependencyContext
import org.kalasim.monitors.FrequencyLevelMonitor
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.definition.BeanDefinition
import org.koin.core.definition.Callbacks
import org.koin.core.definition.Definition
import org.koin.core.instance.SingleInstanceFactory
import org.koin.core.logger.EmptyLogger
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersHolder
import org.koin.core.qualifier.StringQualifier
import org.koin.core.registry.InstanceRegistry
import org.koin.core.registry.PropertyRegistry
import org.koin.core.registry.ScopeRegistry
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID
import java.awt.Dimension
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque

fun main() {
    val saveFile = File("file.bin")
//    saveFile.delete()
    val kryo = buildKryo()

    val smthg: Definition<Dimension> = { it -> Dimension() }

    val output: Output = Output(FileOutputStream(saveFile))
    kryo.writeClassAndObject(output, smthg)
    output.close()


    println("restoring...")
    val input = Input(FileInputStream(saveFile));
    val restord: Definition<Dimension> = kryo.readClassAndObject(input) as Definition<Dimension>


    val koin = GlobalContext.startKoin {}.koin
    val result = restord(Scope(StringQualifier("foo"), ScopeID(), false, koin), ParametersHolder())
    println("result is ${result}")
}


private fun buildKryo(): Kryo {

    val kryo = Kryo()

    kryo.setReferences(true)

    // https://github.com/EsotericSoftware/kryo/issues/196
    kryo.isRegistrationRequired = false

    kryo.register(Dimension::class.java)

    kryo.register(ArrayList::class.java)
    kryo.register(ArrayDeque::class.java)

    kryo.register(Koin::class.java)
    kryo.register(EmptyLogger::class.java)
    kryo.register(Level::class.java)
    kryo.register(HashSet::class.java)
    kryo.register(Module::class.java)
    kryo.register(MM1Queue::class.java)
    kryo.register(PropertyRegistry::class.java)
    kryo.register(StringQualifier::class.java)
    kryo.register(Scope::class.java)
    kryo.register(ScopeRegistry::class.java)

    kryo.register(BeanDefinition::class.java)
    kryo.register(SingleInstanceFactory::class.java)
    kryo.register(Callbacks::class.java)
    kryo.register(ConcurrentHashMap::class.java)


    //https://github.com/EsotericSoftware/kryo/issues/320
//    kryo.register(load("org.kalasim.Environment\$2\$1"))

    return kryo
}
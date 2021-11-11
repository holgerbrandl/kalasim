import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import org.kalasim.*
import org.kalasim.demo.MM1Queue
import org.kalasim.misc.DependencyContext
import org.kalasim.monitors.FrequencyLevelMonitor
import org.koin.core.Koin
import org.objenesis.strategy.StdInstantiatorStrategy
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.Comparator


object KryoComponent {
    @JvmStatic
    fun main(args: Array<String>) {

        createSimulation { }
//        DependencyContext.startKoin{}
        val c = TestComponent()

        val saveFile = File("file.bin")
        val kryo = buildKryo()
        val output: Output = Output(FileOutputStream(saveFile))
        kryo.writeClassAndObject(output, c)
        output.close()


        val input = Input(FileInputStream(saveFile));
        val restored = kryo.readClassAndObject(input) as TestComponent

        println("name is ${restored}")
    }
}


object KryoSim {
    @JvmStatic
    fun main(args: Array<String>) {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
        val sim = MM1Queue().apply { run(10) }
//        val sim = Environment().apply { run(10) }

        // run for a week
//        run(24 * 14)

        val kryo = buildKryo()

        val saveFile = File("file.bin")

        val output = Output(FileOutputStream(saveFile))
        kryo.writeClassAndObject(output, sim)
        output.close()

        val input = Input(FileInputStream(saveFile));
        val restored = kryo.readClassAndObject(input) as Environment

        // analysis
        restored.run(10)
        println(restored)
//    sim.testSim()
    }
}

object KryoCQ {
    @JvmStatic
    fun main(args: Array<String>) {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
        createSimulation {  }

        class StringComp : Comparator<String>{
            override fun compare(o1: String?, o2: String?): Int {
                return o1!!.compareTo(o2!!)
            }
        }

        val cq = ComponentQueue<String>()
        cq.add("foo")
        cq.add("bar")
//        val sim = Environment().apply { run(10) }

        // run for a week
//        run(24 * 14)

        val kryo = buildKryo()

        val saveFile = File("file.bin")

        val output = Output(FileOutputStream(saveFile))
        kryo.writeClassAndObject(output, cq)
        output.close()

        val input = Input(FileInputStream(saveFile));
        val restored = kryo.readClassAndObject(input) as ComponentQueue<String>

        // analysis
        println(restored.size)
//    sim.testSim()
    }
}



fun buildKryo(): Kryo {

    val kryo = Kryo()

    kryo.setOptimizedGenerics(false);
    kryo.setReferences(true)

    kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

    kryo.register(ConcurrentHashMap::class.java)
//    kryo.addDefaultSerializer(PriorityQueue::class.java,  CustomPriorityQueueSerializer())
    kryo.isRegistrationRequired = false


    return kryo
}

//private fun buildKryo(): Kryo {
//    val kryo = Kryo()
//
//    kryo.setReferences(true)
//
//    // https://github.com/EsotericSoftware/kryo/issues/196
//    kryo.isRegistrationRequired = false
//
//    kryo.register(EmergencyRoom::class.java)
//
//    kryo.register(Dimension::class.java)
//
//    kryo.register(ArrayList::class.java)
//    kryo.register(ArrayDeque::class.java)
//
//    kryo.register(Koin::class.java)
//    kryo.register(EmptyLogger::class.java)
//    kryo.register(Level::class.java)
//    kryo.register(HashSet::class.java)
//    kryo.register(Module::class.java)
//    kryo.register(MM1Queue::class.java)
//    kryo.register(PropertyRegistry::class.java)
//    kryo.register(StringQualifier::class.java)
//    kryo.register(Scope::class.java)
//    kryo.register(ScopeRegistry::class.java)
//
//    val koin = DependencyContext.startKoin { }.koin
//
//    kryo.register(InstanceRegistry::class.java, object : FieldSerializer<Any?>(kryo, InstanceRegistry::class.java) {
//        override fun create(kryo: Kryo, input: Input, type: Class<*>?): InstanceRegistry {
//            return InstanceRegistry(koin)
//        }
//    })
//
////
//    kryo.register(InstanceRegistry::class.java, object : Serializer<InstanceRegistry>() {
//
//        override fun write(kryo: Kryo?, output: Output?, registry: InstanceRegistry) {
////            kryo!!.writeObject(output, registry.instances.toMap())
////            kryo!!.writeObject(output, registry.instances.map{ it.key to it.value})
////            kryo!!.writeObject(output, registry.instances.size)
//
//            kryo!!.writeObject(output, registry)
//
////            registry.instances.forEach{ (_, value) ->
////                kryo!!.writeObject(output, value)
////                registry.instances.values.first()
////            }
//
//            output!!.position()
//            println(registry)
//        }
////
////
////
//        override fun read(kryo: Kryo?, input: Input?, type: Class<out InstanceRegistry>?): InstanceRegistry? {
////            val instances = kryo!!.readObject(input, ArrayList::class.java) as List<Pair<IndexKey, InstanceFactory<*>>>
////            val numFac = kryo!!.readObject(input, Int::class.java)
//            val restInstances = kryo!!.readObject(input, InstanceRegistry::class.java) as? InstanceRegistry
//    return restInstances;
//
////            repeat(numFac){
////                kryo!!.readObject(input,  Definition::class.java)
////            }
////            return InstanceRegistry(DependencyContext.startKoin { }.koin).apply {
//////                module(createdAtStart = true) {
//////                    instances.instances=instances
//////                }
////
////                restInstances?.instances?.forEach { (key, value) ->
////                    (instances as MutableMap)[key] = value
////                }
////
////                println("rest instance-registry ${this}")
////            }
//
//        }
//    })
//
//
////    kryo.register(SingleInstanceFactory::class.java, object : Serializer<Definition<*>>() {
////
////        override fun write(kryo: Kryo?, output: Output?, registry: Definition<*>) {
//////            kryo!!.writeObject(output, registry.instances.toMap())
//////            kryo!!.writeObject(output, registry.instances.map{ it.key to it.value})
////            kryo!!.writeObject(output, registry)
////
//////            registry.instances.forEach{ (_, value) ->
//////                kryo.writeObject(output, value.beanDefinition)
//////                registry.instances.values.first()
//////            }
////
////            println(registry)
////        }
////
////        override fun read(kryo: Kryo?, input: Input?, type: Class<out Definition<*>>?): Definition<*> {
//////           return read(kryo, input, BeanDefinition::class.java)
////           return {
////               "null"
////           }
////        }
////    })
//// kryo.register(SingleInstanceFactory::class.java, object : Serializer<SingleInstanceFactory<*>>() {
////
////        override fun write(kryo: Kryo?, output: Output?, registry: SingleInstanceFactory<*>) {
//////            kryo!!.writeObject(output, registry.instances.toMap())
//////            kryo!!.writeObject(output, registry.instances.map{ it.key to it.value})
////            kryo!!.writeObject(output, registry)
////
//////            registry.instances.forEach{ (_, value) ->
//////                kryo.writeObject(output, value.beanDefinition)
//////                registry.instances.values.first()
//////            }
////
////            println(registry)
////        }
////
////        override fun read(
////            kryo: Kryo?,
////            input: Input?,
////            type: Class<out SingleInstanceFactory<*>>?
////        ): SingleInstanceFactory<*> {
////           return read(kryo, input, Bead::class.java)
//////           return SingleInstanceFactory(beanDefinition = BeanDefinition(StringQualifier("foo"), ) )
////        }
////    })
//
//    kryo.register(PropertyRegistry::class.java, object : Serializer<PropertyRegistry>() {
//        override fun write(kryo: Kryo?, output: Output?, registry: PropertyRegistry) {
////            kryo!!.writeObject(output, registry)
//        }
//
//
//        override fun read(kryo: Kryo?, input: Input?, type: Class<out PropertyRegistry>?): PropertyRegistry {
//            return PropertyRegistry(koin)
//        }
//    })
//
//    kryo.register(ScopeRegistry::class.java, object : Serializer<ScopeRegistry>() {
//        override fun write(kryo: Kryo?, output: Output?, registry: ScopeRegistry) {
////            kryo!!.writeObject(output, registry)
//        }
//
//
//        override fun read(kryo: Kryo?, input: Input?, type: Class<out ScopeRegistry>?): ScopeRegistry {
//            return ScopeRegistry(koin)
//        }
//    })
//    kryo.register(BeanDefinition::class.java)
//    kryo.register(SingleInstanceFactory::class.java)
//    kryo.register(Callbacks::class.java)
//    kryo.register(ConcurrentHashMap::class.java)
//
//    kryo.register(Component::class.java)
//    kryo.register(LinkedHashMap::class.java)
//    kryo.register(LinkedList::class.java)
//    kryo.register(ComponentState::class.java)
//    kryo.register(TickTime::class.java)
//    kryo.register(Environment::class.java)
//    kryo.register(GenProcessInternal::class.java)
//    kryo.register(FrequencyLevelMonitor::class.java)
//    kryo.register(TestComponent::class.java)
//
//    //https://github.com/EsotericSoftware/kryo/issues/320
//    kryo.register(load("org.kalasim.Environment\$2\$1"))
//
//    return kryo
//}

@Throws(ClassNotFoundException::class)
private fun load(name: String): Class<*>? {
    return Class.forName(name, false, ClassLoader.getSystemClassLoader())
}

open class TestComponent(
    name: String? = null,
    at: TickTime? = null,
    delay: Number = 0,
    priority: Priority = NORMAL,
    process: ProcessPointer? = org.kalasim.Component::process,
    koin: Koin = DependencyContext.get()
) {
//    KoinComponent,

    internal var oneOfRequest: Boolean = false

    internal val requests = mapOf<Resource, Double>().toMutableMap()
    internal val waits = listOf<StateRequest<*>>().toMutableList()
    val claims = mapOf<Resource, Double>().toMutableMap()

    var failed: Boolean = false
        internal set

    internal var waitAll: Boolean = false

    internal var simProcess: GenProcessInternal? = null


    // TODO 0.6 get rid of this field (not needed because can be always retrieved from eventList if needed
    //  What are performance implications?
    var scheduledTime: TickTime? = null

    internal var remainingDuration: Double? = null

//    init {
//        println(Component::process == this::process)
//        this.javaClass.getMethod("process").getDeclaringClass();
//    }

    //    var status: ComponentState = if(this.javaClass.getMethod("process").getDeclaringClass().simpleName == "Component") DATA else SCHEDULED
    var componentState: ComponentState = ComponentState.DATA
        internal set(value) {
            field = value
            statusMonitor.addValue(value)
        }

    val statusMonitor = FrequencyLevelMonitor(componentState, "status of ${this}", koin)
}


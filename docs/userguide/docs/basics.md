The beauty of discrete event simulation is its very limited vocabulary which still allows expressing complex system dynamics. In essence, `kalasim` relies on just a handful of elements to model real-world processes.

* [Components](component.md)
* [Resources](resource.md)
* [States](state.md)
* [Collections](collections.md)
* [Generators](component.md#component-generator)


## Simulation Environment

All entities in a simulation are governed by an environment context. Every simulation lives in exactly one such environment. The environment provides means for [controlled randomization](#randomness--distributions), [dependency injection](#dependency-injection), and most importantly manages the [event queue](#event-queue).

The environment context of a kalasim simulation is an instance of  `org.kalasim.Environment`, which can be created using simple instantiation or via a builder called `createSimulation`

```kotlin
val env : Environment = createSimulation(){
    // enable logging of built-in simulation metrics
    enableComponentLogger()
    
    // Create simulation entities in here 
    Car()
    Resource("Car Wash")
}.run(5.0)
```

Within its environment, a simulation contains one or multiple [components](component.md) with [process definition](component.md#process-definition)s that define their behavior and interplay with other simulation entities.

Very often, the user will define custom Environments to streamline simulation API experience.

```kotlin
class MySim(val numCustomers: Int = 5) : Environment() {
    val customers = List(numCustomers) { Customer(it) }
}

val sim = MySim(10)
sim.run()

// analyze customers
sim.customers.first().statusTimeline.display()

```

To configure references first, an `Environment` can also be instantiated by configuring dependencies first with `configureEnvironment`. Check out the [Traffic](examples/traffic.md) example to learn how that works.


## Running a simulation

In a discrete event simulation a clear distinction is made between real time and simulation time. With *real time* we refer to the wall-clock time. It represents the execution time of the experiment. The *simulation time* is an attribute of the simulator.

As shown in the example from above a simulation is usually started with `sim.run(ticks)`. Here `ticks` is the number of ticks, that is simulation time units. The simulation will progress for `ticks`. By doing so we may stop right in the middle of a [process](component.md#process-interaction).

Alternatively, we can also run until the event queue is empty (or forever depending on the model) by omitting the argument: 

```kotlin
sim.run(23) // run for 23 ticks
sim.run(5) // run for some more ticks

sim.run(until = 42.tickTime) // run until internal simulation clock is 23 

sim.run() // run until event queue is empty
```

The user can define a [tick transformation](advanced.md#tick-transformation) to map the internal clock to her wall-time clock. If a [tick transformation](advanced.md#tick-transformation) is configured, it also allows to express run durations more naturally:

```kotlin
sim.run(2.hours)

sim.run(1.4.days) // fractionals are suportes as well
sim.run(until = Instant.now() + 3.hours) // wall-time plus 3 hours
sim.run(until = now + 3.hours) // simulation-time plus 3 hours
```

!!! tip
    A component can always stop the current simulation by calling `stopSimulation()` in its [process definition](component.md#process-definition). See [here](https://github.com/holgerbrandl/kalasim/blob/master/src/test/kotlin/org/kalasim/examples/api/StopSimulation.kt) for fully worked out example.


## Event Queue

The core of *kalasim* is an event queue ordered by scheduled execution time, that maintains a list of events to be executed. To provide good insert, delete and update performance, `kalasim` is using a [`PriorityQueue`](https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html) internally. Components are actively and passively scheduled for reevaluating their state. Technically, event execution refers to the continuation of a component's [process definition](component.md#process-definition).

<!--NOTE simmer is also using time and pririty in its queue -->

<!--TODO simmer Ucar2019,p8 is mentioneing unscheduling as important use-case. Do we need/support that?-->


<!--https://stackoverflow.com/questions/19331362/using-an-image-caption-in-markdown-jekyll-->
<figure>
  <img src="../basics_images/event_loop.svg"  alt="kalasim event model"/>
  <figcaption>Kalasim Execution Model</figcaption>
</figure>


## Execution Order

In the real world, events often appear to happen *at the same time*. However, in fact events always occur at slightly differing times. Clearly the notion of *same* depends on the resolution of the used time axis. Birthdays will happen on the *same day* whereas the precise birth events will always differ in absolute timing.

Even if real-world processes may run "in parallel", a simulation is processed sequentially and deterministically. With the same random-generator initialization,  you will always get the same simulation results when running your simulation multiple times.

Although, `kalasim` supports double-precision to schedule events, events will inevitably arise that are scheduled for the *same time*. Because of its  single-threaded, deterministic execution model (like most DES frameworks),  `kalasim`  processes events sequentially – one after another. If two events are scheduled at the same time, the one scheduled first will also be the processed first (FIFO).

As pointed out in [Ucar, 2019](https://www.jstatsoft.org/article/view/v090i02), there are many situations where such simultaneous events may occur in simulation. To provide a well-defined behavior in such situations, process interaction methods (namely  `wait`, `request`,  `activate` and `reschedule`) support user-provided schedule priorities. With the parameter `priority` in these [interaction methods](component.md#process-interaction), it is possible to order components scheduled for the same time in the event-queue. Events with higher priority are executed first in situations where multiple events are scheduled for the same simulation time.

There are different predefined priorities which correspond the following sort-levels

* `LOWEST` (-20)
* `LOW` (-10)
* `NORMAL` (0)
* `IMPORTANT` (10)
* `CRITICAL` (20)

The user can also create more fine-grained priorities with `Priority(23)`

<!--The `urgent` parameters only applies to components scheduled with the same time and same `priority`. TBD do we need it?-->

In contrast to other DSE implementations, the user does not need to make sure that a resource `release()` is prioritized over a simultaneous `request()`. The engine will automatically reschedule tasks accordingly.
<!-- Also see Ucar2019,p9 (table 1)-->

<!--The priority can be accessed with the new Component.scheduled_priority() method.-->

So the key points to recall are

* Real world events may appear to happen at the same discretized simulation time
* Simulation events are processed one after another, even if they are scheduled for the same time
* Race-conditions between events can be avoided by setting a `priority`


## Configuring a Simulation

To minimze initial complexity when creating an environment, some options can be enabled within the scope of an environment
* `enableTickMetrics()` - See [tick metrics](advanced.md#operational-control)
* `enableComponentLogger()` - Enable the [component logger](events.md#component-logger) to track component status

## Dependency Injection

Kalasim is building on top of [koin](https://insert-koin.io/) to inject dependencies between elements of a simulation. This allows creating simulation entities such as resources, components or states conveniently without passing around references.

```kotlin
class Car : Component() {

    val gasStation by inject<GasStation>()
    
    // we could also distinguish different resources of the same type 
    // using a qualifier
//    val gasStation2 : GasStation by inject(qualifier = named("gs_2"))

    override fun process() = sequence {
        request(gasStation) {
            hold(2, "refill")
        }

        val trafficLight = get<TrafficLight>()
        wait(trafficLight, "green")
    }
}

createSimulation{
    dependency { TrafficLight() }
    dependency { GasStation() }

    // declare another gas station and specify 
    dependency(qualifier = named(FUEL_PUMP)) {}
    
    Car()
}
```
As shown in the example, the user can simply pull dependencies from the simulation environment using `get<T>()` or `inject<T>()`. This is realized with via [Koin Context Isolation](https://insert-koin.io/docs/reference/koin-core/context-isolation/) provided by a thread-local `DependencyContext`. This  context is a of type `DependencyContext`. It is automatically created when calling `createSimulation` or by instantiating a new simulation `Environment`. This context is kept as a static reference, so the user may omit it when creating simulation entities. Typically, dependency context management is fully transparent to the user.


```kotlin
Environment().apply {
    // implicit context provisioning (recommended)
    val inUse = State(true)

    // explicit context provisioning
    val inUse2 = State(true, koin = getKoin())
}
```

In the latter case, the context reference is provided explicitly. This is usually not needed nor recommended.

Instead of sub-classing, we can also use qualifiers to refer to dependencies of the same type

```kotlin
class Car : Component() {

    val gasStation1 : GasStation by inject(qualifier = named("gas_station_1"))
    val gasStation2 : GasStation by inject(qualifier = named("gas_station_2"))

    override fun process() = sequence {
        // pick a random gas-station
        request(gasStation, gasStation, oneOf = true) {
            hold(2, "refill")
        }
    }
}

createSimulation{
    dependency(qualifier = named("gas_station_1")) { GasStation() }
    dependency(qualifier = named("gas_station_2")) { GasStation() }
    
    Car()
}
```

### Threadsafe Registry

Because of its [thread locality](https://www.baeldung.com/java-threadlocal) awareness, the dependency resolver of `kalasim` allows for parallel simulations. That means, that even when running multiple simulations in parallel in different threads, the user does not have to provide a dependency context (called `koin`) argument when creating new simulation entities (such as [components](component.md)). 

For a simulation example with multiple parallel `Environment`s see [ATM Queue](examples/atm_queue.md#parallel-whatif)

### Simple Types

Koin does not allow injecting simple types. To inject simple variables, consider using a wrapper class. Example

```kotlin
//{!api/SimpleInject.kts!}
```

For details about how to use lazy injection with `inject<T>()` and instance retrieval with `get<T>()` see [koin reference](https://doc.insert-koin.io/#/koin-core/injection-parameters).

Examples

* [Traffic](examples/traffic.md)
* [Car Wash](examples/car_wash.md)
* [Gas Station](examples/gas_station.md)

## Randomness & Distributions

Experimentation in a simulation context relates to large part to controlling randomness. With `kalasim`, this is achieved by using probabilistic
[distributions](https://commons.apache.org/proper/commons-math/userguide/distribution.html) which are internally backed by [apache-commons-math](https://commons.apache.org/proper/commons-math/). A simulation always allows deterministic execution while still supporting pseudo-random sampling. When creating a new simulation [environment](#running-a-simulation), the user can provide a random seed which used [internally](https://github.com/holgerbrandl/kalasim/blob/9b75303163d96e1c6460798a8030ab5dc8070a51/src/main/kotlin/org/kalasim/Environment.kt#L103-L103) to initialize a random generator. By default kalasim, is using a fixed seed of [`42`](https://en.wikipedia.org/wiki/42_(number)#The_Hitchhiker's_Guide_to_the_Galaxy). Setting a seed is in particular useful when running a simulation repetitively (possibly with [parallelization](examples/atm_queue.md)).

```kotlin
createSimulation(randomSeed = 123){
    // internally kalasim will create a random generator
    //val r = Random(randomSeed)
    
    // this random generator is used automatically when
    // creating distributions
    val normDist = normal(2)   
}
```

With this internal random generator `r`, a wide range of  [probability distributions](https://github.com/holgerbrandl/kalasim/blob/master/src/main/kotlin/org/kalasim/Distributions.kt) are supported to provide controlled randomization. That is, the outcome of a simulation experiment will be the same if the same seed is being used.  


!!!important
All randomization/distribution helpers are accessible  from an `Environment` or `SimulationEntity` context only. That's because kalasim needs the context to associate the random generator of the simulation (which is also bound to the current [thread](#dependency-injection)).


Controlled randomization is a **key** aspect of every process simulation. Make sure to always strive for reproducibility by not using randomization outside the [simulation context](basics.md#simulation-environment).


### Continuous Distributions

#### Numeric Distributions 

The following continuous distributions can be used to model randomness in a simulation model

* `uniform(lower = 0, upper = 1)`
* `exponential(mean = 3)`
* `normal(mean = 0, sd = 1, rectify=false)`
* `triangular(lowerLimit = 0, mode = 1, upperLimit = 2)`
* `constant(value)`

All distributions functions provide common parameter defaults where possible, and are defined as extension functions of `org.kalasim.SimContext`. This makes the accessible in [environment definitions](basics.md#simulation-environment), all simulation entities, as well as [process definitions](component.md#process-definition).

The normal distribution can be  [rectified](https://en.wikipedia.org/wiki/Rectified_Gaussian_distribution), effectively capping sampled values at 0  (example `normal(3.days, rectify=true)`). This allows for  zero-inflated distribution models under controlled randomization.




Example:
```kotlin
object : Component() {
    val waitDist = exponential(3.3) // this is bound to env.rg
    
    override fun process() = sequence {
        hold(waitDist()) 
    }
} 
```

As shown in the example, probability distributions can be sampled with [invoke](https://kotlinlang.org/docs/operator-overloading.html#invoke-operator) `()`.


#### Constant Random Variables

The API also allow to model [constant random variables](https://en.wikipedia.org/wiki/Degenerate_distribution) using `const(<some-value>)`. These are internally resolved as `org.apache.commons.math3.distribution.ConstantRealDistribution`. E.g. consider the time until a request is considered as failed:

```kotlin
val dist =  constant(3)
// create a component generator with a fixed inter-arrival-time
ComponentGenerator(iat = dist) { Customer() }
```

#### Duration Distributions

Typically randomization in a discrete event simulation is realized by stochastic sampling of time durations. To provide a type-safe API for this very common usecase, all continuous distributions are also modeled to sample `kotlin.time.Duration` in addtion `Double`. Examples:

```kotlin
// Create a uniform distribution between 3 days and 4 days and a bit  
val timeUntilArrival = uniform(lower = 3.days, upper = 4.days + 2.hours)

// We can sample distributions by using invoke, that is () 
val someTime : Duration= timeUntilArrival() 

// Other distributions that support the same style
exponential(mean = 3.minutes)

normal(mean = 10.days, sd = 1.hours, rectify=true)

triangular(lowerLimit = 0.days, mode = 2.weeks, upperLimit = 3.years)

constant(42.days)
```

### Enumerations

Very often when working out simulation models, there is a need to sample with controlled randomization, from discrete populations, such as integer-ranges, IDs, enums or collections. Kalasim supports various integer distributions, uuid-sampling, as well as type-safe enumeration-sampling.

* `discreteUniform(lower, upper)` - Uniformly distributed integers in provided interval
* `uuid()` - Creates a random-controlled - i.e. deterministic - series of [universally unique IDs](https://en.wikipedia.org/wiki/Universally_unique_identifier) (backed by [`java.util.UUID`](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html))

Apart fom numeric distributions, also distributions over arbitrary types are supported with `enumerated()`. This does not just work with [`enums`](https://kotlinlang.org/docs/enum-classes.html) but with arbitrary types including [data classes](https://kotlinlang.org/docs/data-classes.html).

```kotlin
enum class Fruit{ Apple, Banana, Peach }

// create a uniform distribution over the fruits
val fruit = enumerated(values())
// sample the fruits
val aFruit: Fruit = fruit()

// create a non-uniform distribution over the fruits
val biasedFruit = enumerated(Apple to 0.7, Banana to 0.1, Peach to 0.2 )
// sample the distribution
biasedFruit()
```
   
### Custom Distributions

Whenever, distributions are needed in method signatures in `kalasim`, the more general interface `org.apache.commons.math3.distribution.RealDistribution` is being used to support a much [wider variety](https://commons.apache.org/proper/commons-math/javadocs/api-3.4/org/apache/commons/math3/distribution/RealDistribution.html) of distributions if needed. So we can also use other implementations as well. For example

```kotlin
ComponentGenerator(iat = NakagamiDistribution(1, 0.3)) { Customer() }
```


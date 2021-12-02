The beauty of discrete event simulation is its very limited vocabulary which still allow expressing complex system dynamics. In essence, `kalasim` relies on just a handful of elements to model real-world processes.

* [Components](component.md)
* [Resources](resource.md)
* [States](state.md)
* [Collections](collections.md)
* [Generators](component.md#component-generator)


## Simulation Environment

All entities in a simulation are governed by a environment context. Every simulation lives in exactly one such environment. The environment provides means for [controlled randomization](#randomness--distributions), [dependency injection](#dependency-injection), and most importantly manages the [event queue](#event-queue).

The environment context of a kalasim simulation is an instance of  `org.kalasim.Environment`, which can be created using simple instantiation or via a builder called `createSimulation`

```kotlin
val env : Environment = createSimulation(enableConsoleLogger = true){
    // Create simulation entities in here 
    Car()
    Resource("Car Wash")
}.run(5.0)
```

Within its environment, a simulation contains one or multiple [components](component.md) with [process definition](component.md#process-definition)s that define their behavior and interplay with other simulation entities.

Very often, the user will define custom Environments to streamline simulation API experience.

```kotlin
class MySim(val numCustomers:Int = 5) : Environment(){
    val customers = List(numCustomers){ Customer(it) }
}

val sim = MySim(10)
sim.run()

// analyze customers
sim.customers.first().statusTimeline.display()

```

To configure references first, an `Environment` can also be instantiated by configuring dependencies first with `configureEnvironment`. Check out the [Traffic](examples/traffic.md) example to learn how that works.


## Running a simulation

In a discrete event simulation a clear distinction is made between real time and simulation time. With *real time* we refer to the wall-clock time. It represents the execution time of the experiment. The *simulation time* is an attribute of the simulator.

!!! tip 
    The user can define a [transformation](advanced.md#tick-transformation) to map the internal clock to her wall-time clock

As shown in the example from above a simulation is usually started with `sim.run(ticks)`. Here `ticks` is the number of ticks, that is simulation time units. The simulation will progress for `ticks`. By doing so we may stop right in the middle of a [process](component.md#process-interaction).

Alternatively, we can also run until the event queue is empty (or forever depending on the model) by omitting the argument: 

```kotlin
sim.run(23) // run for 23 ticks
sim.run(5) // run for some more ticks

sim.run(until=42.tickTime) // run until internal simulation clock is 23 

sim.run() // run until event queue is empty
```

!!! tip
    A component can always stop the current simulation by calling `stopSimulation()` in its [process definition](component.md#process-definition). See [here](https://github.com/holgerbrandl/kalasim/blob/master/src/test/kotlin/org/kalasim/examples/api/StopSimulation.kt) for fully worked out example.


## Event Queue

The core of *kalasim* is an event queue ordered by scheduled execution time, that maintains a list of events to be executed. To provide good insert, delete and update performance, `kalasim` is using a [`PriorityQueue`](https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html) internally. Components are actively and passively scheduled for reevaluating their state. Technically, event execution refers to the continuation of a component's generator or execution function.

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

As pointed out in [Ucar, 2019](https://www.jstatsoft.org/article/view/v090i02), there are many situations where simultaneous events may occur in simulation. To provide a well-defined behavior in such situations, process interaction methods (namely  `wait`, `request`,  `activate` and `reschedule`) support user-define [schedule priorities]. With the parameter `priority`, it is possible to sort a component before or after other components, scheduled for the same time. Events with higher priority are executed first in situations where multiple events are scheduled for the same simulation time.

There are different predefined priorities which correspond the following sort-levels

* `LOWEST` (-20)
* `LOW` (-10)
* `NORMAL` (0)
* `IMPORTANT` (20)
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


## Dependency Injection

Kalasim is building on top of [koin](https://insert-koin.io/) to inject dependencies between elements of a simulation. This allows creating simulation entities such as resources, components or states conveniently without passing around references.

```kotlin
class Car() : Component() {

    val gasStation by inject<GasStation>()

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
    
    Car()
}
```
As shown in the example, the user can simply pull dependencies from the simulation environment using `get<T>()` or `inject<T>()`. This is realized with via [Koin Context Isolation](https://insert-koin.io/docs/reference/koin-core/context-isolation/) provided by a thread-local `DependencyContext`. This  context is a of type `KalasimContext`. It is automatically created when calling `createSimulation` or by instantiating a new simulation `Environment`. This context is kept as a static reference, so the user may omit it when creating simulation entities. Typically, dependency context management is fully transparent to the user.


```kotlin
Environment().apply{
    // implicit context provisioning (recommended)
    val inUse = State(true)

    // explicit context provisioning
    val inUse2 = State(true, koin=getKoin())
}
```

In the latter case, the context reference is provided explicitly. This is usually not needed nor recommended.

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

Experimentation in a simulation context relates to large part to controlling randomness. Here, this is achieved by using probabilistic
[distributions](https://commons.apache.org/proper/commons-math/userguide/distribution.html) which are internally backed by [apache-commons-math](https://commons.apache.org/proper/commons-math/). A simulation always allows deterministic execution while still supporting pseudo-random sampling. When creating a new simulation [environment](#simulation-runtime-environment), the user can provide a random seed which is internally resolved to a random generator to be used in process definitions.

```kotlin
createSimulation(randomSeed = 123){
    val randomGenerator = rg // which is resolved by Environment receiver     
}
```

With random generator, the following [distributions](https://github.com/holgerbrandl/kalasim/blob/master/src/main/kotlin/org/kalasim/Distributions.kt) are supported out of the box (with common defaults where possible) as extension functions on `Component` and `Environment`:

* `uniform(lower=0, upper=1)`
* `discreteUniform(lower, upper)`
* `exponential(mean)`
* `normal(mean=0, sd=1)`


Whenever, distributions are needed in method signatures in `kalasim`, the more general interface `org.apache.commons.math3.distribution.RealDistribution` is being used to support a much [wider variety](https://commons.apache.org/proper/commons-math/javadocs/api-3.4/org/apache/commons/math3/distribution/RealDistribution.html) of distributions if needed. So we can also use other implementations as well. For example

```kotlin
ComponentGenerator(iat=NakagamiDistribution(1,0.3)){ Customer() }
```

The API also include some convenience wrappers to provide fixed values for argument of `RealDistribution`. E.g. consider the  time until a request is considered as failed:

```kotlin
val dist =  3.asConstantDist()
ComponentGenerator(iat=dist){ Customer() }
```

Here, `3` is converted into a `org.apache.commons.math3.distribution.ConstantRealDistribution`.

Also, `RealDistribution.clip(0)` will cap the sampled values at 0 (or any other value,  allowing zero-inflated distribution models with controlled randomization
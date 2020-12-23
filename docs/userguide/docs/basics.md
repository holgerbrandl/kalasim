The beauty of discrete event simulation is its very limited vocabulary which still allow expressing complex system dynamics. In essence, `kalasim` relies on just a handful of types to model a simulation.

* [Components](component.md)
* [Resources](resource.md)
* [States](state.md)
* [Queues](component.md#queue)
* [Generators](component.md#generator)


## Execution & Process Model

The core of *kalasim* is an event queue ordered by scheduled execution time, that maintains a list of events to be executed. To provide good insert, delete and update performance, `kalasim` is using internally a [`PriorityQueue`](https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html). Components are actively and passively scheduled for reevaluating their state. Technically this relates to the component's continued with `process()` generator or execution function.

<!--NOTE simmer is also using time and pririty in its queue -->

<!--TODO simmer Ucar2019,p8 is mentioneing unscheduling as important use-case. Do we need/support that?-->


<!--https://stackoverflow.com/questions/19331362/using-an-image-caption-in-markdown-jekyll-->
<figure>
  <img src="../basics_images/event_loop.png"  alt="kalasim event model"/>
  <figcaption>Kalasim Execution Model</figcaption>
</figure>


As pointed out in [Urcar, 2019](https://www.jstatsoft.org/article/view/v090i02), there are many situations where simultaneous events may occur. To provide a well-defined behavior in such situations, all process interaction methods support a `priority` and a `urgent` parameter:

With `priority` which is 0 by default, it is possible to sort a component before or after other components, scheduled for the same time.

<!--The `urgent` parameters only applies to components scheduled with the same time and same `priority`.-->

This is particularly useful for race conditions. It is possible to change the priority of a component
by cancelling it prior to activating it with another priority.

In contrast to other DSE implementations, the user does not need to make sure that a resource `release()` is prioritized over a simultaneous `request()`. The egine will automatically reschedule tasks accordingly.
<!-- Also see Ucar2019,p9 (table 1)-->

<!--The priority can be accessed with the new Component.scheduled_priority() method.-->

## Execution Order

Order is defined by scheduled time. To avoid race conditions execution order be fine-tuned using `priority` and `urgent` which are supported for all methods that result in a rescheduling of a component, namely  `wait`, `request`,  `activate` and `reschedule`


## Simulation Runtime Environment

The execution context of a kalasim simulation is an `Environment`, which can be created with

```kotlin
val env : Environment = createSimulation(enableTraceLogger = true){
    // Create components in here 
    Car()
    
    // To disambiguate between multiple simulations, provide a reference to the koin context
    // The koin argument can be omitted if just a single simulation is being used
    Component(koin=getKoin())
}.run(5.0)
```

To configure references, an `Environment` can also be instantiated by configuring dependencies first with `configureEnvironment`. Check out the  [Extended Cars](examples.md#extended-cars) example to learn how that works.


## Dependency Injection

Kalasim is building on top of [koin](https://insert-koin.io/) to inject dependencies between elements of a simulation.

As pragmatic approach, it is using a global application context by default, but does allow for parallel simulations with [Koin Isolation](https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3). For a simulation example with multiple `Environment` see `https://github.com/holgerbrandl/kalasim/tree/master/src/test/kotlin/org/kalasim/test/EnvTests.kt`


Koin does not allow injecting simple types. To inject simple variables, consider using a wrapper class. Example

```kotlin
//{!SimpleInject.kts!}
```

For details about how to use lazy injection with `inject<T>()` and instance retrieval with `get<T>()` see [koin reference](https://doc.insert-koin.io/#/koin-core/injection-parameters).

## Randomness & Distributions

Experimentation in a simulation context relates to large part to controlling randomess. Here, this is achieved by using probabilistc
[distributions](https://commons.apache.org/proper/commons-math/userguide/distribution.html) which are provided via `apache-commons-math`. A simulation always allows deterministic execution while still supportin pseudo-random sampling. To do so, distributions need to be configured to use kalasim random generator.

Example
```kotlin
yield(hold(UniformRealDistribution(env.rg, 5.0, 15.0).sample()))
```

The API also include some convenience wrappers to provide fixed values for argument of `RealDistribution`. E.g. consider the  time until a request is considered as failed:

```kotlin
val r = Resource()
c.request(r, failAt = 3.asConstantDist())
```

Here, `3` is converted into a `org.apache.commons.math3.distribution.ConstantRealDistribution`. By doing so, we can provide more typed signatures across the entire API. Instead of support methods that accept fixed values for waiting times etc, we simply rely on fixed random distribution to reduce API complexity while maintaining full flexibility.

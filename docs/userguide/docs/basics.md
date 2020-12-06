##  Overview


The beauty of discrete event simulation is its very limited vocabulary which still allow expressing complex system dynamics. In essence, `kalasim` relies on just a handful of types to model a simulation.

* [Components](component.md)
* [Generators](component.md#generator)
<!--(https://www.salabim.org/manual/ComponentGenerator.html)-->
* [Queues](component.md#queue)
* [States](https://www.salabim.org/manual/State.html)
* [Monitors]() (via apache-commons-math)


## Event Loop

The core of kalasim is a an event-loop. Components are actively and passively scheduled for reevaluating their state. Technically this relates to the component's continued with `process()` generator or execution function.

**{tbd}** image here


## Randomness & Distributions

Experimentation in a simulation context relates to large part to controlling randomess. Here, this is achieved by using probabilistc
[distributions](https://commons.apache.org/proper/commons-math/userguide/distribution.html) which are provided via `apache-commons-math`. A simulation always allows deterministic execution while still supportin pseudo-random sampling. To do so, distributions need to be configured to use kalasim random generator.

Example
```kotlin
yield(hold(UniformRealDistribution(env.rg, 5.0, 15.0).sample()))
```

The API also include some convenience wrappers to provide fixed values for argument of `RealDistribution`. E.g. consider the  time until a request is considered as failed:

```
val r = Resource()
c.request(r, failAt = 3.asConstantDist())
```
Here, 3 is converted into a `ConstantRealDistribution`. By doing so we can provide a more typed signatures across the entire API.

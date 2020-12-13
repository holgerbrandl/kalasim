The beauty of discrete event simulation is its very limited vocabulary which still allow expressing complex system dynamics. In essence, `kalasim` relies on just a handful of types to model a simulation.

* [Components](component.md)
* [Resources](resource.md)
* [States](state.md)
* [Queues](component.md#queue)
* [Generators](component.md#generator)


## Execution & Process Model

The core of kalasim is a an event-loop. Components are actively and passively scheduled for reevaluating their state. Technically this relates to the component's continued with `process()` generator or execution function.


<!--https://stackoverflow.com/questions/19331362/using-an-image-caption-in-markdown-jekyll-->
<figure>
  <img src="../basics_images/event_loop.png"  alt="kalasim event model"/>
  <figcaption>Kalasim Execution Model</figcaption>
</figure>

## Dependency Injection

Kalasim is building on top of koin to inject dependencies between elements of a simulation.

Koin does not allow to inject simple types. To inject simple variables, consider using a wrappe class. Example
``` python
# Python Program to convert temperature in celsius to fahrenheit

# change this value for a different result
celsius = 37.5

# calculate fahrenheit
fahrenheit = (celsius * 1.8) + 32
print('%0.1f degree Celsius is equal to %0.1f degree Fahrenheit' %(celsius,fahrenheit))
```

```kotlin
{!kotlin/SimpleInject.kts!}
```

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
Here, `3` is converted into a `org.apache.commons.math3.distribution.ConstantRealDistribution`. By doing so, we can provide more typed signatures across the entire API. Instead of support methods that accept fixed values for waiting times etc, we simply rely on fixed random distribution to reduce API complexity while maintaining full flexibility.

# Welcome to `kalasim`

[ ![Download](https://api.bintray.com/packages/holgerbrandl/github/kalasim/images/download.svg) ](https://bintray.com/holgerbrandl/github/kalasim/_latestVersion)  [![Build Status](https://github.com/holgerbrandl/kalasim/workflows/build/badge.svg)](https://github.com/holgerbrandl/kalasim/actions?query=workflow%3Abuild) [![Gitter](https://badges.gitter.im/kalasim.svg)](https://gitter.im/holgerbrandl/kalasim)

`kalasim` is a [discrete event simulator](theory.md#what-is-discrete-event-simulation) that enables complex, dynamic process models. It provides a statically typed API, dependency injection, modern persistence, structured logging and automation capabilities.

`kalasim` is written in [Kotlin](https://kotlinlang.org/), is designed around suspendable [coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) for process definitions, runs on the [JVM](https://github.com/openjdk/jdk) for performance and scale, is built with [koin](https://github.com/InsertKoinIO/koin) as dependency wiring framework, and is using [common-math](https://commons.apache.org/proper/commons-math/) for stats and distributions. See [acknowledgements](about.md#acknowledgements) for further references.

One cornerstone of successful discrete event simulation is visualization. `kalasim` is agnostic regarding a  visualization frontend, but we provide bindings/examples using [plotly.kt](https://github.com/mipt-npm/plotly.kt), [lets-plot](https://github.com/JetBrains/lets-plot-kotlin) as well as [kravis](https://github.com/holgerbrandl/kravis).

In contrast to many other simulation tools, `kalasim` is neither low-code nor no-code. It is _code-first_ to enable change tracking, scaling, refactoring, CI/CD, unit-tests, and the rest of the gang that makes simulation development fun.


## Core Features

`kalasim` is a generic [process-oriented](theory.md) discrete event simulation (DES) engine.

* [Simulation entities](component.md) have a generative process description that defines the interplay with other entities
* There is a well-defined rich process interaction vocabulary, including [hold](component.md#hold), [request](component.md#request), [wait](component.md#wait) or [passivate](component.md#passivate)
* An [event trigger queue](basics.md#event-queue) maintains future action triggers and acts as sole driver to progress simulation state
* Built-in [monitoring](monitors.md) and [statistics](analysis.md) gathering across the entire API

Find out more about the [basics](basics.md) of a `kalasim` simulation.

## First Example

Let’s start with a very simple model. The example demonstrates the main mode of operation, the core API and the component process model implemented in `kalasim`. We want to build a simulation where a single car is driving around for a some time before arriving at its destination.

```kotlin
//{!api/Cars.kts!}
```

<!--This example corresponds to the `Cars` `salabim` example https://www.salabim.org/manual/Modeling.html-->

For each (active) component we (can) define a type such as:

```kotlin
class Car : Component()
```

The class inherits from `org.kalasim.Component`.

Our car depends on a [state](state.md) `TrafficLight` and [resource](resource.md) `Driver` for operation. To implement that, we first declare these dependencies with `dependency{}` in the main body of the simulation, and secondly [inject](basics.md#dependency-injection) them into our car with `get<T>`. Note, we could also directly inject states and resources with `dependency {State("red")}` without sub-classing.

Although it is possible to define other processes within a class,
the standard way is to define a generator function called `process` in the class.
A generator is a function that returns `Sequence<Component>`. Within these process definitions we use [`suspend`](https://kotlinlang.org/docs/reference/coroutines/basics.html#your-first-coroutine)able interaction function calls as a [signal](basics.md#dependency-injection) to give control to the centralized [event loop](basics.md#event-queue).

In this example,

```kotlin
hold(1.0)
```

suspends execution control and *comes back* after 1 time unit (referred to as _tick_). Apart from [`hold`](component.md#hold), `kalasim` supports a rich vocabulary of interaction methods including [`passivate`](component.md#passivate), [`request`](component.md#request), [`wait`](component.md#wait) and [`component`](component.md#standby).


The main body of every `kalasim` model usually starts with:
```
createSimulation(enableConsoleLogger = true){
...
}
```
Here, we enable event logging of state changes to see the status of simulation on the console. After declaring our dependencies, we instantiate a single car with `Car()`. It automatically is assigned the name *Car.0*.

As there is a generator function called `process` in `Car`, this process description will be activated (by default at time `now`, which is `0` by default at the beginning of a simulation). It is possible to start a process later, but this is by far the most common way to start a process.

With

```kotlin
run(5.0)
```

we start the simulation and get back control after 5 ticks. A component called *main* is defined under the hood to get access to the main process.

When we run this program, we get the following output (displayed as table for convenience):

```
time   current  receiver  action                             info               
------ -------- --------- ---------------------------------- -------------------
.00             main      Created
.00    main
.00             Driver.1  Created                             capacity=1
.00             Car.1     Created
.00                       activate                           scheduled for .00
.00             main      run +5.00                          scheduled for 5.00
.00    Car.1    Car.1
.00                       Requesting 1.0 from Driver.1 
.00                       Claimed 1.0 from 'Car.1'
.00                       Request honor Driver.1             scheduled for .00
.00
.00                       hold +1.00                         scheduled for 1.00
1.00
1.00                      entering waiters of TrafficLight.1
1.00                      wait                               scheduled for <inf>
5.00   main     main
Process finished with exit code 0
```

There are plenty of other more advanced (that is more **fun**!) examples listed in [examples](examples.md) chapter.


##  How to contribute?

Feel welcome to post ideas and suggestions to the project [tracker](https://github.com/holgerbrandl/kalasim/issues).

We always welcome pull requests. :-)


## Support

Feel welcome to post questions and ideas in the project's [gitter chat](https://gitter.im/holgerbrandl/kalasim)

Feel also invited to chat with us in the [kotlinlang.slack.com](http://kotlinlang.slack.com) in the `#datascience` channel.


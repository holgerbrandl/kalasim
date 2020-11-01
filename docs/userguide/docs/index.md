# Welcome to `kalasim`

[ ![Download](https://api.bintray.com/packages/holgerbrandl/github/kalasim/images/download.svg) ](https://bintray.com/holgerbrandl/github/kalasim/_latestVersion)  [![Build Status](https://travis-ci.org/holgerbrandl/kalasim.svg?branch=master)](https://travis-ci.org/holgerbrandl/kalasim) [![Gitter](https://badges.gitter.im/kalasim.svg)](https://gitter.im/kalasim/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

`kalasim` is a discrete event simulator with type support, dependency injection, modern persistence and logging to enable large-scale, industrial-ready simulations.


`kalasim` started off as a blunt rewrite of [salabim](https://www.salabim.org/). `salabim` is written in python and provides a great model to built simulations. `kalasim` reimplements all core APIs of `salabim` in a more typesafe API while providing better test coverage, real-time capabilities and (arguably) more modern built-in support for visualization.


`kalsim` is written in [Kotlin](https://kotlinlang.org/), designed on top of [koin](https://github.com/InsertKoinIO/koin) as dependency injection framework, is using [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for event tracking and persistence, and [plotly.kt](https://github.com/mipt-npm/plotly.kt) as well as [`kravis`](https://github.com/holgerbrandl/kravis) for visualization.

In contrast to many other simulation tools, `kalasim` is neither low-code nor no-code. It is _just-code_ to enable change tracking, scaling, refactoring, CI/CD, unit-tests, and the rest of the gang that makes development fun.


## First Example

Let’s start with a very simple model, to demonstrate the basic structure, process interaction, component definition and output.

We want to build a simulation where a single car is driving around for a some time before arriving at its destination.
```kotlin
class Car : Component() {
    override suspend fun ProcContext.process() {
            // drive around for 1 second
            yield(hold(1.0))
            // and terminate when reaching the destination
            yield(terminate())
    }
}

createSimulation{
    Car()
}.run(5.0)
```

<!--This example corresponds to the `Cars` `salabim` example https://www.salabim.org/manual/Modeling.html-->


Although the example lacks dynamic aspects that make simulations fun, it demonstrates the main mode of operation, the core API and the state model implemented in `kalasim`.

The main body of every `kalasim` model usually starts with:
```
createSimulation{
...
}
```

For each (active) component we (can) define a type such as:

```
class Car : Component()
```

The class inherits from `org.github.holgerbrandl.kalasim.Component`.

Although it is possible to define other processes within a class,
the standard way is to define a generator function called `process` in the class.
A generator is a function with at least one `yield` statement. These are used in the `kalasim` context as a signal to give control to the sequence mechanism.

In this example,

```kotlin
yield(hold(1.0))
```

gives control, to the sequence mechanism and *comes back* after 1 time unit. We will see later other uses of yield like `passivate`,
`request`, `wait` and `standby`.

In the main body an instance of a car is created by `Car()`. It automatically gets the name *Car.0*.
As there is a generator function called process in Car, this process description will be activated (by default at time now, which is 0 here). It is possible to start a process later, but this is by far the most common way to start a process.

With :

```
run(5.0)
```

we start the simulation and get back control after 5 time units. A component called *main* is defined under the hood to get access to the main process.

When we run this program, we get the following output (displayed as table for convenience):

```
time      current component        component                action      info                          
--------- ------------------------ ------------------------ ----------- -----------------------------
.00                                main                     DATA        create
.00       main
.00                                Car.1                    DATA        create
.00                                Car.1                    DATA        activate
.00                                main                     CURRENT     run +5.0
.00       Car.1
.00                                Car.1                    CURRENT     hold +1.0
1.00                               Car.1                    CURRENT
1.00                               Car.1                    DATA        ended
5.00      main
Process finished with exit code 0
```

There are plenty of other more advanced (that is more **fun**!) examples listed in [examples](examples.md) chapter.


##  How to contribute?

Feel welcome to post ideas and suggestions to the project [tracker](https://github.com/holgerbrandl/kalasim/issues).

We always welcome pull requests. :-)


## Support

Feel welcome to post questions and ideas in the project's [gitter chat](https://gitter.im/holgerbrandl/kalasim)

Feel also invited to chat with us in the [kotlinlang.slack.com](http://kotlinlang.slack.com) in the `#datascience` channel.


## Acknowledgements


`kalasim` started off as a blunt rewrite of [salabim](https://www.salabim.org/). The latter provided a great path into simulation, states and resources. Its excellent documentation and wonderful examples made this project possible after all.


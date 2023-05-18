# Welcome to `kalasim`

[ ![Download](https://img.shields.io/github/v/release/holgerbrandl/kalasim) ](https://github.com/holgerbrandl/kalasim/releases)  [![Build Status](https://github.com/holgerbrandl/kalasim/workflows/build/badge.svg)](https://github.com/holgerbrandl/kalasim/actions?query=workflow%3Abuild)  [![slack](https://img.shields.io/badge/kotlinlang%20slack-kalasim-yellowgreen)](https://kotlinlang.slack.com/messages/kalasim/)
[![github-discussions](https://img.shields.io/badge/discuss-kalasim-orange)](https://github.com/holgerbrandl/kalasim/discussions)

`kalasim` is a [discrete event simulator](theory.md#what-is-discrete-event-simulation). It provides a statically typed API, dependency injection, modern persistence, structured logging and automation capabilities.

kalasim is designed for simulation practitioners, process analysts and industrial engineers, who need to go beyond the limitations of existing simulation tools to model and optimize their business-critical use-cases.

In contrast to many other simulation tools, `kalasim` is neither low-code nor no-code. It is _code-first_ to enable change tracking, scaling, refactoring, CI/CD, unit-tests, and the rest of the gang that makes simulation development fun.

`kalasim` is written in [Kotlin](https://kotlinlang.org/), is designed around suspendable [coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) for process definitions, runs on the [JVM](https://github.com/openjdk/jdk) for performance and scale, is built with [koin](https://github.com/InsertKoinIO/koin) as dependency wiring framework, and is using [common-math](https://commons.apache.org/proper/commons-math/) for stats and distributions. See [acknowledgements](about.md#acknowledgements) for further references. `kalasim` is agnostic regarding a visualization frontend, but we provide bindings/examples using [plotly.kt](https://github.com/mipt-npm/plotly.kt), [lets-plot](https://github.com/JetBrains/lets-plot-kotlin) as well as [kravis](https://github.com/holgerbrandl/kravis).


---

!!! tip "Meet kalasim at KotlinConf" 
    We presented at [KotlinConf 2023](https://kotlinconf.com/talks/389146/) in Amsterdam! We were there together with other technology leads from cloud, mobile & data-science for a great week of discussion and knowledge sharing. Our talk  about "Make more money by modeling and optimizing your business processes with Kotlin" was well perceived and a lot of fun. Enjoy:


<div class="video-wrapper">
  <iframe width="750" height="500" src="https://www.youtube.com/embed/1pqVCOZp9Ko" frameborder="0" allowfullscreen></iframe>
</div>

[//]: # (https://www.youtube.com/watch?v=lo1BhmF5DVU)

---



## Core Features

`kalasim` is a generic [process-oriented](theory.md) discrete event simulation (DES) engine.

* [Simulation entities](component.md) have a generative process description that defines the interplay with other entities
* There is a well-defined rich process interaction vocabulary, including [hold](component.md#hold), [request](component.md#request), [wait](component.md#wait) or [passivate](component.md#passivate)
* An [event trigger queue](basics.md#event-queue) maintains future action triggers and acts as sole driver to progress simulation state
* Built-in [monitoring](monitors.md) and [statistics](analysis.md) gathering across the entire API

Find out more about the [basics](basics.md) of a `kalasim` simulation.

## First Example

Let’s start with a very simple model. The example demonstrates the main mode of operation, the core API and the component process model implemented in `kalasim`. We want to build a simulation where a single car is driving around for a some time before stopping in front of a red traffic light.

```kotlin
//{!api/Car.kts!}
```

Curious about an in-depth analysis of this example? It's your lucky day, see [here](examples/car.md).

##  How to contribute?

Feel welcome to post ideas and suggestions to the project [tracker](https://github.com/holgerbrandl/kalasim/issues).

We always welcome pull requests. :-)


## Support

Feel welcome to post questions and ideas in the project's [discussion forum](https://github.com/holgerbrandl/kalasim/discussions)

Feel also invited to chat with us in the [kotlinlang.slack.com](http://kotlinlang.slack.com) in the `#kalasim` channel.


# Welcome to `kalasim`

[ ![Download](https://img.shields.io/github/v/release/holgerbrandl/kalasim) ](https://github.com/holgerbrandl/kalasim/releases)  [![Build Status](https://github.com/holgerbrandl/kalasim/workflows/build/badge.svg)](https://github.com/holgerbrandl/kalasim/actions?query=workflow%3Abuild)  [![slack](https://img.shields.io/badge/kotlinlang%20slack-kalasim-yellowgreen)](https://kotlinlang.slack.com/messages/kalasim/)
[![github-discussions](https://img.shields.io/badge/discuss-kalasim-orange)](https://github.com/holgerbrandl/kalasim/discussions)

`kalasim` is a [discrete event simulation](theory.md#what-is-discrete-event-simulation) framework
providing a statically typed API, dependency injection, enterprise-grade persistence, structured logging, and
comprehensive automation capabilities for building digital twins.

`kalasim` is designed for simulation practitioners, process analysts, and industrial engineers who require advanced
modeling capabilities beyond conventional simulation tools to analyze and optimize business-critical systems and
processes.

Unlike many simulation tools, `kalasim` is neither low-code nor no-code. It follows a _code-first_ philosophy, enabling
version control, scalability, refactoring, continuous integration and deployment (CI/CD), comprehensive unit testing,
and modern software engineering practices that ensure robust simulation development.

`kalasim` is implemented in [Kotlin](https://kotlinlang.org/), leveraging
suspendable [coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) for intuitive process
definitions. The framework operates on the [JVM](https://github.com/openjdk/jdk) to deliver enterprise-level performance
and scalability, utilizes [koin](https://github.com/InsertKoinIO/koin) for dependency injection, and
integrates [Apache Commons Math](https://commons.apache.org/proper/commons-math/) for statistical analysis and
probability distributions. For additional technical references, see [acknowledgements](about.md#acknowledgements).
`kalasim` maintains visualization framework independence while providing integration examples
for [plotly.kt](https://github.com/mipt-npm/plotly.kt), [lets-plot](https://github.com/JetBrains/lets-plot-kotlin),
and [kravis](https://github.com/holgerbrandl/kravis).

## 2026 - Advancing Digital Twin Development

Building on our deep understanding of the challenges faced by simulation practitioners, we are pleased to announce
significant enhancements to `kalasim` in 2026, further strengthening its capabilities for enterprise digital twin
development.

* **Enhanced Performance**: Significant improvements in simulation performance through optimized event processing and
  memory management architectures, enabling accurate modeling of larger, more complex industrial systems
* **Modern Technology Stack**: Full compatibility with Kotlin 2.2, providing access to advanced language features and
  state-of-the-art development tooling for professional simulation engineering
* **Industry-Validated**: `kalasim` has been successfully deployed across diverse industrial sectors—from automotive
  supply chain optimization to precision execution control in semiconductor manufacturing—demonstrating proven
  reliability in production-critical environments
* **Expanding Professional Community**: A growing network of researchers, practitioners, and industrial engineers
  actively contributing to the framework through comprehensive documentation, peer-reviewed case studies, and
  collaborative knowledge exchange

---

!!! tip "KotlinConf Presentation"

We presented at [KotlinConf](https://kotlinconf.com/talks/389146/) in Amsterdam, joining technology leaders from cloud
computing, mobile development, and data science for collaborative knowledge exchange. Our presentation on "Make more
money by modeling and optimizing your business processes with Kotlin" was well-received by the professional community:


<div class="video-wrapper">
  <iframe width="750" height="500" src="https://www.youtube.com/embed/1pqVCOZp9Ko" frameborder="0" allowfullscreen></iframe>
</div>

[//]: # (https://www.youtube.com/watch?v=lo1BhmF5DVU)

---



## Core Features

`kalasim` is a comprehensive [process-oriented](theory.md) discrete event simulation (DES) engine designed for
industrial applications.

* [Simulation entities](component.md) employ generative process descriptions that define interactions and dependencies
  with other system entities
* A well-defined, expressive process interaction vocabulary,
  including [hold](component.md#hold), [request](component.md#request), [wait](component.md#wait),
  and [passivate](component.md#passivate) operations
* An [event trigger queue](basics.md#event-queue) that maintains scheduled future actions and serves as the primary
  mechanism for simulation state progression
* Integrated [monitoring](monitors.md) and [statistical analysis](analysis.md) capabilities throughout the entire API

Find out more about the [basics](basics.md) of a `kalasim` simulation.

## First Example

Let’s start with a very simple model. The example demonstrates the main mode of operation, the core API and the component process model implemented in `kalasim`. We want to build a simulation where a single car is driving around for a some time before stopping in front of a red traffic light.

```kotlin
//{!api/Car.kts!}
```

Curious about an in-depth analysis of this example? It's your lucky day, see [here](examples/car.md).

## How to Contribute

We welcome contributions from the community. Please submit ideas and suggestions to the
project [issue tracker](https://github.com/holgerbrandl/kalasim/issues).

Pull requests are always appreciated and will be reviewed promptly.

## Support

For questions and discussions, please visit the
project's [discussion forum](https://github.com/holgerbrandl/kalasim/discussions).

You are also invited to join our community on [kotlinlang.slack.com](http://kotlinlang.slack.com) in the `#kalasim`
channel.


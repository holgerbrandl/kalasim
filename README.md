# kalasim

Discrete Event Simulator


[ ![Download](https://api.bintray.com/packages/holgerbrandl/github/kalasim/images/download.svg) ](https://bintray.com/holgerbrandl/github/kalasim/_latestVersion)  [![Build Status](https://travis-ci.org/holgerbrandl/kalasim.svg?branch=master)](https://travis-ci.org/holgerbrandl/kalasim) [![Gitter](https://badges.gitter.im/kalasim.svg)](https://gitter.im/kalasim/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

This repo is a rather blunt copy of https://www.salabim.org/. It reimplements all core APIs of salabim in a more typesafe API, better test coverage, real-time capabilities,


## Installation

To get started simply add it as a dependency via Jcenter:
```
implementation "com.github.holgerbrandl:kalasim:0.1"
```

You can also use [JitPack with Maven or Gradle](https://jitpack.io/#holgerbrandl/kalasim) to build the latest snapshot as a dependency in your project.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
        implementation 'com.github.holgerbrandl:kalasim:-SNAPSHOT'
}
```

To build and install it into your local maven cache, simply clone the repo and run
```bash
./gradlew install
```

##  Features

Currently we support a subset of features as documented under https://www.salabim.org/manual/ including
* [Components](https://www.salabim.org/manual/Component.html)
* [ComponentGenerator](https://www.salabim.org/manual/ComponentGenerator.html)
* [Queue](https://www.salabim.org/manual/Queue.html)
* [Distributions](https://www.salabim.org/manual/Distributions.html) (via apache-commons-math)
* [Monitor](https://www.salabim.org/manual/Monitor.html) (via apache-commons-math)

Planned
* [Resource](https://www.salabim.org/manual/Resource.html)
* [State](https://www.salabim.org/manual/State.html)


Not planned
* [Animation](https://www.salabim.org/manual/Animation.html) - which we believe should live in a different project


## Example

```kotlin
class Car : Component() {
    override suspend fun SequenceScope<Component>.process() {
            // wait for 1 sec
            yield(hold(1.0))
            // and terminate it
            yield(terminate())
    }
}

createSimulation{
    Car()
}.run(5.0)

```

This example corresponds to the `Cars` `salabmim` example https://www.salabim.org/manual/Modeling.html

## References

Its started off as a blunt clone of `salabmin`, so the best reference is
* https://www.salabim.org/
* [Salabim paper](https://www.semanticscholar.org/paper/salabim%3A-discrete-event-simulation-and-animation-in-Ham/b513ce3d7cd56c478bb045d7080f7e34c0eb20de)


`kalasim` is built on top of some great libraries
* https://github.com/InsertKoinIO/koin for dependency injection,
* [apache-commons-math](http://commons.apache.org/proper/commons-math/) for stats and distributions

Further reading
* https://jabm.sourceforge.io/doc/easss2013/jabm-beamer.pdf
* https://en.wikipedia.org/wiki/Comparison_of_agent-based_modeling_software


Other discrete simulation engines
*  https://github.com/aybabtme/desim - written in

## Terminology

> Generator

A `Component` that contains at least one yield.
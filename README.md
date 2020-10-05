# desim

{D}iscrete {Event} {Sim}ulator


[ ![Download](https://api.bintray.com/packages/holgerbrandl/github/desim/images/download.svg) ](https://bintray.com/holgerbrandl/github/desim/_latestVersion)  [![Build Status](https://travis-ci.org/holgerbrandl/desim.svg?branch=master)](https://travis-ci.org/holgerbrandl/desim) [![Gitter](https://badges.gitter.im/desim.svg)](https://gitter.im/desim/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

This repo is a rather blunt copy of https://www.salabim.org/. It reimplements all core APIs of salabim in a more typesafe API, better test coverage, real-time capabilities,


## Installation

To get started simply add it as a dependency via Jcenter:
```
implementation "com.github.holgerbrandl:desim:0.1"
```

You can also use [JitPack with Maven or Gradle](https://jitpack.io/#holgerbrandl/desim) to build the latest snapshot as a dependency in your project.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
        implementation 'com.github.holgerbrandl:desim:-SNAPSHOT'
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


`desim` is built on top of some great libraries
* https://github.com/InsertKoinIO/koin for dependency injection,
* [apache-commons-math](http://commons.apache.org/proper/commons-math/) for stats and distributions

Further reading
* https://jabm.sourceforge.io/doc/easss2013/jabm-beamer.pdf
* https://en.wikipedia.org/wiki/Comparison_of_agent-based_modeling_software


## Terminology

> Generator

A `Component` that contains at least one yield.
# Kalasim Release History

## v0.8-SNAPSHOT

Not yet released

* Added `RequestScopeContext` to honor-block of `request` including `requestingSince` time (relates to [#27](https://github.com/holgerbrandl/kalasim/issues/27)) 

Minor enhancements
* Reworked distribution support API for better API experience to enable controlled randomization in process models

## v0.7 

Released 2021-11-27

See [release announcement](articles/2021-11-27-kalasim-v07.md)


Major enhancements

* Reworked event & metrics logging API
* Introduced [`ComponentList`](collections.md#list)
* Implemented [ticks metrics](advanced.md#operational-control) monitor (fixes [#9](https://github.com/holgerbrandl/kalasim/issues/9))
* New [timeline](https://www.kalasim.org/resource/#timeline) and [activity log](https://www.kalasim.org/resource/#activity-log)  attributes to resources for streamlined usage and capacity analysis  
* Extended `display()` support API on all major components and their collections (including `Resource`, `Component` or `List<Component>`, `MetricTimeline`) (fixes [#18](https://github.com/holgerbrandl/kalasim/issues/18))
* Thread-local context registry enabled via [Koin Context Isolation](https://insert-koin.io/docs/reference/koin-core/context-isolation/) (fixes [#20](https://github.com/holgerbrandl/kalasim/issues/20))
* Dramatically improved simulation performance

Documentation

* New chapter about [collections](collections.md)
* Revised [resource](resource.md) documentation
* Rewritten [ATM](https://www.kalasim.org/examples/atm_queue/) example to better illustrate parallelization and generators
* New example [Bridge Games](examples/bridge_game.md)
* Started new canonical complex simulation example: [emergency room](examples/emergency_room.md)

Minor enhancements

* Added possibility [stop](basics.md#running-a-simulation) a simulation from a process definition using ``stopSimulation`
* Introduced `AssertMode`s (`Full`, `Light` (default), `None`) to enable/disable internal [consistency checks](https://www.kalasim.org/advanced/#internal-state-validation). This will optimize performance by another ~20% (depending on simulation logic)
* Improved request priority API
* Allow for runtime reconfiguration of `ClockSync` to enable adjustable simulation speed
* Lifted `Component` sub-type requirement from `ComponentQueue`
* Fixed `oneOf` in `request()`
* Redesigned `honorBlock` in `request()` to return `Unit` and to provide claimed resource via `it` 
```kotlin
request(doctorFoo, doctorBar, oneOf = true) { doctor ->
    println("patient treated by $doctor")
}
```
* Added `RealDistribution.clip` to allow zero-inflated distribution models with controlled randomization

Breaking changes

* Removed `components` from `Environment` and created `componentCollector` as optional replacement
* Redesigned events & metrics API 
* Updated to `koin` v3.1 (fixes [#15](https://github.com/holgerbrandl/kalasim/issues/15)): `GlobalContext` has been replaced with `DependencyContext`
* Established use of `TickTime` across the entire API to disambiguate simulation time instants from durations, which are still modelled as `Double`
* Changed `Component.now `and `Environment.now` to new value class `TickTime` for better type safety
* Simplified `ClockSync` API by removing redundant `speedUp` parameter
* `Component.status` has been renamed to `Component.componentState` to enable extending classes to use the property name `status` for domain modelling
* Removed requirement to implement `info` in `SimulationEntity`
* Moved stochastic distributions support API to from `Component` to `SimulationEntity`
* Removed `Component::setup` because the user can just use an `init{}` block instead
* Migrated release channel from jcenter to [maven-central](https://search.maven.org/artifact/com.github.holgerbrandl/kalasim)


## v0.6

Released 2021-02-12 -> Updated to v0.6.6 on 2021-05-05

Major Enhancements

* Added `selectResource()` to [select from resources with policy](https://www.kalasim.org/resource/#resource-selection)
```kotlin
val doctors = List(3) { Resource() }
val selected = selectResource( doctors, policy = SHORTEST_QUEUE )
```

* New suspending [`batch`](https://www.kalasim.org/component/#batching) interaction to group an entity stream into blocks
```kotlin
val queue = ComponentQueue<Customer>()
val batchLR: List<Customer> = batch(queue, 4, timeout = 10)
```

* Added option to [configure](advanced.md#tick-transformation) a tick to wall time transformer 
```kotlin
createSimulation {
    tickTransform = OffsetTransform(Instant.now(), TimeUnit.MINUTES)

    run(Duration.ofMinutes(90).asTicks())
    println(asWallTime(now))
}
```

* Added [lifecycle records](analysis.md#tabular-interface) to streamline component state analyses

* Changed `ComponentGenerator` to allow generating arbitrary types (and not just `Component`s)
```kotlin
ComponentGenerator(uniform(0,1)){ counter -> "smthg no${counter}"}
```

* Added `forceStart` to `ComponentGenerator` to define if an arrival should be happen when it is activated for the first time

* Changed scheduling priority from `Int` to inline class `Priority` (with defaults `NORMAL`, `HIGH`, `LOW`) in all interaction methods for more typesafe API

* Started bundled simulations for adhoc experimentation and demonstration by adding [M/M/1 queue](https://en.wikipedia.org/wiki/M/M/1_queue) `MM1Queue`

* Added support for pluggable visualization backend. Currently [kravis](https://github.com/holgerbrandl/kravis) and [lets-plot](https://github.com/JetBrains/lets-plot-kotlin) are supported. For jupyter-notebook examples [mm1-queue analysis](https://nbviewer.jupyter.org/github/holgerbrandl/kalasim/blob/master/simulations/notebooks/simu-letsplot.ipynb)
```kotlin
// simply toggle backend by package import
import org.kalasim.plot.letsplot.display
// or
//import org.kalasim.plot.kravis.display

MM1Queue().apply {
    run(100)
    server.claimedMonitor.display()
}
```

* New Example: ["The ferryman"](examples/ferryman.md)
* New Example: [Office Tower](examples/office_tower.md)

## v0.5

Released 2021-01-12

Major Enhancements

* Added first [jupyter notebook](https://github.com/holgerbrandl/kalasim/blob/master/simulations/notebooks/dining.ipynb) example
* New [depletable resource](https://www.kalasim.org/resource/#depletable-resources) type
* New [statistical distributions](https://www.kalasim.org/basics/#randomness-distributions) API
* New more structured event logging. See [user manual](https://www.kalasim.org/analysis/#event-log)
* Implemented support for [real-time simulations](https://www.kalasim.org/advanced/#clock-synchronization)
* New example [Dining Philosophers](https://www.kalasim.org/examples/dining_philosophers/)
* New example [Movie Theater](https://www.kalasim.org/examples/movie_theater/)
* New API to add dependencies in simulation context using `dependency {}`

Notable Fixes

* Fixed `failAt` in `request`


## v0.4

Released 2021-01-03

Major Enhancements

* Implemented [`interrupt`](https://www.kalasim.org/component/#interrupt) interaction
* Reworked documentation and examples
* Implemented [`standby`](https://www.kalasim.org/component/#standby)
* Implement disable/enable for [monitors](https://www.kalasim.org/monitors/)
* Yield internally, to simplify process definitions
```kotlin
// before
object : Component() {
    override fun process() = sequence { yield(hold(1.0)) }
}

// now
object : Component() {
    override fun process() = sequence { hold(1.0) }
}
```

* Made `scheduledTime` nullable: Replaced `scheduledTime = Double.MAX_VALUE` with `null` where possible to provide better mental execution model
* Provide lambda parameter to enable auto-releasing of resources
```kotlin
// before
object : Component() {
    override fun process() = sequence { 
        request(r)
        hold(1)
        release(r)
    }
}

// now
object : Component() {
    override fun process() = sequence { 
        request(r){
            hold(1)
        }
    }
}

```
* Implemented `Environment.toString` to provide json description
* Various bug-fixes


## v0.3

* Reimplemented monitors
* Continued salabim core API reimplementation
* Fixed: Decouple simulation with different koin application contxts

## v0.2

* Reimplement core salabim examples in kotlin
* Port all salabim examples
* Started MkDocs manual

## v0.1

* Reimplement salabim's main component lifecycle
* Add timing API

# Kalasim Release History

## v2023.1

!! not yet released!

Major & Breaking API Changes

* Most importantly we have migrated the API to use `org.kalasim.SimTime` to track simulation. `SimTime` is a simple typealias for `kotlinx.datetime.Instant`, effectively giving users the full flexibility of using a well designed and established date-time concept. `org.kalasim.TickTime` is still available for backward compatibility reasons, but is opt-in or required to subclass `TickedComponent`.
* Simplified the configurability for [tracking](advanced.md#continuous-simulation) of entity timelines and statistics. It's now more direct via constructor parameters in addition to environment defaults
* [#65](https://github.com/holgerbrandl/kalasim/issues/65) Improved arithmetics of [metric timelines](monitors.md#value-monitors)

Minor improvements

* [#51](https://github.com/holgerbrandl/kscript/issues/51) Added `description` for better readiability when supepending exeuction for simulatoin states using [`wait()`](component.md#wait) 
* [#56](https://github.com/holgerbrandl/kalasim/issues/56) Improved support for [duration distributions](basics.md#duration-distributions) 
* Expose `Environment.getOrNull<T>()` from [koin](https://github.com/InsertKoinIO/koin/issues/182) to check for presence of registered dependencies in simulation environment
* [#46](https://github.com/holgerbrandl/kalasim/issues/46) clarify use of collect with filter 
* [#52](https://github.com/holgerbrandl/kalasim/issues/54) Improved visualization of metric timelines to support zoom range
* [#67](https://github.com/holgerbrandl/kalasim/issues/67) & [#64](https://github.com/holgerbrandl/kalasim/issues/64) Added more safety guard mechanisms to prevent context violations when branching component processes.

Starting with this release we have switched to calendar versioning for better clarity regarding our release density, timing and schedule.


## v0.11

Major improvements 

* significantly improved library performance
* Added `Int.Weeks` extension
* Introduced suspendable `join(components: List<Component>)` to wait for other components to become `DATA`  

Documentation & Examples
* New Example Shipyard - Multipart assembly


## v0.10

Released 2023-06-16

Breaking API Changes

* tick metrics and component-logger are now [configured](basics.md#configuring-a-simulation) and not enabled via constructor parameter any longer (to minimize constructor complexity)

Improvements

* More robust [dependency injection](basics.md#dependency-injection)

Performance

* Added jmh benchmark-suite and reporting

Documentation

* Continued migration to `Duration` as replacement for `Number` in `hold()`, `wait()` etc.  

## v0.9 

Released at 2023-04-13

Major

* [#49](https://github.com/holgerbrandl/kalasim/issues/49) Changed API to always favor `kotlin.time.Duration` to express durations. Previously untyped `Numbers` were used that often led to confusion in larger simulations models. Evey simulation environment has now a `DurationUnit` such as seconds, hours, etc.  (defaulting to minutes if not specified). 
* New [opt-in](https://kotlinlang.org/docs/opt-in-requirements.html) annotations were introduced to prevent use of untyped duration arguments in interaction functions such as ``
* Migrated use of `Instant` to `kotlinx.datetime.Instant` for better API consistency
* New sampling functions to sample durations directly: `val uni = uniform(5.minutes, 2.hours); uni() // results in Duration`

Minor 

* Overwrite `shuffled()` and `random()` as extensions on `Collection<T>` in simulation entities to enable better control over randomization by default

## v0.8

Released [announced](articles/2022-09-27-kalasim-v08.md) at 2022-09-27

Milestone Enhancements

* Implemented [honor policies](resource.md#request-honor-policies) allowing for more configurable request queue consumption
```kotlin
val r  = Resource(honorPolicy = RequestHonorPolicy.StrictFCFS)
```
* Added [Timeline Arithmetics](monitors.md#monitors-arithmetics). It is now possible to perform stream arithmetics on timeline attributes
* Introduced different [capacity modes](resource.md#capacity-limit-modes) if resource requests exceed resource capacity.
```kotlin
val tank  = DepletableResource(capacity=100, initialLevel=60)

put(gasSupply, 50, capacityLimitMode = CapacityLimitMode.CAP)
```
* [#23](https://github.com/holgerbrandl/kalasim/issues/23) Added support for duration extensions introduced in kotlin [v1.6](https://blog.jetbrains.com/kotlin/2021/11/kotlin-1-6-0-is-released/) to express durations more naturally with `2.hours`, `3.minutes` and so on. It is now possible to use `java.time.Instant` and `kotlin.time.Duration` in `Component.hold()` and `Environment.run`. 
```kotlin
createSimulation{
    object: Component{
        val driver = Resource(2) 
        override fun process() = sequence {
            request(driver) {
                hold(23.minutes)
            }
            hold(3.hours)
        }
    
    }
}.run(2.5.days) // incl. fractional support
```

Major Enhancements

* [#37](https://github.com/holgerbrandl/kalasim/issues/37) Simplified [process activation](component.md#activate) in process definitions  
* [#34](https://github.com/holgerbrandl/kalasim/issues/34) Added support for [triangular distributions](basics.md#continuous-distributions)
* [#43](https://github.com/holgerbrandl/kalasim/issues/43) Simplified states to consume [predicates](state.md#predicate-testing) directly in `wait()`
* [#27](https://github.com/holgerbrandl/kalasim/issues/27) Made resource events more informative and consistent. These event now include a request-id to enable simplified bottleneck analyses  
* Added `RequestScopeContext` to honor-block of `request` including `requestingSince` time
* [#35](https://github.com/holgerbrandl/kalasim/pull/35) Improved support for asynchronous event consumption (contributed by [pambrose](https://github.com/pambrose) via PR)
* Reduced memory requirements of [resource](resource.md) monitoring by 50% by inferring `occupancy` and `availability` using [Timeline Arithmetics](monitors.md#monitors-arithmetics)
* [#38](https://github.com/holgerbrandl/kalasim/issues/38) Extended and improved API support for [depletable resources](resource.md#depletable-resources). 
* Added `ComponentQueue.asSortedList()` to sorted copy of underlying priority queue 
* Ported data-frame-support from [krangl](https://github.com/holgerbrandl/krangl) to the more modern [kotlin-dataframe](https://github.com/Kotlin/dataframe).

Minor enhancements

* [#47](https://github.com/holgerbrandl/kalasim/issues/47) Added entity [auto-indexing](component.md) to allow for more descriptive names
* [#50](https://github.com/holgerbrandl/kalasim/pull/50) Fixed [`trigger()`](state.md#state-change-triggers)
* Introduced more event-types and improved [structured logging](events.md) capabilities
* Renamed all `info` attributes to `snapshot` to convey intent better
* Unified naming [resource](resource.md) attributes
* [#28](https://github.com/holgerbrandl/kalasim/pull/28) Added support API to [sample UUIDs](basics.md#enumerations) with engine-controlled randomization 
* Added `capacity` to [component collections](collections.md)
* Reworked [distribution support API](basics.md#randomness--distributions) for better API experience to enable controlled randomization in process models
* Removed `Resource.release()` because of incomplete and unclear semantics
* [#53](https://github.com/holgerbrandl/kalasim/issues/53) Generified [`MetricsTimeline`](analysis.md#monitors)

Documentation

* [#38](https://github.com/holgerbrandl/kalasim/issues/38) Rewritten gas-station example to illustrate [depletable resource](resource.md#depletable-resources) usage
* Added new datalore example workbook: [Extended Traffic](https://datalore.jetbrains.com/view/notebook/k1y5ufCMLdAWZOu1ztU2fV)
* Reworked [The Office Tower](examples/office_tower.md) to include better model description and animation
* New: [Lunar Mining](animation/lunar_mining.md) model to illustrate new animation toolbox of kalasim  


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
* Established use of `SimTime` across the entire API to disambiguate simulation time instants from durations, which are still modelled as `Double`
* Changed `Component.now `and `Environment.now` to new value class `SimTime` for better type safety
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
val selected = selectResource( doctors, policy = ShortestQueue )
```

* New suspending [`batch`](https://www.kalasim.org/component/#batching) interaction to group an entity stream into blocks
```kotlin
val queue = ComponentQueue<Customer>()
val batchLR: List<Customer> = batch(queue, 4, timeout = 10)
```

* Added option to [configure](advanced.md#tick-transformation) a tick to wall time transformer 
```kotlin
createSimulation {
    tickTransform = OffsetTransform(Instant.now(), DurationUnit.MINUTES)

    run(Duration.ofMinutes(90).asTicks())
    println(asSimTime(now))
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
* New more structured event logging. See [user manual](https://www.kalasim.org/analysis/#events)
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

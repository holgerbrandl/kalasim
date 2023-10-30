## Clock Synchronization

In simulation a clear distinction is made between real time and simulation time. With *real time* we refer to the wall-clock time. It represents the execution time of the experiment. The *simulation time* is an attribute of the simulator.

To support use cases where a simulation may drive a demonstration or system check, the `kalasim` API allows to run a simulation at a defined clock speed. Such real-time simulations may be necessary

* If you have hardware-in-the-loop
* If the intent of the simulation is to drive a visualization of a process
* If there is human interaction with your simulation, or
* If you want to analyze the real-time behavior of an algorithm

```kotlin hl_lines="9"
{!api/RealtimeSimulation.kts!}
```
This example will execute in 10 seconds. Since the simulation is empty (for educational reasons to keep the focus on the clock here), it is entirely idle during that time.

To enable clock synchronization, we need to add a `ClockSync` to our simulation. We need to define what one _tick_ in simulation time corresponds to in wall time. In the example, one tick equals to one second wall time. This is configured with the parameter `tickDuration`. It defines the duration of a simulation tick in wall clock coordinates. It can be created with `Duration.ofSeconds(1)`, `Duration.ofMinutes(10)` and [so on](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/Duration.html).

`ClockSync` also provides settings for more advanced uses-cases

* To run simulations, in more than realtime, the user can specify `speedUp` to run a simulation faster (`speedUp` > 1) or slower (`speedUp` < 1) than realtime. It defaults to `1`, that is no speed-up will be applied.
* The argument `syncsPerTick` defines how often a clock synchronization should happen. Per default it synchronizes once per _tick_ (i.e. an 1-increment of simulation time).


It may happen that a simulation is too complex to run at a defined clock. In such a situation, it (i.e. `Environment.run()`) will throw a `ClockOverloadException` if the user has specified a maximum delay `maxDelay` parameter between simulation and wall clock coordinates.


<!-- Inspired by <https://simpy.readthedocs.io/en/latest/topical_guides/real-time-simulations.html>-->

## Operational Control

Even if `kalasim` tries to provide a simplistic, efficient, declarative approach to define a simulation, it may come along with computational demands simulation. To allow introspection into time-complexity of the underlying computations, the user may want to enable the built-in `env.tickMetrics` [monitor](monitors.md) to analyze how much time is spent per time unit (aka *tick*). This monitor can be enabled by calling `enableTickMetrics()` when [configuring](basics.md#configuring-a-simulation) the simulation.

```kotlin hl_lines="5"
{!api/TickMetricsExample.kts!}
```

[comment]: <> (<!-- Also see https://cran.r-project.org/web/packages/simmer/vignettes/simmer-01-introduction.html#replication --> )

## Performance tuning

There are multiple ways to improve the performance of a simulation. 

1. Disable internal event logging: The [interaction model](component.md) is configured by default to provide insights into the simulation via the [event log](events.md). However, to optimize performance of a simulation a user may want to consume only custom event-types. If so, internal interaction logging can be adjusted by setting a [logging policy](#continuous-simulation).  
2. Disable component statistics: Components and queues log various component statistics with built-in [monitors](monitors.md) which can be adjusted by setting a [logging policy](#continuous-simulation) to reduce compute and memory footprint of a simulation.  
3. Set the correct `AssertMode`: The assertion mode determines which internal consistency checks are being performed.  The mode can be set to `Full` (Slowest), `Light` (default) or `Off` (Fastest). Depending on simulation logic and complexity, this will improve performance by ~20%.


To further fine-tune and optimize simulation performance and to reveal bottlenecks, a JVM profiler (such as [yourkit](https://www.yourkit.com/) or the built-in profiler of [Intellij IDEA Ultimate](https://www.jetbrains.com/idea/)) can be used. Both call-counts and spent-time analysis have been proven useful here. 

## Continuous Simulation

For some use-cases, simulations may run for a very long simulation and wall time. To prevent internal metrics gathering from consuming all available memory, it needs to be disabled or at least configured carefully. This can be achieved, but either disabling [timelines and monitors](monitors.md) manually on a per-entity basis, or by setting a sensible default policy via `Environment.entityTrackingDefaults`

For each entity type a corresponding tracking-policy `TrackingConfig` can be provisioned along with an entity matcher to narrow down its scope. A _tracking-policy_ allows to change 

1. How events are logged 
2. How internal metrics are gathered

There are different default implementations, but the user can also implement and register custom tracking-configurations.

* ComponentTrackingConfig
* ResourceTrackingConfig
* StateTrackingConfig
* ComponentCollectionTrackingConfig

```kotlin hl_lines="7 8 11 30"
//{!api/InternalMetricsConfig.kts!}
```

!!!note
    Tracking configuration policies defaults must be set before instantiating simulation entities to be used

To disable all metrics and to minimize internal event logging, the user can run `env.entityTrackingDefaults.disableAll()`

The same mechanism applies also fine-tune the internal [event logging](events.md). By disabling some -  not-needed for production - events, simulation performance can be improved significantly.

The user can also register her own `TrackConfig` implementations using the factory. See [here](https://github.com/holgerbrandl/kalasim/blob/4f284e6f52ab9ab2f09b6bf5331f4fd413476702/src/test/kotlin/org/kalasim/test/ComponentTests.kt#L134-L134) for simple example. 



## Save and Load Simulations

<!-- TODO learn from https://github.com/r-simmer/simmer.json -->

`kalasim` does not include a default mechanism to serialize and deserialize simulations yet. However, it [seems](https://github.com/holgerbrandl/kalasim/blob/master/src/test/kotlin/org/kalasim/experimental/SaveLoadSimulation.kt) that with [xstream](https://x-stream.github.io/) that `Environment` can be saved including its current simulation state across all included entities. It can be restored from the xml snapshot and continued with `run()`.

 We have not [succeeded](https://github.com/holgerbrandl/kalasim/blob/master/src/test/kotlin/org/kalasim/misc/SaveLoadSimulation.kt#L39) to do the same with [gson](https://github.com/google/gson) yet. Also, some experiments with [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) were not that successful.
 

## Internal State Validation

The simulation engine provides different levels of internal consistency checks. As these are partially computationally expensive these can be be/disabled. There are 3 modes

* `OFF` - Productive mode, where asserts that may impact performance are disabled.
* `LIGHT` - Disables compute-intensive asserts. This will have a minimal to moderate performance impact on simulations.
* `FULL` - Full introspection, this will have a measurable performance impact on simulations. E.g. it will validate that passive components are not scheduled, and queued components have unique names.

Switching off asserts, will typically optimize performance by another ~20% (depending on simulation logic).
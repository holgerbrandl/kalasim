# Analysis

A core aspect when building simulations is to understand, define and modulate the inherent system dynamics. To build a correct simulation, the designer/developer must carefully analyze how states progress over time.

To facilitate this process, `kalasim` offers various means to analyze data created by a simulation

* [Event Log](#event-log) - to track state changes
* [Monitors](monitors.md) - to track state and statistics of the [basic](basics.md) elements within a simulation


## Event Log

To analyze a simulation, you may want to monitor entity creation and process progression. You may also want to trace which process caused an event or which processes waited for an event. `kalasim` is collecting these data across all basic [simulation entities](basics.md) and in particular in the support process [interaction model](component.md#process-interaction).


With console logging being enabled, we get the following output (displayed as table for convenience):

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

This output is not active by default, but must be enabled when creating the simulation with `createSimulation(enableConsoleLogger = true)`

For larger simulations, console logging might not scale. Also, users may want to consume state changes for analysis and visualization. To support these use-cases, default logging can be disabled and custom log handlers can be added.

```kotlin
// create simulation without default console logging
val sim = createSimulation(enableConsoleLogger = false) {  }

// add custom log consumer
sim.addEventListener(EventListener { event -> TODO("do something with")  })
```

By supporting a pub-sub pattern, users can easily attach different monitoring backends such as files, databases, or in-place-analytics.

<!-- TODO detail out monitoring backends https://github.com/r-simmer/simmer.mon-->

Trace logs a suitable for standard kotlin collection processing. E.g. we can setup a [coroutines channel](https://kotlinlang.org/docs/reference/coroutines/channels.html) for log events that is is consumed asynchronously ([example](examples/misc.md#coroutine-channels)).

Events can also be acculated by using `traceCollector()`

For example to fetch all events related to resource requests we could filter by the corresponding event type

```kotlin
//{!api/EventCollector.kts!}
```

## Monitors

See chapter about [monitors](monitors.md).

## Visualization

There are two type of visualization

* Statistical plots. See the [Movie Theater](examples/movie_theater.md)


## Tabular Interface

Often a sound type is usually the preferred solution for modelling. Still, accessing data in a tabluar way can also be helpful to enable more statistical analysis. `kalasim` supports transformation. This also allows to provide a semantic compatibility layer with other DES engines (such as [simmer](about.md#simmer)), that are centered around tables for model analysis.

Most metric types in `kalasim` provide a transformation to  a `*Record` type. For instance, for component states we can extract a lifecycle history summary `ComponentLifecycleRecord` with

```kotlin
val customers : List<Component> // = ...
val records: List<ComponentLifecycleRecord> = customers.map { it.toLifeCycleRecord() }

records.asDataFrame()
```

This transform the customers straight into a `krangl` dataframe. Its structure is

```
A DataFrame: 1034 x 11
      component   createdAt   inCurrent    inData   inDataSince   inInterrupted   inPassive
 1    Vehicle.1       0.366           0   989.724        10.276               0           0
 2    Vehicle.2       1.294           0   984.423        15.577               0           0
 3    Vehicle.3       1.626           0   989.724        10.276               0           0
 4    Vehicle.4       2.794           0   989.724        10.276               0           0
and 1024 more rows, and and 4 more variables: inScheduled, inStandby, inWaiting
```

Clearly if needed, the user may also work with the records directly to drive a visualization for instance.

A similar approach can be applied to simulation `Event`s. For example, we can apply an instance filter to the recorded log to extract only log records relating to resource requests. These can be transformed and converted to a csv with just:

```kotlin
// ... add your simulation here ...
data class RequestRecord(val requester: String, val timestamp: Double, 
            val resource: String, val quantity: Double)

val tc = sim.get<TraceCollector>()
val requests = tc.filterIsInstance<ResourceEvent>().map {
    val amountDirected = (if(it.type == ResourceEventType.RELEASED) -1 else 1) * it.amount
    RequestRecord(it.requester.name, it.time, it.resource.name, amountDirected)
}

// transform data into data-frame (for visualization and stats)  
requests.asDataFrame().writeCSV("requests.csv")
```

The transformation step is optional, `List<Event>` can be transformed `asDataFrame()` directly.

## Replication

Running a simulation just once, often does not provide sufficient insights into the dynamics of the system under consideration. Often, the user may want to execute a model many times with altered initial conditions, and then perform a statistical analysis over the output. This is also considered as *what-if* analyis. See [here](examples/atm_queue.md#simple-what-if) for simple example.

By design `kalasim` does not make use of parallelism. So when scaling up execution to run in paralell, we need to be careful, that the internal [dependency injection](basics.md#dependency-injection) (which relates by default to a global context variable) does not cause trouble. See [here](examples/atm_queue.md#parallel-what-if) for an example that defines a parameter grid to be assessed with multi-threading with a simulation run per hyper-parameter.

<!--See also 4.2 in Ucar2019-->





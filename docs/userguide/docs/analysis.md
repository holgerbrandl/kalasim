# Analysis

A core aspect when building simulations is to understand, define and modulate the inherent system dynamics. To build a correct simulation, the designer/developer must carefully analyze how states progress over time.

To facilitate this process, `kalasim` offers various means to analyze data created by a simulation

* The [Event Log](event_log.md) tracks events in a simulation
* [Monitors](monitors.md) track state and statistics of the [basic](basics.md) elements within a simulation, and may be used for domain-specific entities as well
* [Lifecycle Records](#event-log) summarize a component's states history
* [visualization](visualization.md) to inspect complex spatio-temporal patterns   


## Monitors

See chapter about [monitors](monitors.md).

## Event Log

See chapter about [event logging](event_log.md).

## Visualization

See chapter about [visualization](visualization.md).

## Component Status

The state transition of a component provide value insight into its behavior. This is facilitated by lifecycle statistics `ComponentLifecycleRecord` that summarize a component's states history. 

These data can also be transformed easily into a table as well
```kotlin
val customers : List<Component> // = ...
val records: List<ComponentLifecycleRecord> = customers.map { it.toLifeCycleRecord() }

records.asDataFrame()
```

This transforms the `customers` straight into a [`krangl`](https://github.com/holgerbrandl/krangl) dataframe with the following structure

```
A DataFrame: 1034 x 11
      component   createdAt   inCurrent    inData   inDataSince   inInterrupted   inPassive
 1    Vehicle.1       0.366           0   989.724        10.276               0           0
 2    Vehicle.2       1.294           0   984.423        15.577               0           0
 3    Vehicle.3       1.626           0   989.724        10.276               0           0
 4    Vehicle.4       2.794           0   989.724        10.276               0           0
and 1024 more rows, and and 4 more variables: inScheduled, inStandby, inWaiting
```

Clearly if needed, the user may also work with the records directly. For instance to configure a visualization.



## Replication

Running a simulation just once, often does not provide sufficient insights into the dynamics of the system under consideration. Often, the user may want to execute a model many times with altered initial conditions, and then perform a statistical analysis over the output. This is also considered as *what-if* analyis. See [here](examples/atm_queue.md#simple-what-if) for simple example.

By design `kalasim` does not make use of parallelism. So when scaling up execution to run in paralell, we need to be careful, that the internal [dependency injection](basics.md#dependency-injection) (which relates by default to a global context variable) does not cause trouble. See [here](examples/atm_queue.md#parallel-what-if) for an example that defines a parameter grid to be assessed with multi-threading with a simulation run per hyper-parameter.

<!--See also 4.2 in Ucar2019-->


## Component Tracking

To prevent memory leaks, the [environment](basics.md#simulation-environment) just keeps track of scheduled components, that is components that are queued for execution. In some situations the user may want to track all components irrespective of their queuing status. This can be achieved by setting up a component collector **before** creating the components

```kotlin
createSimulation{
    val cc = componentCollector()

    // create components
    Component("foo")
    Component("bar")
    
    // analyze all components created until this point
    cc.size // will be 2
}
```


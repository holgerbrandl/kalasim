# Analysis

A core aspect when building simulations is to understand, define and modulate the inherent system dynamics. To build a correct simulation, the designer/developer must carefully analyze how states progress over time.

To facilitate this process, it offers various means to analyze data created by a simulation

* [Event Log](#event-log) - to track state changes
* [Monitors](monitors.md) - to track state and statistics of the [basic](basics.md) elements within a simulation


## Event Log

While a simulation is running, we get the following output (displayed as table for convenience):

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

This output is not active by default, but must be enabled when creating the simulation with `createSimulation(enableTraceLogger = true)`

For larger simulations, console logging might not scale. Also, users may want to consume state changes for analysis and visualization. To support these use-cases, default logging can be disabled and custom log handlers can be added.

```kotlin
// create simulation without default console logging
val sim = createSimulation(enableTraceLogger = false) {  }

// add custom log consumer
sim.addTraceListener(TraceListener { traceElement -> TODO("do something with")  })
```


## Monitors

See chapter about monitors

## Visualization




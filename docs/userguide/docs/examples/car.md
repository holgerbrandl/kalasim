<!--## Cars-->

A single car, a driver, and red traffic light in the middle of the night. 

![Stanhope Forbes A Ferryman at Flushing](trafficlight.jpg){: .center}

<p align="center">
<i><a href="https://pxhere.com/en/photo/385193">Red Light</a>, Matthias Ripp (CC BY 2.0)</i>
</p>

Let’s start with a very simple model. The example demonstrates the main mode of operation, the core API and the component process model implemented in `kalasim`. We want to build a simulation where a single car is driving around for a some time before arriving at its destination.

```kotlin
//{!api/Cars.kts!}
```

<!--This example corresponds to the `Cars` `salabim` example https://www.salabim.org/manual/Modeling.html-->

For each (active) component we (can) define a type such as:

```kotlin
class Car : Component()
```

The class inherits from `org.kalasim.Component`.

Our car depends on a [state](../state.md) `TrafficLight` and [resource](../resource.md) `Driver` for operation. To implement that, we first declare these dependencies with `dependency{}` in the main body of the simulation, and secondly [inject](../basics.md#dependency-injection) them into our car with `get<T>`. Note, we could also directly inject states and resources with `dependency {State("red")}` without sub-classing.

Although it is possible to define other processes within a class,
the standard way is to define a generator function called `process` in the class.
A generator is a function that returns `Sequence<Component>`. Within these process definitions we use [`suspend`](https://kotlinlang.org/docs/reference/coroutines/basics.html#your-first-coroutine)able interaction function calls as a [signal](../basics.md#dependency-injection) to give control to the centralized [event loop](../basics.md#event-queue).

In this example,

```kotlin
hold(1.0)
```

suspends execution control and *comes back* after 1 time unit (referred to as _tick_). Apart from [`hold`](../component.md#hold), `kalasim` supports a rich vocabulary of interaction methods including [`passivate`](../component.md#passivate), [`request`](../component.md#request), [`wait`](../component.md#wait) and [`component`](../component.md#standby).


The main body of every `kalasim` model usually starts with:
```
createSimulation(enableConsoleLogger = true){
...
}
```
Here, we enable event logging of state changes to see the status of simulation on the console. After declaring our dependencies, we instantiate a single car with `Car()`. It automatically is assigned the name *Car.0*.

As there is a generator function called `process` in `Car`, this process description will be activated (by default at time `now`, which is `0` by default at the beginning of a simulation). It is possible to start a process later, but this is by far the most common way to start a process.

With

```kotlin
run(5.0)
```

we start the simulation and get back control after 5 ticks. A component called *main* is defined under the hood to get access to the main process.

When we run this program, we get the following output (displayed as table for convenience):

```
time   current  receiver  action                             info               
------ -------- --------- ---------------------------------- -------------------
.00             main      Created
.00    main
.00             Driver.1  Created                             capacity=1
.00             Car.1     Created
.00                       activate                           scheduled for .00
.00             main      run +5.00                          scheduled for 5.00
.00    Car.1    Car.1
.00                       Requesting 1.0 from Driver.1 
.00                       Claimed 1.0 from 'Car.1'
.00                       Request honor Driver.1             scheduled for .00
.00
.00                       hold +1.00                         scheduled for 1.00
1.00
1.00                      entering waiters of TrafficLight.1
1.00                      wait                               scheduled for <inf>
5.00   main     main
Process finished with exit code 0
```

There are plenty of other more advanced (that is more **fun**!) examples listed in [examples](../examples.md) chapter.

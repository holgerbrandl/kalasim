<!--## Customer Queue: A bank example-->

Queue problems are common-place application of [discrete event simulation](../theory.md#what-is-discrete-event-simulation).

Often there are multiple solutions for a model. Here we model similar problems - a customer queue -  differently using resources, states and queues in various configurations and interaction patterns.

## Simple Bank Office (1 clerk)

Lets start with a bank office where customers are arriving in a bank, where there is **one** clerk. This clerk handles the customers in a *first in first out* (FIFO) order.

We see the following processes:

* The customer generator that creates the customers, with an inter-arrival time of `uniform(5,15)`
* The customers
* The clerk, which serves the customers in a constant time of 30 (overloaded and non steady state system)

We need a queue for the customers to wait for service.

The model code is:

```kotlin hl_lines="15 17 31 33 34 47 65"
//{!bank/oneclerk/Bank1clerk.kt!}
```

Let's look at some details (marked in yellow for convenience).

With:

```kotlin
waitingLine.add(this@Customer)
```

the customer places itself at the tail of the waiting line.

Then, the customer checks whether the clerk is idle, and if so, activates him immediately.:

```kotlin
if (clerk.isPassive) clerk.activate()
```

Once the clerk is active (again), it gets the first customer out of the waitingline with:

```kotlin
val customer = waitingLine.poll()
```

and holds for 30 time units with:

```kotlin
hold(10.0)
```

After that hold the customer is activated and will terminate:

```kotlin
customer.activate()
```

In the main section of the program, we create the `CustomerGenerator`, the `Clerk` and a [`ComponentQueue`](../collections.md#queue) called waitingline. Here the customer generator is implemented as a custom instance of `Component` for educational puroposes. Using the provided [`ComponentGenerator`](../component.md#component-generator) API would be more concise.

```kotlin
hold(uniform(5.0, 15.0).sample())
```

will do the statistical sampling and wait for that time till the next customer is created.

Since logging is enabled when creating the simulation with `createSimulation` the following log trace is being produced

```
time      current component        action                                       info                               
--------- ------------------------ -------------------------------------------- ----------------------------------
.00                                main create
.00       main
.00                                Clerk.1 create
.00                                Clerk.1 activate                             scheduled for .00
.00                                CustomerGenerator.1 create
.00                                CustomerGenerator.1 activate                 scheduled for .00
.00                                main run +50.00                              scheduled for 50.00
.00       Clerk.1
.00                                Clerk.1 passivate
.00       CustomerGenerator.1
.00                                Customer.1 create
.00                                Customer.1 activate                          scheduled for .00
.00                                CustomerGenerator.1 hold +11.95              scheduled for 11.95
.00       Customer.1
.00                                Customer.1 entering waiting line
.00                                Clerk.1 activate                             scheduled for .00
.00                                Customer.1 passivate
.00       Clerk.1
.00                                Customer.1 leaving waiting line
.00                                Clerk.1 hold +10.00                          scheduled for 10.00
10.00                              Clerk.1
10.00                              Customer.1 activate                          scheduled for 10.00
10.00                              Clerk.1 passivate
10.00     Customer.1
10.00                              Customer.1 ended
11.95     CustomerGenerator.1
11.95                              Customer.2 create
11.95                              Customer.2 activate                          scheduled for 11.95
11.95                              CustomerGenerator.1 hold +7.73               scheduled for 19.68
11.95     Customer.2
11.95                              Customer.2 entering waiting line
11.95                              Clerk.1 activate                             scheduled for 11.95
11.95                              Customer.2 passivate
11.95     Clerk.1
11.95                              Customer.2 leaving waiting line
11.95                              Clerk.1 hold +10.00                          scheduled for 21.95
19.68     CustomerGenerator.1
19.68                              Customer.3 create
19.68                              Customer.3 activate                          scheduled for 19.68
19.68                              CustomerGenerator.1 hold +10.32              scheduled for 30.00
19.68     Customer.3
19.68                              Customer.3 entering waiting line
19.68                              Customer.3 passivate
21.95     Clerk.1
21.95                              Customer.2 activate                          scheduled for 21.95
21.95                              Customer.3 leaving waiting line
21.95                              Clerk.1 hold +10.00                          scheduled for 31.95
21.95     Customer.2
21.95                              Customer.2 ended
30.00     CustomerGenerator.1
30.00                              Customer.4 create
30.00                              Customer.4 activate                          scheduled for 30.00
30.00                              CustomerGenerator.1 hold +10.63              scheduled for 40.63
30.00     Customer.4
30.00                              Customer.4 entering waiting line
30.00                              Customer.4 passivate
31.95     Clerk.1
31.95                              Customer.3 activate                          scheduled for 31.95
31.95                              Customer.4 leaving waiting line
31.95                              Clerk.1 hold +10.00                          scheduled for 41.95
31.95     Customer.3
31.95                              Customer.3 ended
40.63     CustomerGenerator.1
40.63                              Customer.5 create
40.63                              Customer.5 activate                          scheduled for 40.63
40.63                              CustomerGenerator.1 hold +5.31               scheduled for 45.95
40.63     Customer.5
40.63                              Customer.5 entering waiting line
40.63                              Customer.5 passivate
41.95     Clerk.1
41.95                              Customer.4 activate                          scheduled for 41.95
41.95                              Customer.5 leaving waiting line
41.95                              Clerk.1 hold +10.00                          scheduled for 51.95
41.95     Customer.4
41.95                              Customer.4 ended
45.95     CustomerGenerator.1
45.95                              Customer.6 create
45.95                              Customer.6 activate                          scheduled for 45.95
45.95                              CustomerGenerator.1 hold +12.68              scheduled for 58.63
45.95     Customer.6
45.95                              Customer.6 entering waiting line
45.95                              Customer.6 passivate
50.00     main
```

After the simulation is finished, the statistics of the queue are presented with:

```kotlin
waitingLine.stats.print()
```

The statistics output looks like

```json
{
  "queue_length": {
    "all": {
      "duration": 50,
      "min": 0,
      "max": 1,
      "mean": 0.15,
      "standard_deviation": 0.361
    },
    "excl_zeros": {
      "duration": 7.500540828621098,
      "min": 1,
      "max": 1,
      "mean": 1,
      "standard_deviation": 0
    }
  },
  "name": "waiting line",
  "length_of_stay": {
    "all": {
      "entries": 5,
      "ninety_pct_quantile": 3.736,
      "median": 1.684,
      "mean": 1.334,
      "ninetyfive_pct_quantile": 3.736,
      "standard_deviation": 1.684
    },
    "excl_zeros": {
      "entries": 3,
      "ninety_pct_quantile": 3.736,
      "median": 1.645,
      "mean": 2.223,
      "ninetyfive_pct_quantile": 3.736,
      "standard_deviation": 1.645
    }
  },
  "type": "QueueStatistics",
  "timestamp": 50
}
```


## Bank Office with 3 Clerks

Now, let's add more clerks:

```kotlin
add { (1..3).map { Clerk() } }
```


And, every time a customer enters the waiting line, we need to make sure at least one passive clerk (if any) is activated:

```kotlin
for (c in clerks) {
    if (c.isPassive) {
        c.activate()
        break // activate at max one clerk
    }
}
```


The complete source of a three clerk post office:

```kotlin hl_lines="63"
//{!bank/threeclerks/Bank3Clerks.kt!}
```

## Bank Office with Resources

`kalasim` contains another useful concept for modelling: [Resources](../resource.md). Resources have a limited capacity and can be claimed by components and released later.

In the model of the bank with the same functionality as the above example, the clerks are defined as a resource with capacity 3.

The model code is:

```kotlin hl_lines="12 21"
//{!bank/resources/Bank3ClerksResources.kt!}
```

Let's look at some details.:

```kotlin
add { Resource("clerks", capacity = 3) }
```

This defines a resource with a capacity of `3`.

Each customer tries to claim one unit (=clerk) from the resource with:

```kotlin
request(clerks)
```

B default 1 unit will be requested. If the resource is not available, the customer needs to wait for it to become available (in order of arrival).

In contrast with the previous example, the customer now holds itself for 30 time units (clicks). After this time, the customer releases the resource with:

```kotlin
release(clerks)
```

The effect is that `kalasim` then tries to honor the next pending request, if any.

In this case the release statement is not required, as resources that were claimed are automatically released when a process terminates).`

The statistics are maintained in two system queues, called `clerk.requesters` and `clerk.claimers`.

The output is very similar to the earlier example. The statistics are exactly the same.

##  Bank Office with Balking and Reneging

Now, we assume that clients are not going to the queue when there are more than 5 clients waiting (balking). On top of that, if a client is waiting longer than 50, he/she will leave as well (reneging).

The model code is:

```kotlin hl_lines="32 46-56 72 73"
//{!bank/reneging/Bank3ClerksReneging.kt!}
```

Let's look at some details.

```kotlin
cancel()
```

This makes the current component (a customer) a `DATA` component (and be subject to garbage collection), if the queue length is `5` or more.

The *reneging* is implemented after a hold of `50`. If a clerk can service a customer, it will take the customer out of the waitingline and will activate it at that moment. The customer just has to check whether he/she is still in the waiting line. If so, he/she has not been serviced in time and thus will renege.

```kotlin
hold(50.0)

if (waitingLine.contains(this@Customer)) {
    waitingLine.leave(this@Customer)

    numReneged++
    printTrace("reneged")
} else {
    passivate()
}
```


All the clerk has to do when starting servicing a client is to get the next customer in line out of the queue (as before) and activate this customer (at time now). The effect is that the hold of the customer will end.

```kotlin
hold(30.0) 
customer.activate() // signal the customer that's all's done
```


##  Bank Office with Balking and Reneging (resources)

Now we show how  balking and reneging can be implemented with resources.

The model code is:

```kotlin hl_lines="23 25 26"
//{!bank/reneging_resources/Bank3ClerksRenegingResources.kt!}
```

As you can see, the balking part is exactly the same as in the example without resources.

For the renenging, all we have to do is add a `failDelay`:

```kotlin
request(clerks, failDelay = 50.asDist())
```

If the request is not honored within `50` time units (ticks), the process continues after that `request` statement. We check whether the request has failed with the built-in `Component` property:

```kotlin
iff (failed)
    numReneged++
```

This example shows clearly the advantage of the resource solution over the `passivate`/`activate` method, in [former](#bank-office-with-3-clerks) example.

##  Bank Office with States

Another useful concept for modelling are [states](../state.md). In this case, we define a state called `worktodo`.

The model code is:

```kotlin hl_lines="22 35 50"
//{!bank/state/Bank3ClerksState.kt!}
```

Let's look at some details.

```kotlin
add { State(false, "worktodo") }
```

This defines a state with an initial value `false` and registers it as a dependency.

In the code of the customer, the customer tries to trigger one clerk with:

```kotlin
workTodo.trigger(true, max = 1)
```

The effect is that if there are clerks waiting for worktodo, the first clerk's wait is honored and that clerk continues its process after:

```kotlin
wait(workTodo, true)
```

Note that the clerk is only going to wait for worktodo after completion of a job if there are no customers waiting.


## Bank Office with Standby

The `kalasim` package contains yet another powerful process mechanism, called [standby](../component.md#standby). When a component is in [`STANDBY`](../component.md#lifecycle) mode, it will become current after *each* event. Normally, the standby will be used in a while loop where at every event one or more conditions are checked.

The model with standby is:

```kotlin
//{!bank/standby/Bank3ClerksStandby.kt!}
```

In this case, the condition is checked frequently with:

```kotlin
while(waitingLine.isEmpty())
    standby()
```


The rest of the code is very similar to the version with states.

<!--https://squidfunk.github.io/mkdocs-material/reference/admonitions-->

!!! warning

    It is very important to realize that this mechanism can have significant impact on the performance, as after EACH event, the component becomes current and has to be checked. In general, it is recommended to try and use [state](../state.md)s or a more straightforward `passivate`/`activate` construction.


<!--TODO Document `Bank3ClerksData.kt`-->
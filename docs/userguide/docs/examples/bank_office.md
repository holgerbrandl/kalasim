<!--## Customer Queue: A bank example-->

Now let's move to a more realistic model. Here customers are arriving in a bank, where there is one clerk. This clerk handles the customers in *first in first out* (FIFO) order.

We see the following processes:

* The customer generator that creates the customers, with an inter-arrival time of `uniform(5,15)`
* The customers
* The clerk, which serves the customers in a constant time of 30 (overloaded and non steady state system)

We need a queue for the customers to wait for service.

The model code is:

```kotlin hl_lines="17 19 33 35 36 49 67"
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
yield(hold(10.0))
```

After that hold the customer is activated and will terminate:

```kotlin
customer.activate()
```

In the main section of the program, we create the `CustomerGenerator`, the `Clerk` and a [`ComponentQueue`](../component.md#queue) called waitingline. Here the customer generator is implemented as a custom instance of `Component` for educational puroposes. Using the provided [`ComponentGenerator`](../component.md#component-generator) API would be more concise.

```kotlin
yield(hold(uniform(5.0, 15.0).sample()))
```

will do the statistical sampling and wait for that time till the next customer is created.

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
      "mean": 0.111,
      "standard_deviation": 0.317
    },
    "excl_zeros": {
      "duration": 5.541386232954704,
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
      "ninty_pct_quantile": 2.968653617609821,
      "median": 1.524,
      "mean": 1.108,
      "nintyfive_pct_quantile": 2.968653617609821,
      "standard_deviation": 1.524
    },
    "excl_zeros": {
      "entries": 2,
      "ninty_pct_quantile": 2.968653617609821,
      "median": 0.28,
      "mean": 2.771,
      "nintyfive_pct_quantile": 2.968653617609821,
      "standard_deviation": 0.28
    }
  },
  "type": "QueueStatistics",
  "timestamp": 50
}
```


## 3 Clerks

Now, let's add more clerks. Here we have chosen to put the three clerks in a list:

```
clerks = [Clerk() for _ in range(3)]
```


although in this case we could have also put them in a salabim queue, like:

```
clerks = sim.Queue('clerks')
for _ in range(3):
    Clerk().enter(clerks)
```


And, to restart a clerk:

```
for clerk in clerks:
    if clerk.ispassive():
       clerk.activate()
       break  # reactivate only one clerk
```


The complete source of a three clerk post office:

```kotlin
//{!bank/threeclerks/Bank3Clerks.kt!}
```

## Bank Office with Resources

The salabim package contains another useful concept for modelling: resources.
Resources have a limited capacity and can be claimed by components and released later.

In the model of the bank with the same functionality as the above example, the
clerks are defined as a resource with capacity 3.

The model code is:

```kotlin
//{!bank/resources/Bank3ClerksResources.kt!}
```

Let's look at some details.:

```
clerks = sim.Resource('clerks', capacity=3)
```


This defines a resource with a capacity of 3.

And then, a customer, just tries to claim one unit (=clerk) from the resource with:

```
yield self.request(clerks)
```


Here, we use the default of 1 unit. If the resource is not available, the customer just
waits for it to become available (in order of arrival).

In contrast with the previous example, the customer now holds itself for 30 time units.

And after these 30 time units, the customer releases the resource with:

```
self.release()
```



The effect is that salabim then tries to honor the next pending request, if any.

`(actually, in this case this release statement is not required, as resources that were claimed are automatically
released when a process terminates).`

The statistics are maintained in two system queue, called clerk.requesters() and clerk.claimers().

The output is very similar to the earlier example. The statistics are exactly the same.

##  Bank Office with Balking and Reneging

Now, we assume that clients are not going to the queue when there are more than 5 clients
waiting (balking). On top of that, if a client is waiting longer than 50, he/she will
leave as well (reneging).

The model code is:

```kotlin
//{!bank/reneging/Bank3ClerksReneging.kt!}
```

Let's look at some details.:

```
yield self.cancel()
```


This makes the current component (a customer) a data component (and be subject to
garbage collection), if the queue length is 5 or more.

The reneging is implemented by a hold of 50. If a clerk can service a customer, it will take
the customer out of the waitingline and will activate it at that moment. The customer just has to check
whether he/she is still in the waiting line. If so, he/she has not been serviced in time and thus will renege.:

```
yield self.hold(50)
if self in waitingline:
   self.leave(waitingline)
   env.number_reneged += 1
else:
    self.passivate()
```


All the clerk has to do when starting servicing a client is to get the next customer in line
out of the queue (as before) and activate this customer (at time now). The effect is that the hold
of the customer will end.:

```
self.customer = waitingline.pop()
self.customer.activate()
```


##  Bank Office with Balking and Reneging (resources)

Now we show how the balking and reneging is implemented with resources.

The model code is:

```kotlin
//{!bank/reneging_resources/Bank3ClerksRenegingResources.kt!}
```

As you can see, the balking part is exactly the same as in the example without resources.

For the renenging, all we have to do is add a fail_delay:

```
yield self.request(clerks, fail_delay=50)
```


If the request is not honored within 50 time units, the process continues after that request statement.
And then, we just check whether the request has failed:

```
if self.failed():
   env.number_reneged += 1
```


This example shows clearly the advantage of the resource solution over the passivate/activate method, in this example.

##  Bank Office with States

The salabim package contains yet another useful concept for modelling: states.
In this case, we define a state called worktodo.

The model code is:

```kotlin
//{!bank/state/Bank3ClerksState.kt!}
```

Let's look at some details.:

```
worktodo = sim.State('worktodo')
```


This defines a state with an initial value False.

In the code of the customer, the customer tries to trigger one clerk with:

```
worktodo.trigger(max=1)
```


The effect is that if there are clerks waiting for worktodo, the first clerk's wait is honored and
that clerk continues its process after:

```
yield self.wait(worktodo)
```


Note that the clerk is only going to wait for worktodo after completion of a job if there
are no customers waiting.

## Bank Office with Standby

The salabim package contains yet another powerful process mechanism, called standby. When a component
is in standby mode, it will become current after *each* event. Normally, the standby will be
used in a while loop where at every event one or more conditions are checked.

The model with standby is:

```kotlin
//{!bank/standby/Bank3ClerksStandby.kt!}
```

In this case, the condition is checked frequently with:

```
while len(waitingline) == 0:
    yield self.standby()
```


The rest of the code is very similar to the version with states.

.. warning:

    It is very important to realize that this mechanism can have significant impact on the performance,
    as after EACH event, the component becomes current and has to be checked.
    In general it is recommended to try and use states or a more straightforward passivate/activate
    construction.


**{todo}** Document `Bank3ClerksData.kt`
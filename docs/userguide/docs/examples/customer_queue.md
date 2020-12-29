## Customer Queue: A bank example

Now let's move to a more realistic model. Here customers are arriving in
a bank, where there is one clerk. This clerk handles the
customers in *first in first out* (FIFO) order.

We see the following processes:

- The customer generator that creates the customers, with an inter-arrival time of `uniform(5,15)`
- The customers
- The clerk, which serves the customers in a constant time of 30 (overloaded and non steady state system)

And we need a queue for the customers to wait for service.

The model code is:

```kotlin
//{!bank/oneclerk/Bank1clerk.kt!}
```

Let's look at some details:

   yield self.hold(sim.Uniform(5, 15).sample())

will do the statistical sampling and wait for that time till the next customer is created.

With:

```
self.enter(waitingline)
```


the customer places itself at the tail of the waiting line.

Then, the customer checks whether the clerk is idle, and if so, activates him immediately.:

```
if clerk.ispassive():
    clerk.activate()
```


Once the clerk is active (again), it gets the first customer out of the waitingline with:

```
self.customer = waitingline.pop()
```


and holds for 30 time units with:

```
yield self.hold(30)
```


After that hold the customer is activated and will terminate:

```
self.customer.activate()
```


In the main section of the program, we create the CustomerGenerator, the Clerk and a queue called waitingline.
After the simulation is finished, the statistics of the queue are presented with:

```
waitingline.print_statistics()
```


The output looks like

```
    line#         time current component    action                               information
    -----   ---------- -------------------- -----------------------------------  ------------------------------------------------
                                            line numbers refers to               Example - bank, 1 clerk.py
       30                                   default environment initialize
       30                                   main create                          
       30        0.000 main                 current                              
       32                                   customergenerator create             
       32                                   customergenerator activate           scheduled for      0.000 @    6  process=process
       33                                   clerk.0 create                       
       33                                   clerk.0 activate                     scheduled for      0.000 @   21  process=process
       34                                   waitingline create                   
       36                                   main run                             scheduled for     50.000 @   36+
        6        0.000 customergenerator    current                              
        8                                   customer.0 create                    
        8                                   customer.0 activate                  scheduled for      0.000 @   13  process=process
        9                                   customergenerator hold               scheduled for     14.631 @    9+
       21        0.000 clerk.0              current                              
       24                                   clerk.0 passivate                    
       13        0.000 customer.0           current                              
       14                                   customer.0                           enter waitingline
       16                                   clerk.0 activate                     scheduled for      0.000 @   24+
       17                                   customer.0 passivate                 
       24+       0.000 clerk.0              current                              
       25                                   customer.0                           leave waitingline
       26                                   clerk.0 hold                         scheduled for     30.000 @   26+
        9+      14.631 customergenerator    current                              
        8                                   customer.1 create                    
        8                                   customer.1 activate                  scheduled for     14.631 @   13  process=process
        9                                   customergenerator hold               scheduled for     21.989 @    9+
       13       14.631 customer.1           current                              
       14                                   customer.1                           enter waitingline
       17                                   customer.1 passivate                 
        9+      21.989 customergenerator    current                              
        8                                   customer.2 create                    
        8                                   customer.2 activate                  scheduled for     21.989 @   13  process=process
        9                                   customergenerator hold               scheduled for     32.804 @    9+
       13       21.989 customer.2           current                              
       14                                   customer.2                           enter waitingline
       17                                   customer.2 passivate                 
       26+      30.000 clerk.0              current                              
       27                                   customer.0 activate                  scheduled for     30.000 @   17+
       25                                   customer.1                           leave waitingline
       26                                   clerk.0 hold                         scheduled for     60.000 @   26+
       17+      30.000 customer.0           current                              
                                            customer.0 ended                     
        9+      32.804 customergenerator    current                              
        8                                   customer.3 create                    
        8                                   customer.3 activate                  scheduled for     32.804 @   13  process=process
        9                                   customergenerator hold               scheduled for     40.071 @    9+
       13       32.804 customer.3           current                              
       14                                   customer.3                           enter waitingline
       17                                   customer.3 passivate                 
        9+      40.071 customergenerator    current                              
        8                                   customer.4 create                    
        8                                   customer.4 activate                  scheduled for     40.071 @   13  process=process
        9                                   customergenerator hold               scheduled for     54.737 @    9+
       13       40.071 customer.4           current                              
       14                                   customer.4                           enter waitingline
       17                                   customer.4 passivate                 
       36+      50.000 main                 current                              

    Statistics of waitingline at        50    
                                                                         all    excl.zero         zero
    -------------------------------------------- -------------- ------------ ------------ ------------
    Length of waitingline                        duration             50           35.369       14.631
                                                 mean                  1.410        1.993
                                                 std.deviation         1.107        0.754

                                                 minimum               0            1    
                                                 median                2            2    
                                                 90% percentile        3            3    
                                                 95% percentile        3            3    
                                                 maximum               3            3    

    Length of stay in waitingline                entries               2            1            1    
                                                 mean                  7.684       15.369
                                                 std.deviation         7.684        0    

                                                 minimum               0           15.369
                                                 median               15.369       15.369
                                                 90% percentile       15.369       15.369
                                                 95% percentile       15.369       15.369
                                                 maximum              15.369       15.369
```

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

## A bank office example with resources

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

##  The bank office example with balking and reneging

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


## The bank office example with balking and reneging (resources)

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

##  The bank office example with states

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

##  The bank office example with standby

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
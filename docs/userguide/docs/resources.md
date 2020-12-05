Resources
---------

Resources are a powerful way of process interaction. 

A resource has always a capacity (which can be zero and even negative). This capacity will be specified at time of creation, but
may change over time.

There are two of types resources:

* standard resources, where each claim is associated with a component (the claimer). It is not necessary that the claimed quantities are integer.
* anonymous resources, where only the claimed quantity is registered. This is most useful for dealing with levels, lengths, etc.

Resources are defined like:

```kotlin
val clerks = Resource("clerks", capacity=3)
```

And then a component can request a clerk:

```kotlin
yield(request(clerks))  // request 1 from clerks 
yield(request(clerks withQuantity 2)) // request 2s from clerks
```
    
It is also possible to request for more resources at once:

```kotlin
yield(request(clerks withQuantity 1, assistance withQuantity 2))  // request 1 from clerks AND 2 from assistance
```

Resources have a queue `requesters` containing all components trying to claim from the resource.
And a queue `claimers` containing all components claiming from the resource
(not for anonymous resources).

It is possible to release a quantity from a resource with `c.release()`, e.g.

```kotlin
r.release()  // releases all claimed quantity from r
r.release(2)  // release quantity 2 from r
```
    
Alternatively, it is possible to release from a resource directly, e.g.

    r.release()  // releases the total quantity from all claiming components
    r.release(10)  // releases 10 from the resource; only valid for anonymous resources
    
After a release, all requesting components will be checked whether their claim can be honored.

Resources have a number monitors:

* claimers().length
* claimers().length_of_stay
* requesters().length
* requesters().length_of_stay
* claimed_quantity
* available_quantity
* capacity
* occupancy  (=claimed_quantity / capacity)

By default, all monitors are enabled.

With ``r.print_statistics()`` the key statistics of these all monitors are printed.

E.g.:

.. code-block:: none

    Statistics of clerk at     50000.000
                                                                         all    excl.zero         zero
    -------------------------------------------- -------------- ------------ ------------ ------------
    Length of requesters of clerk                duration          50000        48499.381     1500.619
                                                 mean                  8.427        8.687
                                                 std.deviation         4.852        4.691

                                                 minimum               0            1    
                                                 median                9           10    
                                                 90% percentile       14           14    
                                                 95% percentile       16           16    
                                                 maximum              21           21    

    Length of stay in requesters of clerk        entries            4995         4933           62    
                                                 mean                 84.345       85.405
                                                 std.deviation        48.309       47.672

                                                 minimum               0            0.006
                                                 median               94.843       95.411
                                                 90% percentile      142.751      142.975
                                                 95% percentile      157.467      157.611
                                                 maximum             202.153      202.153

    Length of claimers of clerk                  duration          50000        50000            0    
                                                 mean                  2.996        2.996
                                                 std.deviation         0.068        0.068

                                                 minimum               1            1    
                                                 median                3            3    
                                                 90% percentile        3            3    
                                                 95% percentile        3            3    
                                                 maximum               3            3    

    Length of stay in claimers of clerk          entries            4992         4992            0    
                                                 mean                 30           30    
                                                 std.deviation         0.000        0.000

                                                 minimum              30.000       30.000
                                                 median               30           30    
                                                 90% percentile       30           30    
                                                 95% percentile       30           30    
                                                 maximum              30.000       30.000

    Capacity of clerk                            duration          50000        50000            0    
                                                 mean                  3            3    
                                                 std.deviation         0            0    

                                                 minimum               3            3    
                                                 median                3            3    
                                                 90% percentile        3            3    
                                                 95% percentile        3            3    
                                                 maximum               3            3    

    Available quantity of clerk                  duration          50000          187.145    49812.855
                                                 mean                  0.004        1.078
                                                 std.deviation         0.068        0.268

                                                 minimum               0            1    
                                                 median                0            1    
                                                 90% percentile        0            1    
                                                 95% percentile        0            2    
                                                 maximum               2            2    

    Claimed quantity of clerk                    duration          50000        50000            0    
                                                 mean                  2.996        2.996
                                                 std.deviation         0.068        0.068

                                                 minimum               1            1    
                                                 median                3            3    
                                                 90% percentile        3            3    
                                                 95% percentile        3            3    
                                                 maximum               3            3      
                                             
    Occupancy of clerks                          duration          50000        50000            0    
                                                 mean                  0.999        0.999
                                                 std.deviation         0.023        0.023
    
                                                 minimum               0.333        0.333
                                                 median                1            1    
                                                 90% percentile        1            1    
                                                 95% percentile        1            1    
                                                 maximum               1            1
     
With ``r.print_info()`` a summary of the contents of the queues can be printed.

E.g.:

    Resource 0x112e8f0b8
      name=clerk
      capacity=3
      requesting component(s):
        customer.4995        quantity=1
        customer.4996        quantity=1
      claimed_quantity=3
      claimed by:
        customer.4992        quantity=1
        customer.4993        quantity=1
        customer.4994        quantity=1
        
The capacity may be changed with ``r.set_capacity(x)``. Note that this may lead to requesting
components to be honored.

Querying of the capacity, claimed quantity, available quantity and occupancy can be done via the label monitors:
``r.capacity()``, ``r.claimed_quantity()``, ``r.available_quantity()`` and ``r.occupancy()``

If the capacity of a resource is constant, which is very common, the mean occupancy can be found with:

    r.occupancy.mean()
    
When the capacity changes over time, it is recommended to use:
    
    occupancy = r.claimed_quantity.mean() / r.capacity.mean()
    
to obtain the mean occupancy.

Note that the occupancy is set to 0 if the capacity of the resource is &lt;= 0.

Additional methods for anonymous resources
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
For anonymous resources, it may be not allowed to exceed the capacity and have a component wait
for enough (claimed) capacity to be available. That may be accomplished by using a negative
quantity in the ``self.request`` call.

Alternatively, it possible to use the ``Component.put`` method, where quantities of anonymous resources
are negated. For symmetry reasons, salabim also offers the ``Component.get()`` method, which is behaves exactly
like ``Component.request``.

The model below illustrates the use of get and put.

.. literalinclude:: ..\..\sample models\Gas station.py
    :linenos:</pre><!--EndFragment-->
</body>
</html>


//// Preemptive Resources

preemptive resources

It is now possible to specify that a resource is to be preemptive, by adding preemptive=True when the resource
is created.
If a component requests from a preemptive resource, it may bump component(s) that are claiming from
the resource, provided these have a lower priority = higher value).
If component is bumped, it releases the resource and is the activated, thus essentially stopping the current
action (usually hold or passivate).
Therefore, it is necessary that a component claiming from a preemptive resource should check
whether the component is bumped or still claiming at any point where they can be bumped.
This can be done with the method Component.isclaiming which is True if the component is claiming from the resource,
or the opposite (Component.isbumped) which is True is the component is not claiming from te resource.
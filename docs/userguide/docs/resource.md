# Resources

Resources are a powerful way of process interaction.  Next to [process definitions](component.md#process-definition), resources are usually the most important elements of a simulation. Resources allow to model rate-limits which are omnipresent in every business process.

A resource has always a capacity (which can be zero and even negative). This capacity will be specified at time of creation, but can be changed later with `r.capacity = newCapacity`. Note that this may lead to requesting components to be honored if possible.

<!--see org.kalasim.test.RequestTests#`it should reevaluate requests upon capacity changes`-->

There are two of types resources:

* [*Regular resources*](#regular-resources), where each claim is associated with a component (the claimer). It is not necessary that the claimed quantities are integer.
* [*Depletable resources*](#depletable-resources), where only the claimed quantity is registered. This is most useful for dealing with levels, lengths, etc.

<!-- todo consider to add a dedicated container type instead of anonymous resources https://simpy.readthedocs.io/en/latest/topical_guides/resources.html#containers-->

## Regular Resources

Regular resources are declared with:

```kotlin
val clerks = Resource("clerks", capacity=3)
```

Any [component](component.md) can `request` from a resource in its [process method](component.md#creation-of-a-component). The user must not use [`request`](component.md#request) outside of a component's  process definition.

`request` has the effect that the component will check whether the requested quantity from a resource is available. It is possible to check for multiple availability of a certain quantity from several resources.

Regular resources have a [queue](collections.md#queue) called `requesters` containing all components trying to claim from the resource. In addition, there is a queue `claimers` containing all components claiming from the resource. Both queues can not be modified but very useful for analysis.

Notes

* `request` is not allowed for data components or main.
* If to be used for the current component (which will be nearly always the case), use `yield (request(...))`.
* If the same resource is specified more that once, the quantities are summed.
* The requested quantity may exceed the current capacity of a resource.
* The parameter `failed` will be reset by a calling `request` or `wait`.

## Request Scope

The most common usage pattern for resources is the _request scope_ which 

1. requests a resource, 
2. executes some action,
3. and finally releases the claimed resources 

```kotlin
request(clerks) { //1
    hold(1, description ="doing something") //2
} //3
```

In the example, `kalasim` will release the clerks automatically at the end of the request scope.

When requesting from a single resource in a nested way, claims are merged.

## Unscoped Usage

The user can omit the request scope (not recommended and mostly not needed), and release claimed resources with `release`.

```kotlin
request(clerks)

hold(1, description ="doing something")

release(clerks) 
```


Typically, this is only needed when releasing a defined quantity from a resource with `c.release()`, e.g.

```kotlin
customer.release()  // releases all claimed quantity from r
customer.release(2)  // release quantity 2 from r
```

After a release, all other requesting components will be checked whether their claim can be honored.


## Examples

* [Bank Office with Resources](examples/bank_office.md#bank-office-with-resources)
* [Car Wash](examples/car_wash.md)
* [Traffic](examples/traffic.md)
* [Gas Station](examples/gas_station.md)


## Quantity

Some requests may request more than 1 units from a requests. The number of requested resource units is called _request quantity_. Quantites are strictly positives, and kalasim also supports non-integer quantities. To request more than one unit from a resource, the user can use the follow API:

```kotlin
// request 1 from clerks 
request(clerks)  

// request 2 elements from clerks
request(clerks, quantity = 2) 

// also an infix version is supported
request(clerks withQuantity 2)
```

## Request Honor Policies

* **Strict first come, first serve (S-FCFS)**. With the policy requests will be honored by order of appearance. So, it  actually will wait to honour "big" requests, if if smaller requests that could honored already are queueing up already. This is the the default, as we assume this the most intuitive behavior in most situations.
* **Relaxed first come, first serve (R-FCFS)**: honour first claimable request first. This will prefer small requests, i.e. requests for a small quantity, over big requests.
* **Smallest Quantity First (SQF)** (to maximize "customer" throughput) with FCFS to resolve ambiguities. This will maximize the total number of requests being honored, whereas large requests may need to wait for a long time.
* **Weighted SQF**: Here the user can supply a weight `α` that is used to compute an ordering based on `quantity*(1-α)+ time_since_insert*α)`. This will progressively weigh insert-time against request quantity, to ensure that also larger request will finally be honored even if new small requests are coming in.

!!!important
[priorities](#request-priority) that always take precedence over the honor policies. If a user sets a request priority, it will be respected first. That is, it does always try honouring by priority first, and only once all requests at the highest prio level are honoured, it will climb down the ladder. Within a priority-level the selected honor policy is applied.

!!!note
A SQF policy could also be realized by using the negated quantity as request priority. However, for sake of clarity is is recommended to use priorities to actual reflect business/domain needs, and use the provided SQL as baseline policy.

The principle applies to both regular but also [depletable resources](#depletable-resources). Just imagine an resource that is constantly low on supply. When new supply becomes available, the resource could serve as many requesters as possible. 

Also for non-anonymous resources this concept applies, e.g. in customer support, where customers require one or multiple mechanics, and the company decides to serve the least staffing-intense requests first. This is from what I understood so far not possible with the current resource model, and would also not be supported with the envisioned strict mode. It is IMHO analogous to a SJF policy, although the name is I guess a bit misleading as it relates more to task duration than quantity, and something more appropriate could be used to better clarify the intent to the user. In case of several requests with the same minimal quantity, FCFS could be used to impose a secondary ordering scheme. I hope this clarifies "Shortest Job First with FCFS as a secondary ordering scheme".

## Request Priority

As multiple components may request the same resource, it is important to prioritize requests. This is possible by providing a request priority

```kotlin
request(clerks withPriority IMPORTANT) 
```

`kalasim` will order requests on a resource by priority.

There are different predefined priorities which correspond the following sort-levels 

* `LOWEST` (-20)
* `LOW` (-10)
* `NORMAL` (0)
* `IMPORTANT` (10)
* `CRITICAL` (20)

The user can also create more fine-grained priorities with `Priority(23)`


## Multiple resources

It is also possible to request for more resources at once. In the following examples, we request 1 quantity from `clerks` **AND** 2 quantities from `assistance`.

```kotlin
request(clerks withQuantity 1, assistance withQuantity 2) 
```

To request alternative resources, the user can define the parameter `oneOf=true`, which will would result in requesting 1 quantity from `clerks` **OR** 2 quantities from `assistance`.

Another method to query from a pool of resources are group requests. These are simply achieved by grouping resources in a `List` before requesting from it using `oneOf=true`.

```kotlin
//{!api/ResourceGroups.kts!}
```

Typical use cases are staff models, where certain colleagues have similar but not identical qualification. In case of the same qualification, a single resource with a `capacity` equal to the staff size, would be usually the better/correct solution.

## Activity Log

Resources have a `activities` attribute that provides a history of [scoped requests](#request-scope) as a `List<ResourceActivityEvent>`

```kotlin
r1.activities
    .plot(y={resource.name},  yend={resource.name},x={start},xend={end}, color={activity})
    .geomSegment(size=10.0)
    .yLabel("Resource")
```

![](resource_timeline.png)

This visualization is also provided by a built-in `display()` extension for the activity log.

There's also a [notebook](https://github.com/holgerbrandl/kalasim/blob/master/simulations/notebooks/ResourceTimeline.ipynb) with a complete example.



## Timeline

The `timeline` attribute of a resource reports the progression of all its major metrics. The `timeline` provides a changelog of a resource in terms of:

* `capacity` of the resource
* `claimed` capacity
* `# requesters` in the queue of the resource at a given time
* `# claimers` claiming from the resource at a given time

For convenience also 2 inferrable attributes are also included:

* `availability` 
* `occupancy`

Technically, the `timeline` is a `List<ResourceTimelineSegment>` that covers the entire lifespan of the resource as step functions per metric.

Example (from [example notebook](https://github.com/holgerbrandl/kalasim/blob/master/simulations/notebooks/ResourceTimeline.ipynb)) that illustrates how the `timeline` can be used to visualize some aspects of the resource utilization over time.

```kotlin
r.timelime
    .filter{listOf(ResourceMetric.Capacity, ResourceMetric.Claimed).contains(it.metric)}
    .plot(x={start}, y={value} , color={metric})
    .geomStep()
    .facetWrap("color", ncol=1, scales=FacetScales.free_y)
```
![](timeline_example.png)

This visualization is also provided by a built-in `display()` extension for the timeline attribute.

## Monitors

Resources have a number of monitors:

* claimers
  * `queueLength`
  * `lengthOfStay`
* requesters
  * `queueLength`
  * `lengthOfStay`
* `claimedQuantity`
* `availableQuantity`
* `capacity`
* `occupancy`  (= claimed quantity / capacity)

By default, all monitors are enabled.

With `r.printStatistics()` the key statistics of these all monitors are printed. E.g.

```json
{
  "availableQuantity": {
    "duration": 3000,
    "min": 0,
    "max": 3,
    "mean": 0.115,
    "standard_deviation": 0.332
  },
  "claimedQuantity": {
    "duration": 3000,
    "min": 0,
    "max": 3,
    "mean": 2.885,
    "standard_deviation": 0.332
  },
  "occupancy": {
    "duration": 3000,
    "min": 0,
    "max": 1,
    "mean": 0.962,
    "standard_deviation": 0.111
  },
  "name": "clerks",
  "requesterStats": {
    "queue_length": {
      "all": {
        "duration": 3000,
        "min": 0,
        "max": 3,
        "mean": 0.564,
        "standard_deviation": 0.727
      },
      "excl_zeros": {
        "duration": 1283.1906989415463,
        "min": 1,
        "max": 3,
        "mean": 1.319,
        "standard_deviation": 0.49
      }
    },
    "name": "requesters of clerks",
    "length_of_stay": {
      "all": {
        "entries": 290,
        "ninety_pct_quantile": 15.336764014133065,
        "median": 6.97,
        "mean": 5.771,
        "ninetyfive_pct_quantile": 17.9504616361896,
        "standard_deviation": 6.97
      },
      "excl_zeros": {
        "entries": 205,
        "ninety_pct_quantile": 17.074664209460025,
        "median": 7.014,
        "mean": 8.163,
        "ninetyfive_pct_quantile": 19.28443602612993,
        "standard_deviation": 7.014
      }
    },
    "type": "QueueStatistics"
  },
  "type": "ResourceStatistics",
  "timestamp": 3000,
  "claimerStats": {
    "queue_length": {
      "all": {
        "duration": 3000,
        "min": 0,
        "max": 3,
        "mean": 2.885,
        "standard_deviation": 0.332
      },
      "excl_zeros": {
        "duration": 3000,
        "min": 1,
        "max": 3,
        "mean": 2.885,
        "standard_deviation": 0.332
      }
    },
    "name": "claimers of clerks",
    "length_of_stay": {
      "all": {
        "entries": 287,
        "ninety_pct_quantile": 30,
        "median": 0,
        "mean": 30,
        "ninetyfive_pct_quantile": 30,
        "standard_deviation": 0
      },
      "excl_zeros": {
        "entries": 287,
        "ninety_pct_quantile": 30,
        "median": 0,
        "mean": 30,
        "ninetyfive_pct_quantile": 30,
        "standard_deviation": 0
      }
    },
    "type": "QueueStatistics"
  },
  "capacity": {
    "duration": 3000,
    "min": 3,
    "max": 3,
    "mean": 3,
    "standard_deviation": 0
  }
}

```
     
With `r.printInfo()` a summary of the contents of the queues can be printed. E.g.:

```json
{
  "claimedQuantity": 3,
  "requestingComponents": [
    {
      "component": "Customer.292",
      "quantity": 1
    },
    {
      "component": "Customer.291",
      "quantity": 1
    }
  ],
  "creationTime": 0,
  "name": "clerks",
  "claimedBy": [
    {
      "first": "Customer.288",
      "second": null
    },
    {
      "first": "Customer.289",
      "second": null
    },
    {
      "first": "Customer.290",
      "second": null
    }
  ],
  "capacity": 3
}
```

Querying of the capacity, claimed quantity, available quantity and occupancy can be done with:
`r.capacity`, `r.claimedQuantity`, `r.availableQuantity` and `r.occupancy`. All quantities are tracked by corresponding level monitors to provide statistics.

If the capacity of a resource is constant, which is very common, the mean occupancy can be found with:

    r.occupancyMonitor.statistics().mean
    
When the capacity changes over time, it is recommended to use:
    
    occupancy = r.claimedQuanityMonitor.statistics().mean / r.capacityMonitor.statistics().mean()
    
to obtain the mean occupancy.

Note that the occupancy is set to 0 if the capacity of the resource is <= 0.

## Resource Selection

<!--https://r-simmer.org/reference/select.html-->
<!--Equivalent of simmer::select (See Ucar2019,p12) with multiple select policies-->

There is a special mechanism to select resources dynamically. With `selectResource()` a resource can be  selected from a list of resources using a policy. There are several policies provided:

* `SHORTEST_QUEUE`: The resource with the shortest queue, i.e. the least busy resource is selected.
* `ROUND_ROBIN`: Resources will be selected in a cyclical order.
* `FIRST_AVAILABLE`: The first available resource is selected.
* `RANDOM`: A resource is randomly selected.

The `*_AVAILABLE` policies check for resource availability (i.e. whether the current capacity is sufficient to honor the requested quantity (defaulting to `1`). Resources that do not meet this requirement will not be considered for selection. When using these policies, an error will be raised if all resources are unavailable.

!!! warning
    With `selectResource`, a resource will be only selected. It won't actually [request](component.md#request) it.

Example

```kotlin
//{!api/ResourceSelection.kts!}
```

An alternative more direct approach to achieve round-robin resource selection (e.g. for nested calls) could also be implemented ([example](https://github.com/holgerbrandl/kalasim/blob/master/src/test/kotlin/org/kalasim/examples/api/ResourceSelectionClassic.kts)) with an iterator.



<!--A simple round-robin selector could also be achieved with a simple iterator-->
<!--```kotlin-->
<!--val resources = List(3) { Resource() }-->
<!--val resIter = resources.repeat().iterator()-->
<!--//...-->
<!--while(true) {-->
<!--    request(resIter.next())-->
<!--}-->
<!--```-->


##  Depletable Resources

For depletable (which are also sometimes referred to as _anonymous_) resources, it may be not allowed to exceed the capacity and have a component wait for enough (claimed) capacity to be available. That may be accomplished by using a negative quantity in the `Component.request()` call. However, to clarify the semantics of resource depletion, the API includes a dedicated `DepletableResource`. 

* A depletable resource can be consumed with `Component.take()`.
* A depletable resource can refilled/recharged with `Component.put()`.

!!!info
   Both `put()` and `take` are just typesafe wrappers around [`request()`](#resources).  With `put()` quantities of resources are negated before calling `Component.request()` internally.

To create a depletable resource we do
```kotlin
val tank = DepletableResource(capacity = 10, initialLevel = 3)
```
We can declare its maximum capacity and its initial fill level. The latter is optional and defaults to the capacity of the resource.

In addition to the `Resource` attributs, depletable resources have the following attributes to streamline model building

* `level` - Indicates the current level of the resource
* `isDepleted` - Indicates if depletable resource is depleted (level==0)
* `isFull` - Indicates if depletable resource is at full capacity

It may happen that a `put` would fail because its quantity would exceed the depletable resource's `capacity`. With `PutOverflowMode` different modes can be configured to handle such situations if just a _single_

1. `CAP` - Cap request at capacity level (Default)
2. `FAIL`-  Fail if request size exceeds resource capacity.
3. `SCHEDULE` - Schedule put if necessary, hoping for a later capacity increase.


The model below illustrates the use of `take` and `put`. See the [Gas Station](examples/gas_station.md) simulation for a living example.


## Pre-emptive Resources

<!--see salabim change-log version 19.0.9  2019-10-08-->

It is possible to specify that a resource is to be preemptive, by adding `preemptive = true` when the resource is created.

<!--todo learn from https://simpy.readthedocs.io/en/latest/topical_guides/resources.html#preemptiveresource-->

If a component requests from a preemptive resource, it may bump component(s) that are claiming from
the resource, provided these have a lower priority. If component is bumped, it releases the resource and is then activated, thus essentially stopping the current
action (usually `hold` or `passivate`).

Therefore, a component claiming from a preemptive resource should check whether the component is bumped or still claiming at any point where they can be bumped. This can be done with the method `Component.isClaiming(resource)` which is `true` if the component is claiming from the resource, or the opposite (Component.isBumped) which is `true` is the component is not claiming from the resource.

Examples

* [Machine Shop](examples/machine_shop.md)
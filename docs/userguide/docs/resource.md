# Resources

Resources are a powerful way of process interaction. 

A resource has always a capacity (which can be zero and even negative). This capacity will be specified at time of creation, but can be changed later with `r.capacity = newCapacity`. Note that this may lead to requesting components to be honored if possible.

<!--see org.kalasim.test.RequestTests#`it should reevaluate requests upon capacity changes`-->

There are two of types resources:

* *Standard resources*, where each claim is associated with a component (the claimer). It is not necessary that the claimed quantities are integer.
* [*Depletable resources*](#depletable-resources), where only the claimed quantity is registered. This is most useful for dealing with levels, lengths, etc.

<!-- todo consider to add a dedicated container type instead of anonymous resources https://simpy.readthedocs.io/en/latest/topical_guides/resources.html#containers-->

Resources are declared with:

```kotlin
val clerks = Resource("clerks", capacity=3)
```

Any [component](component.md) can `request` from a resource in its [process method](component.md#creation-of-a-component). The user must not use `request` outside of a component's  process definition.

`request` has the effect that the component will check whether the requested quantity from a resource is available. It is possible to check for multiple availability of a certain quantity from several resources.

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


## Unscoped Usage

The user can omit the request scope (not recommended for your own good).

```kotlin
request(clerks)

hold(1, description ="doing something")

release(clerks) 
```
## Examples

* [Bank Office with Resources](examples/bank_office.md#bank-office-with-resources)
* [Car Wash](examples/car_wash.md)
* [Traffic](examples/traffic.md)
* [Gas Station](examples/gas_station.md)


## Quantity

Some requests may have a capacity greater than 1. To request more than one unit from a resource, the user can use `withQuantity`:

```kotlin
request(clerks)  // request 1 from clerks 
request(clerks withQuantity 2) // request 2 elements from clerks
```

## Request Priority

As multiple components may request the same resource, it is important to prioritize requests. This is possible by providing a request priority

```kotlin
request(clerks withPriority IMPORTANT) 
```

There are different predefined priorities which correspond the following sort-levels 

* `LOWEST` (-20)
* `LOW` (-10)
* `NORMAL` (0)
* `IMPORTANT` (20)
* `CRITICAL` (20)

The user can also create more fine-grained priorities with


### Multiple resources

It is also possible to request for more resources at once. In the following examples, we request 1 quantity from `clerks` **AND** 2 quantities from `assistance`.

```kotlin
request(clerks withQuantity 1, assistance withQuantity 2) 
```

To request alternative resources, the user can define the parameter `oneOf=true`, which will would result in requesting 1 quantity from `clerks` **OR** 2 quantities from `assistance`.


Resources have a queue `requesters` containing all components trying to claim from the resource.
In addition, there is a queue `claimers` containing all components claiming from the resource (not for anonymous resources).

It is possible to release a quantity from a resource with `c.release()`, e.g.

```kotlin
customer.release()  // releases all claimed quantity from r
customer.release(2)  // release quantity 2 from r
```
    
Alternatively, it is possible to release from a resource directly, e.g.

```kotlin
// releases the total quantity from all claiming components:
r.release()  

// releases 10 from the resource; only valid for anonymous resources
r.release(10)
```
    
After a release, all requesting components will be checked whether their claim can be honored.

Notes

* `request` is not allowed for data components or main.
* If to be used for the current component (which will be nearly always the case), use `yield (request(...))`.
* If the same resource is specified more that once, the quantities are summed.
* The requested quantity may exceed the current capacity of a resource.
* The parameter `failed` will be reset by a calling `request` or `wait`.


## Monitors

Resources have a number monitors:

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

For depletable (which are also referred to as _anonymous_) resources, it may be not allowed to exceed the capacity and have a component wait for enough (claimed) capacity to be available. That may be accomplished by using a negative quantity in the `Component.request()` call.

To create a depleteable resource we **{done}**
```kotlin
val tank = DepletableResource(capacity = 10, initialLevel = 5)
```
We can declare its maximum capacity and its initial fill level. The latter is optional and defaults to the capacity of the resource.

Alternatively, it possible to use the `Component.put()` method, where quantities of anonymous resources are negated. For symmetry reasons, `kalasim` also offers the `Component.get()` method, which is behaves exactly like `Component.request()`.

The model below illustrates the use of `get` and `put`. See the [Gas Station](examples/gas_station.md) simulation for a living example.


## Pre-emptive Resources

<!--see salabim change-log version 19.0.9  2019-10-08-->

It is possible to specify that a resource is to be preemptive, by adding `preemptive = true` when the resource is created.

<!--todo learn from https://simpy.readthedocs.io/en/latest/topical_guides/resources.html#preemptiveresource-->

If a component requests from a preemptive resource, it may bump component(s) that are claiming from
the resource, provided these have a lower priority = higher value). If component is bumped, it releases the resource and is then activated, thus essentially stopping the current
action (usually `hold` or `passivate`).

Therefore, a component claiming from a preemptive resource should check whether the component is bumped or still claiming at any point where they can be bumped. This can be done with the method `Component.isClaiming(resource)` which is `true` if the component is claiming from the resource, or the opposite (Component.isBumped) which is `true` is the component is not claiming from the resource.

Examples

* [Machine Shop](examples/machine_shop.md)
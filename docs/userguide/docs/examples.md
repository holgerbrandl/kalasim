# Examples

## Extended Cars

The following example integrates three simulation entities

* A gas station with a limited number of pumps
* A traffic light that prevents cars from driving
* Multiple cars that need to pass the cross with the traffic light to reach a gas station. There each car needs to refill before it reaching its end of live within the simulation context

The example illustrates how to establish a simple interplay of  [states](state.md) and [resources](resource.md). It is realized elegantly with [dependency injection](basics.md#dependency-injection).

```kotlin
//{!ExtendedCars.kts!}
```

Here,  we use both lazy injection with `inject<T>()` and instance retrieval with `get<T>()`. For details see [koin reference](https://doc.insert-koin.io/#/koin-core/injection-parameters)


## Elevators

Parameters

* Origin/destination distribution of visitors
* Number of elevators
* Capacity of each elevator


## ATM Queue

Let's explore the expressiveness of `kalasim`s process description using a *traditional queuing* example, the [M/M/1](https://en.wikipedia.org/wiki/M/M/1_queue). This [Kendall's notation](https://en.wikipedia.org/wiki/Kendall%27s_notation) describes a single server - here a ATM - with exponentially distributed arrivals, exponential service time and an infinte queue.
<!--see Ucar2019, 4.1 for more details-->

![](https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Mm1_queue.svg/440px-Mm1_queue.svg.png)

The basic parameters of the system are

* λ - people arrival rate at the ATM
* µ - money withdrawal rate

If  λ/µ > 1, the queue is referred to as *unstable* since there are more arrivals than the ATM can handle. The queue will grow indefinitely.



```kotlin
//{!Atm.kt!}
```

The ATM example is inspired from the `simmer` paper [Ucar et al. 2019](https://www.jstatsoft.org/article/view/v090i02).

<!--TODO add analytics screenshots here-->


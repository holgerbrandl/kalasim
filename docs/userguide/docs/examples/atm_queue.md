<!--## ATM Queue-->

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

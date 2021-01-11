
The [Dining Philosophers problem](https://en.wikipedia.org/wiki/Dining_philosophers_problem) is a classical example in computer science to illustrate synchronisation issues in concurrent processes. It was originally formulated in 1965 by [E. W. Dijkstra](https://en.wikipedia.org/wiki/Edsger_W._Dijkstra) as a student exam exercise, and was later reworked in its current form by [Tony Hoare](https://en.wikipedia.org/wiki/Tony_Hoare):

> Some philosophers sit at a round table with bowls of spaghetti with tomato sauce and tasty cheese. Forks are placed between each pair of adjacent philosophers.
>
> Each philosopher must alternately think and eat. However, a philosopher can only eat spaghetti when they have both left and right forks. Each fork can be held by only one philosopher and so a philosopher can use the fork only if it is not being used by another philosopher. After an individual philosopher finishes eating, they need to put down both forks so that the forks become available to others. A philosopher can take the fork on their right or the one on their left as they become available, but cannot start eating before getting both forks.

![Dining Philosophers, with $N=4$.](philosophers.png){: .center}

> The problem is how to design a discipline of behavior (a concurrent algorithm) such that no philosopher will starve; i.e., each can forever continue to alternate between eating and thinking, assuming no philosopher can know when others may want to eat or think.

## Simulation

Let us define each philosopher as a process executing a thinking + eating loop, and acting concurrently on shared resources (the forks). Each process will follow a similar trajectory in which they

1. Spend some random time thinking until they become hungry.
2. Take one fork, when available, following a given policy.
3. After some lag, take the other fork, when available.
4. Spend some random time eating.
5. Put both forks down and go back to 1.

The following function sets up a simulation of $N$ dining philosophers as established above:

```kotlin
//{!DiningPhilosophers.kt!}
```

To enable a strictly typed simulation, we declare the [resource](../resource.md) `Fork` and [component](../component.md) `Philosopher`. The latter is associated to a process where the philosopher first thinks for some exponentially distributed time, takes a fork, meditates for a brief second, and finally takes the second fork once it becomes available. Both interactions modelled as [request](../component.md#request)s where we use a self-releasing request context. Once the philosopher has eaten, the whole process starts over again.

A variable number of philosophers (here N=4) is instantiated and are equipped with forks on their left and right.

<!--With the simplest algorithm, each philosopher would take, for example, the right fork first and then the left one. But it is easy to see that this policy may result in starvation:-->

<!--```{r, message=FALSE}-->
<!--fork_seq <- list(-->
<!--  Socrates   = c(1, 2),-->
<!--  Pythagoras = c(2, 3),-->
<!--  Plato      = c(3, 4),-->
<!--  Aristotle  = c(4, 1)-->
<!--)-->
<!--simulate(fork_seq, time=50) %>%-->
<!--  print() %>%-->
<!--  philosophers_gantt() + theme_bw()-->
<!--```-->

Our implementation follows the solution originally proposed by Dijkstra, which establishes the convention that all resources must be requested in order. This means that, in our simulation, Aristotle should pick fork 1 first instead. Without that convention, the simulation would stop soon at a point in which every philosopher holds one fork and waits for the other to be available.

<!--```{r, message=FALSE}-->
<!--fork_seq$Aristotle <- rev(fork_seq$Aristotle)-->
<!--simulate(fork_seq, time=50) %>%-->
<!--  print() %>%-->
<!--  philosophers_gantt() + theme_bw()-->
<!--```-->

Finally, we can transform the resulting [monitoring](../monitors.md) data with [`krangl`](http://holgerbrandl.github.io/krangl/) and visualize it with [`kravis`](https://github.com/holgerbrandl/kravis).

![](spaghetti_time.png){: .center}


See [here](https://github.com/holgerbrandl/kalasim/blob/master/simulations/notebooks/dining.ipynb) for a jupyter notebook implementation of this example.

This example was adopted from the [simmer manual](https://r-simmer.org/articles/simmer-08-philosophers.html).
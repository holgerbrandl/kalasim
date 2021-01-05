# Development Roadmap


## Next steps

https://github.com/denisvstepanov/lets-plot-examples

## v0.4



## v0.5

**{todo}** implement support for real-time simulations

**{todo}**  implement `Demo preemptive resource animated.py`

**{todo}** finish elevator example with visualization


**{todo}**  <https://r-simmer.org/articles/simmer-08-philosophers.html>


**{todo}** Add equivalent of simmer::select (See Ucar2019,p12) with multiple select policies `List<Resource>.select()`; Also detail out how DI for multiple elements by type (see https://stackoverflow.com/questions/54374067/how-to-retrieve-all-instances-that-matches-a-given-type-using-koin-dependency-in)

---

work out an example that is equivalent to simmer::batch (Ucar2019, p14); RollerCoasterManager with ComponentQueue?

```
roller <- trajectory() %>%
+ batch(10, timeout = 5, permanent = FALSE) %>%
+ seize("rollercoaster", 1) %>%
+ timeout(5) %>%
+ release("rollercoaster", 1) %>%
+ separate()
```


---
generator should monitor generation, from Ucar
>  Returns timing information per arrival: name of the arrival,
start_time, end_time, activity_time (time not spent in resource queues) and a flag,
finished, that indicates whether the arrival exhausted its activities (or was rejected)


---
**{todo}** add averaging resource usage (see AtmQueue.kt)

---
**{todo}** review simmer.plot
> Ucar I, Smeets B (2019b). simmer.plot: Plotting Methods for simmer. R package version 0.1.15, URL https://CRAN.R-project.org/package=simmer.plot

## v0.6

**{todo}** https://simpy.readthedocs.io/en/latest/examples/movie_renege.html

**{todo}** Cleanup ComponentGenerator API --> take out from until into own constructor

**{todo}**  better document/discuss intent of `standby`
* isn't it just like hold? What are the benefits?



**{todo}** pluggable backend for visualization functions
```
env.visEngine = KRAVIS
nlm.printHistogram()
```

**{todo}** consider a more simplistic process definition pattern
```kotlin
val c = Component(){
    hold(1)
}
```

**{todo}** restrict more methods in Component from being overridden

## Pre-release


**{todo}** update logs example in docs intro

**{todo}** review docs for https://holgerbrandl.github.io/kalasim/state/

**{todo}** port interesting examples from <https://simjuliajl.readthedocs.io/en/stable/examples/index.html>

**{todo}** port interesting simmer examples
* https://r-simmer.org/articles/simmer-08-philosophers.html

**{todo}** port machine repair example from  <https://github.com/matloff/des>

**{todo}** port interesting SimPy examples from https://simpy.readthedocs.io/en/latest/examples/index.html

**{todo}** port  machine maintenance example from ucar 2019


## Later

**{todo}** should we support a resource queue limit ` queue_size = Inf)` (as in simmer ucar209,p19)

---

Benchmark desim, salabim, simmer vs kalasim
* See Ucar2019  5.1 `Comparison with similar frameworks`

---

Slicing of monitors https://www.salabim.org/manual/Monitor.html#slicing-of-monitors

---

consider modeling `Store` Using Stores you can model the production and consumption of concrete objects as in <https://simpy.readthedocs.io/en/latest/topical_guides/resources.html#stores>

---

**{todo}** Remove distibution arguments ins `request`; Do we  really need
```
failAt: RealDistribution? = null,
failDelay: RealDistribution? = null,
```
and why the distribution can not be used at the call site?

---

Register project at awesome-kotlin


---
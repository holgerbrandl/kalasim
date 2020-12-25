# Development Roadmap


## Next steps

continue here
https://simpy.readthedocs.io/en/latest/topical_guides/monitoring.html

https://www.pythonpodcast.com/salabim-with-ruud-van-der-ham-episode-151/
* 15:08 https://www.scribd.com/listen/podcast/419047861

simmer https://github.com/r-simmer/simmer/
* in particular https://r-simmer.org/articles/simmer-08-philosophers.html


## v0.4

**{todo}** implement support for real-time simulations


**{todo}** implement `enabled` disabled for monitors


**{todo}** ensure that just yieldable methods are used in yield by checking stacktrace

**{todo}**  make `scheduledTime` nullable: replace scheduledTime = Double.MAX_VALUE with `null` which is semantically more meaningful here


**{todo}** info for envirroment that render similar to simmer (Ucar 2019, p4)
```
R> env %>%
+ add_resource("machine", 10) %>%
+ add_resource("operative", 5) %>%
+ add_generator("job", job, NEW_JOB) %>%
+ add_generator("task", task, NEW_TASK) %>%
+ run(until = 1000)
simmer environment: Job Shop | now: 1000 | next: 1000.11891377085
{ Monitor: in memory }
{ Resource: machine | monitored: TRUE | server status: 9(10) | queue... }
{ Resource: operative | monitored: TRUE | server status: 4(5) | queue... }
{ Source: job | monitored: 1 | n_generated: 4954 }
{ Source: task | monitored: 1 | n_generated: 1041 }
```

**{todo}** provide log trajectory filter (see Ucar2019, p10)

```
trajectory: anonymous, 3 activities
{ Activity: Timeout | delay: 10 }
{ Activity: Log | message: Leaving th..., level: 0 }
{ Activity: Timeout | delay: 10 }
```

## v0.5

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

## Pre-release


**{todo}** update logs example in docs intro

**{todo}** review docs for https://holgerbrandl.github.io/kalasim/state/

**{todo}** ingest

**{todo}** port interesting examples from <https://simjuliajl.readthedocs.io/en/stable/examples/index.html>


**{todo}** port interesing simmer examples
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

Review if request and wait should really need
```
failAt: RealDistribution? = null,
failDelay: RealDistribution? = null,
```
and why the distribution can not be used at the call site?

---

Register project at awesome-kotlin


---

pluggable backend for visualization functions
```
env.visEngine = KRAVIS
nlm.printHistogram()
```
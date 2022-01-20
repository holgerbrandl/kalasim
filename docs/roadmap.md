# Development Roadmap

## Next steps

**TODO** bug `State changes to 'WaitForStart' with trigger allow`

**TODO** ` Furnace.1             Waiting, scheduled for 31.00` for what?


## v0.8


**TODO** complete and publish hospital example
* https://www.sciencedirect.com/science/article/pii/S037722172101002X?dgcid=rss_sd_all

**TODO** add display option to discard zero-duration intervals for simplified vis

better work out https://www.kalasim.org/setup/ for novice users (e.g. use koans/or datalore share)

simple optimizing example

https://groups.google.com/g/salabim/c/iK6o7umeXKc/m/l3t59WbKBgAJ?utm_medium=email&utm_source=footer

---

**{todo}** finish elevator example with visualization

---
**TODO** pathfinding example

https://github.com/citiususc/hipster

https://www.baeldung.com/java-a-star-pathfinding

---
visualization

https://medium.com/@benjaminmbrown/real-time-data-visualization-with-d3-crossfilter-and-websockets-in-python-tutorial-dba5255e7f0e
* ktor backend for serving https://www.youtube.com/watch?v=wXpEKouOV3E&t=1926s&ab_channel=KotlinbyJetBrains


---

**TODO** add planning example from classical scheduling theory
parallel machines --> solve with or tools or optaplanner

> Other relevant applications arise in the context of health-care, where, for example, patients have to be assigned to surgery rooms that must be
equipped by considering the type (i.e., the family) of surgery to
be performed. In such cases, the weight usually models a level of
urgency for the patient.

---
## Later

better api for time distrubtions: `normal(tbfDays.days.asTicks(), 2.days.asTicks()) ` should alllow to sample `kotlin.time.Duration` directly

--

**TODO** consider removing supported for untyped durations in hold etc. The user could be forced to simply provide a tick-duration when creating any sim, and would used typed durations always! This is likely to remove quite some errors

---

**TODO** automatically render process diagrams 
* gant-charts over time-->display extension

* dependency who is requesting from whom, with link-weight iniating how often


---

**TODO** try to establish better default names by using similar concept as in `dataframe` 

**TODO** nice pedestrian simulation with heatmap

**TODO** for branch vis, we could use a similar technique as in https://www.supplychaindataanalytics.com/animated-monte-carlo-simulation-with-gganimate-in-r/

**TODO** work out logistics example similar to <https://www.supplychaindataanalytics.com/monte-carlo-simulation-in-r-for-warehouse-location-risk-assessment/>

**{todo}** review `simjulia` <https://simjuliajl.readthedocs.io/en/stable/topical_guides/5_shared_resources.html#containers>

**{todo}** should we support a resource queue limit ` queue_size = Inf)` (as in simmer ucar209,p19)

**{todo}** Benchmark `desim`, `salabim`, `simmer` vs `kalasim`
* See Ucar2019  5.1 `Comparison with similar frameworks`

---
**{todo}** lambda builder for interrupt/resume

---

Slicing of monitors <https://www.salabim.org/manual/Monitor.html#slicing-of-monitors>

---

consider modeling `Store` Using Stores you can model the production and consumption of concrete objects as in <https://simpy.readthedocs.io/en/latest/topical_guides/resources.html#stores>

---

**{todo}** restrict more methods in Component from being overridden


---

fix <https://github.com/holgerbrandl/kravis/issues/25>

---

also adjust random generator of kotlin to prevent non-determinism entering simulation when user is doing `listOf().random()`

---

**TODO** consider using processcontext across the api

---
Explore scalalabe queues https://chronicle.software/open-hft/queue/ & https://dzone.com/articles/java-creating-terabyte-sized-queues-with-low-laten-1

## More Examples

**{todo}** port interesting examples from <https://simjuliajl.readthedocs.io/en/stable/examples/index.html>

**{todo}** port other interesting simmer examples

**{todo}** port machine repair example from  <https://github.com/matloff/des>

**{todo}** port interesting SimPy examples from <https://simpy.readthedocs.io/en/latest/examples/index.html>

**{todo}** port  machine maintenance example from ucar 2019
![](.roadmap_images/2bad897b.png)

https://www.sciencedirect.com/science/article/abs/pii/S0377221721010894?dgcid=rss_sd_all

## Misc 

state-machine-like https://github.com/fraktalio/fmodel

consider using delegate for state variables https://medium.com/mobile-app-development-publication/how-kotlin-by-variable-delegate-helps-me-avoid-anti-pattern-558004000341
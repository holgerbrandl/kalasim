# Development Roadmap


## Next steps

<https://github.com/denisvstepanov/lets-plot-examples>


## v0.6

**{fixme}** fix  `val x = Component()` to create a DATA component (as stated in the docs)
* this will also fix the `Passenger` in the ferryman
* https://stackoverflow.com/questions/30812204/i-need-to-check-if-a-method-is-overridden
* it should be `DATA` until process is overridden or pointing to a different method

**{todo}** add averaging resource usage (see AtmQueue.kt)

**{todo}** hide `anonymous` in resources

**{todo}** Cleanup ComponentGenerator API --> take out from until into own constructor

**{todo}**  better document/discuss intent of `standby`
* isn't it just like hold? What are the benefits?

**{todo}** pluggable backend for visualization functions
```
env.visEngine = KRAVIS
nlm.printHistogram()
```

**{todo}** review simmer.plot and provide similar API
> Ucar I, Smeets B (2019b). simmer.plot: Plotting Methods for simmer. R package version 0.1.15, URL <https://CRAN.R-project.org/package=simmer.plot>

**{todo}** it should track renege stats in resource directly

**{todo}** restrict more methods in Component from being overridden

**{todo}** Register project at awesome-kotlin


## v0.7

**{todo}** build visualization for covid19 simulation

**{todo}** review simjulia <https://simjuliajl.readthedocs.io/en/stable/topical_guides/5_shared_resources.html#containers>

**{todo}** finish elevator example with visualization

**{todo}** consider if ComponentQueue should support all types (and not just Component)

**{todo}** wait lambda should use state.value as receiver

**{todo}** lambda builder for interrupt/release

**{todo}** continue inline predicates

## Later

**{todo}** find better way to draw random value without clutter `uniform(0,5)()` --> `uniform(0,5)`?!
**{todo}** use inline class for time, quantity, priorities and so so on

**{todo}** should we support a resource queue limit ` queue_size = Inf)` (as in simmer ucar209,p19)

**{todo}** Benchmark desim, salabim, simmer vs kalasim
* See Ucar2019  5.1 `Comparison with similar frameworks`

---

Slicing of monitors <https://www.salabim.org/manual/Monitor.html#slicing-of-monitors>

---

consider modeling `Store` Using Stores you can model the production and consumption of concrete objects as in <https://simpy.readthedocs.io/en/latest/topical_guides/resources.html#stores>

---



---

fix <https://github.com/holgerbrandl/kravis/issues/25>

---

also adjust random generator of kotlin to prevent non-determinism entering simulation when user is doing `listOf().random()`


## More Examples

**{todo}** port interesting examples from <https://simjuliajl.readthedocs.io/en/stable/examples/index.html>

**{todo}** port other interesting simmer examples

**{todo}** port machine repair example from  <https://github.com/matloff/des>

**{todo}** port interesting SimPy examples from <https://simpy.readthedocs.io/en/latest/examples/index.html>

**{todo}** port  machine maintenance example from ucar 2019
![](.roadmap_images/2bad897b.png)

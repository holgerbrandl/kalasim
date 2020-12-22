# Development Roadmap


## Next steps

continue here
https://simpy.readthedocs.io/en/latest/topical_guides/monitoring.html

https://www.pythonpodcast.com/salabim-with-ruud-van-der-ham-episode-151/
* 15:08 https://www.scribd.com/listen/podcast/419047861

simmer https://github.com/r-simmer/simmer/
* in particular https://r-simmer.org/articles/simmer-08-philosophers.html

https://cran.r-project.org/web/packages/DES/README.html

## v0.4

**{todo}** implement support for real-time simulations


**{todo}** implement `enabled` disabled for monitors


**{todo}** ensure that just yieldable methods are used in yield by checking stacktrace

**{todo}**  make `scheduledTime` nullable: replace scheduledTime = Double.MAX_VALUE with `null` which is semantically more meaningful here


## Pre-release


**{todo}** update logs example in docs intro

**{todo}** review docs for https://holgerbrandl.github.io/kalasim/state/

**{todo}** ingest

**{todo}** port interesting examples from <https://simjuliajl.readthedocs.io/en/stable/examples/index.html>


**{todo}** port interesing simmer examples
* https://r-simmer.org/articles/simmer-08-philosophers.html



**{todo}** port interesting SimPy examples from https://simpy.readthedocs.io/en/latest/examples/index.html

## Later


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
# simmer

* process-oriented and trajectory-based Discrete-Event Simulation (DES)
* Rcpp for performance
* ~ 1k DL/months
* nice article in 2019 in R Journal simmer_paper.pdf
* simmer exploits the concept of **trajectory**: a common path in the simulation model for entities of the same type.
* ships with a parameter optimization functions
* since 2014

>  two main elements: the simmer **environment** (or
simulation environment) and the **trajectory object**,

```r
traj0 <- trajectory() %>%
+ log_("Entering the trajectory") %>%
+ timeout(10) %>%
+ log_("Leaving the trajectory")
```
* parameters can also be function poiners (instead of 10)
* request/release are seize/release in simmer

extended example (from paper p11)
```r
 patient <- trajectory() %>%
   log_("arriving...") %>%
   seize(
   "doctor", 1, continue = c(TRUE, FALSE),
   post.seize = trajectory("accepted patient") %>%
   log_("doctor seized"),
   reject = trajectory("rejected patient") %>%
   log_("rejected!") %>%
   seize("nurse", 1) %>%
   log_("nurse seized") %>%
   timeout(2) %>%
   release("nurse", 1) %>%
   log_("nurse released")) %>%
   timeout(5) %>%
   release("doctor", 1) %>%
   log_("doctor released")
   
env <- simmer() %>%
  add_resource("doctor", capacity = 1, queue_size = 0) %>%
  add_resource("nurse", capacity = 10, queue_size = 0) %>%
  add_generator("patient", patient, at(0, 1)) %>%
  run()
```

<https://r-simmer.org/index.html>

<https://github.com/r-simmer/simmer>

## Batching

<https://r-simmer.org/reference/batch.html>

```r
pacman::p_load(simmer)

## unnamed batch with a timeout
traj <- trajectory() %>%
  log_("arrived") %>%
  batch(2, timeout=5) %>%
  log_("in a batch") %>%
  timeout(5) %>%
  separate() %>%
  log_("leaving")

simmer() %>%
  add_generator("dummy", traj, at(0:2)) %>%
  run() %>% invisible#> 0: dummy0: arrived
#> 1: dummy1: arrived
#> 1: batch0: in a batch
#> 2: dummy2: arrived
#> 6: dummy0: leaving
#> 6: dummy1: leaving
#> 7: batch1: in a batch
#> 12: dummy2: leaving


## batching based on some dynamic rule
traj <- trajectory() %>%
  log_("arrived") %>%
  # always FALSE -> no batches
  batch(2, rule=function() FALSE) %>%
  log_("not in a batch") %>%
  timeout(5) %>%
  separate() %>%
  log_("leaving")

simmer() %>%
  add_generator("dummy", traj, at(0:2)) %>%
  run() %>% invisible#> 0: dummy0: arrived
#> 0: dummy0: not in a batch
#> 1: dummy1: arrived
#> 1: dummy1: not in a batch
#> 2: dummy2: arrived
#> 2: dummy2: not in a batch
#> 5: dummy0: leaving
#> 6: dummy1: leaving
#> 7: dummy2: leaving
## named batch, shared across trajectories
traj0 <- trajectory() %>%
  log_("arrived traj0") %>%
  batch(2, name = "mybatch")

traj1 <- trajectory() %>%
  log_("arrived traj1") %>%
  timeout(1) %>%
  batch(2, name = "mybatch") %>%
  log_("in a batch") %>%
  timeout(2) %>%
  separate() %>%
  log_("leaving traj1")

simmer() %>%
  add_generator("dummy0", traj0, at(0)) %>%
  add_generator("dummy1", traj1, at(0)) %>%
  run() %>% invisible#> 0: dummy00: arrived traj0
#> 0: dummy10: arrived traj1
#> 1: batch_mybatch: in a batch
#> 3: dummy00: leaving traj1
#> 3: dummy10: leaving traj1

```

## Optimization

**{todo}** <https://github.com/r-simmer/simmer.optim>


## simmer.plot


https://github.com/r-simmer/simmer.plot

methods overview <https://r-simmer.org/extensions/plot/reference/>

review simmer.plot and provide similar API
> Ucar I, Smeets B (2019b). simmer.plot: Plotting Methods for simmer. R package version 0.1.15, URL <https://CRAN.R-project.org/package=simmer.plot>


plot(<simmer>) --> deprecated

Plot Method for simmer Objects

plot(<trajectory>) --> simple flow chart (we can do that I think)

plot(<arrivals>) 
plot(<attributes>)
plot(<resources>)

Plot Methods for simmer Monitoring Statistic 

From <https://r-simmer.org/extensions/plot/reference/plot.mon.html>

see `simmer_plot.R`

# R Alternatives

### DES
<https://cran.r-project.org/web/packages/DES/README.html>
* example sources https://github.com/matloff/des/blob/master/inst/examples/MachRep.R

* event-oriented approach, which means the programmer codes how the system reacts to any specific event.
* 4 github stars
* similar event-loop paradigm `mainloop(simlist)`
* `mrpreact` -> <https://github.com/matloff/des#user-suppplied-reaction-function-and-package-function-mainloop>





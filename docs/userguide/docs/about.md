# About


## License

`kalasim` is licensed under MIT License.


## Acknowledgements


### salabim

`kalasim` started off as a blunt rewrite of [salabim](https://www.salabim.org/). and we are deeply thankful for its permissive licence that enabled setting up this project. A great starting point was in particular the wonderful article [salabim: discrete event simulation and animation in Python](https://www.semanticscholar.org/paper/salabim%3A-discrete-event-simulation-and-animation-in-Ham/b513ce3d7cd56c478bb045d7080f7e34c0eb20de).

`salabims`s excellent documentation and wonderful examples made this project possible after all. `kalasim` reimplements all core APIs of `salabim` in a more typesafe API while also providing better test coverage, real-time capabilities and (arguably) more modern built-in support for visualization.

* [SimPy](https://simpy.readthedocs.io/) inspired the development of `salabim`. The latter is more opinionated, provides a more complete ecosystem for simulation, ships with visualization, and a more consistent process interaction model
* [Salabim, Discrete Event Simulation In Python](https://www.youtube.com/watch?v=I74j2KtGouA) - PyCon 2018 Talk
* [Python.__init__ Podcast: Salabim](https://www.pythonpodcast.com/salabim-with-ruud-van-der-ham-episode-151/) - Great podcast episode with [Ruud van der Ham](https://www.linkedin.com/in/ruudvanderham/)

### SimJulia

[SimJulia](https://simjuliajl.readthedocs.io/en/stable/welcome.html) is a combined continuous time / discrete event process oriented simulation framework written in Julia inspired by the Simula library DISCO and the Python library SimPy.

### DSOL

[DSOL3](https://simulation.tudelft.nl/simulation/index.php/dsol) which is an open source, Java based suite of Java classes for continuous and discrete event simulation

* The wonderful [DSOL manual](https://simulation.tudelft.nl/dsol/manual/)
* [The DSOL simulation suite - Enabling multi-formalism simulation in a distributed context](https://simulation.tudelft.nl/files/dissertations/tpm_jacobs_20051115.pdf), PhD Thesis, Peter Jacobs, 2005
* [Mastering D-SOL: A Java based suite for simulation](https://www.researchgate.net/publication/228941076_Mastering_D-SOL_A_Java_based_suite_for_simulation) with several examples, 2006
* [opentrafficsim](https://opentrafficsim.org/manual/) is a traffic simulation built with DSOL3


### simmer

[simmer](https://r-simmer.org/) is a process-oriented and trajectory-based Discrete-Event Simulation (DES) package for R.

It centres around the concept of a *trajectory* that defines a component lifecycle. To enable scale it is built on top of Rcpp (C++ backend for R)

<!--```r-->
<!--traj <- trajectory() %>%-->
<!-- log_("Entering the trajectory") %>%-->
<!-- timeout(10) %>%-->
<!-- log_("Leaving the trajectory")-->
<!--```-->

* Great overview [simmer: Discrete-Event Simulation for R](https://www.jstatsoft.org/article/view/v090i02), Ucar et al., 2019
* Support for optimization in [simmer.optim](https://github.com/r-simmer/simmer.optim)


### Other discrete simulation engines

*  <https://github.com/aybabtme/desim> - Discrete event simulation framework written in GO that implements a similar API as `kalasim`
*  [SimPy](https://simpy.readthedocs.io/en/latest/index.html)is a process-based discrete-event simulation framework based on standard Python. Processes in SimPy are defined by Python generator functions. SimPy also provides various types of shared resources to model limited capacity congestion points (like servers, checkout counters and tunnels).


### Libraries used to build kalasim

`kalasim`  is built on top of some great libraries. It was derived as merger of ideas, implementation and documentation from the following projects

* [Kotlin](https://kotlinlang.org/) - Not really a library, but for obvious reasons the foundation of this project
* [koin](https://github.com/InsertKoinIO/koin) which is a pragmatic lightweight dependency injection framework for Kotlin developers
* [Apache Commons Math](https://commons.apache.org/proper/commons-math/) is a library of lightweight, self-contained mathematics and statistics components
* [jsonbuilder](https://github.com/holgerbrandl/jsonbuilder) is a small artifact that serves a single purpose: It allows creating json using an idiomatic kotlin DSL. Its main purpose it to make sure that `kalasim` provides a machine readable log-format for all [basics](basics.md) in a simulation.
* [kotest.io](http://kotest.io/) is a flexible and elegant multiplatform test framework, assertions library, and property test library for Kotlin. We use it to make sure kalasim fulfils its [component](component.md) contract.

Visualization

* <https://github.com/holgerbrandl/kravis> which implements a grammar to create a wide range of plots using a standardized set of verbs


## Repo Maintainer

[Holger Brandl](https://linkedin.com/in/holgerbrandl/) holds a Ph.D. degree in machine learning and has developed new concepts in the field of computational linguistics. More recently he has co-authored [publications](https://orcid.org/0000-0003-1911-8570) in high-ranking journals such as Nature and Science.

To stay in sync with what's happening in tech, he's [developing](https://github.com/holgerbrandl) open-source tools, methods and algorithms for bioinformatics, high-performance computing and data science. He's passionate about data science, machine learning, kotlin, R, elegant APIs and data visualisation in particular in relations applications from systems biology and industrial manufacturing.

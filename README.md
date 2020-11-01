# kalasim

Discrete Event Simulator

[ ![Download](https://api.bintray.com/packages/holgerbrandl/github/kalasim/images/download.svg) ](https://bintray.com/holgerbrandl/github/kalasim/_latestVersion)  [![Build Status](https://travis-ci.org/holgerbrandl/kalasim.svg?branch=master)](https://travis-ci.org/holgerbrandl/kalasim) [![Gitter](https://badges.gitter.im/kalasim.svg)](https://gitter.im/kalasim/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)


`kalasim` is a discrete event simulator with type support, dependency injection, modern persistence and logging to enable large-scale, industrial-ready simulations.


`kalasim` started off as a blunt rewrite of [salabim](https://www.salabim.org/). `salabim` is written in python and provides a great model to built simulations. `kalasim` reimplements all core APIs of `salabim` in a more typesafe API while providing better test coverage, real-time capabilities and (arguably) more modern built-in support for visualization.


`kalsim` is written in [Kotlin](https://kotlinlang.org/), designed on top of [koin](https://github.com/InsertKoinIO/koin) as dependency injection framework, is using [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for event tracking and persistence, and [plotly.kt](https://github.com/mipt-npm/plotly.kt) as well as [`kravis`](https://github.com/holgerbrandl/kravis) for visualization.

In contrast to many other simulation tools, `kalasim` is neither low-code nor no-code. It is _just-code_ to enable change tracking, scaling, refactoring, CI/CD, unit-tests, and the rest of the gang that makes development fun.


## Documentation

### >> [kalasim user guide](https://holgerbrandl.github.io/kalasim) <<


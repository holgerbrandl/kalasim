# kalasim

Discrete Event Simulator

[ ![Download](https://img.shields.io/github/v/release/holgerbrandl/kalasim) ](https://github.com/holgerbrandl/kalasim/releases) [![Build Status](https://github.com/holgerbrandl/kalasim/workflows/build/badge.svg)](https://github.com/holgerbrandl/kalasim/actions?query=workflow%3Abuild)  [![Gitter](https://badges.gitter.im/kalasim.svg)](https://gitter.im/holgerbrandl/kalasim) [![slack](https://img.shields.io/badge/kotlinlang%20slack-kalasim-yellowgreen)](https://kotlinlang.slack.com/messages/kalasim/)


`kalasim` is a discrete event simulator with type support, dependency injection, modern persistence and logging to enable large-scale, industrial-ready simulations.


`kalasim` is written in [Kotlin](https://kotlinlang.org/), designed on top of [koin](https://github.com/InsertKoinIO/koin) as dependency injection framework, and is using [common-math](https://commons.apache.org/proper/commons-math/) for stats and distributions, modern event tracking and persistence, and [plotly.kt](https://github.com/mipt-npm/plotly.kt), [lets-plot](https://github.com/JetBrains/lets-plot-kotlin) as well as [`kravis`](https://github.com/holgerbrandl/kravis) for visualization.

In contrast to many other simulation tools, `kalasim` is neither low-code nor no-code. It is _code-first_ to enable change tracking, scaling, refactoring, CI/CD, unit-tests, and the rest of the gang that makes simulation development fun.


## Documentation

<!--### >> [kalasim user guide](https://holgerbrandl.github.io/kalasim) <<-->
All docs are hosted under <http://www.kalasim.org/>


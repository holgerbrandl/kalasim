# Visualization 

There are two type of visualizations

* **Statistical plots** to inspect distributions, trends and outliers. That's what described in this chapter
* **Process rendering** to actually show simulation entities, their state or position changes on a 2D (or even 3D) grid as rendered movie. This may also involve interactive controls to adjust simulation parameters. Such functionality is planned but not yet implemented in `kalasim`

Examples
* [Movie Theater](examples/movie_theater.md)


## Built-in Visualizations

Currently, the following extensions for distribution analysis are supported

[Components](component.md)



[Monitors](monitors.md)

* `CategoryTimeline<T>.display()` provides a segment chart of the level
* `FrequencyTable<T>.display()` provides a barchart of the frequencies of the different values 
* `NumericStatisticMonitor.display()` provides histogram of the underlying distribution
* `MetricTimeline.display()` provides a line chart with time on the x and the value on y


[Resources](resource.md)

* `r.activiities` to show the activities as segments timeline
* `r.timeline` to show the resource utilization and queuing status 
* All monitor related plots from above


[Component Queue](collections.md#queue)

* All monitor related plots from above



For [monitors](monitors.md), see corresponding [section](monitors.md#visualization)

## Framework Support

By default, `kalasim` supports 2 pluggable visualization backends. Currently [kravis](https://github.com/holgerbrandl/kravis) and [lets-plot](https://github.com/JetBrains/lets-plot-kotlin) are supported.

Since we may not be able to support all visualizations in both frontends, the user can simply toggle the frontend by package import:

```kotlin
// simply toggle backend by package import
import org.kalasim.plot.letsplot.display
// or
//import org.kalasim.plot.kravis.display

MM1Queue().apply {
    run(100)
    server.claimedMonitor.display()
}
```

### Kravis

`kalasim` integrates nicely with [`kravis`](https://github.com/holgerbrandl/kravis) to visualize monitor data. For examples see `src/test/kotlin/org/kalasim/analytics/KravisVis.kt`.

!!! note
    To visualize data with kravis, [R](https://www.r-project.org/) must be installed on the system. See [here](https://github.com/holgerbrandl/kravis)) for details.

### LetsPlot

[lets-plot](https://github.com/JetBrains/lets-plot-kotlin) is another very modern visualization library that renders within the JVM and thus does not have any external dependencies. Similar to `kravis` it mimics the API of [ggplot2](https://ggplot2.tidyverse.org/).


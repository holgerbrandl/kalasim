# Visualization 

There are two type of visualizations

* **Statistical plots** to inspect distributions, trends and outliers. That's what described in this chapter
* **Process rendering** to actually show simulation entities, their state or position changes on a 2D (or even 3D) grid as rendered movie. This may also involve interactive controls to adjust simulation parameters. Such functinoality is planned but not yet implemented in `kalasim`

Examples
* [Movie Theater](examples/movie_theater.md)


To structure this chapter we present visualizations grouped by entity or function

## Framework Support

### Kravis

`kalasim` integrates nicely with [`kravis`](https://github.com/holgerbrandl/kravis) to visualize monitor data. For examples see `src/test/kotlin/org/kalasim/analytics/KravisVis.kt`.

 **Prerequisite**
 > To visualize data with kravis, [R](https://www.r-project.org/) must be installed on the system. See [here](https://github.com/holgerbrandl/kravis)) for details.

### LetsPlot

**{tbd}** 


## Visualization


### Monitors

See corresponding [chapter](monitors.md#visualization)


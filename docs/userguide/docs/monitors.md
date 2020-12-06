# Monitor

Monitors are a way to collect data from the simulation. They are automatically collected
for resources, queues and states. On top of that the user can define its own monitors.

Monitors can be used to get statistics and as a feed for graphical tools.

There are two types of monitors:

* **Level monitors** are useful to collect data about a variable that keeps its value over a certain length
  of time, such as the length of a queue orcthe colour of a traffic light.
* **Non level monitors** are useful to collect data about a values that occur just once. Examples, are the length of stay in a queue and
  the number of processing steps of a part.

For both types, the time is always collected, along with the value.

Monitors support a wide range of statistical properties via `m.statistics()` including

* mean
* median
* percentiles
* min and max
* standard deviation
* histograms


For all these statistics, it is possible to exclude zero entries,
e.g. `m.statistics(statistics=true)` returns the mean, excluding zero entries.

Monitors can be enabled or disabled by setting the boolean flag `m.enabled`.

```kotlin
m.enabled = false  // disable monitoring
m.enabled = true   // enable monitoring
```
## Non level monitors

Non level monitors collects values which do not reflect a level, e.g. the processing time of a part.

There are  2 implementations to support categorical and numerical attributes

* `org.kalasim.NumericStatisticMonitor`
* `org.kalasim.FrequencyMonitor`


Besides, it is possible to get all collected values as list with `m.statistics().values`.


Calling `m.reset()` will clear all tallied values.

The statistics of a monitor can be printed with `printStatistics()`.
E.g: `waitingLine.lengthOfStayMonitor.printStatistics()`:

```json
{
    "all": {
      "entries": 5,
      "ninty_pct_quantile": 4.142020545932034,
      "median": 1.836,
      "mean": 1.211,
      "nintyfive_pct_quantile": 4.142020545932034,
      "standard_deviation": 1.836
    },
    "excl_zeros": {
      "entries": 2,
      "ninty_pct_quantile": 4.142020545932034,
      "median": 1.576,
      "mean": 3.027,
      "nintyfive_pct_quantile": 4.142020545932034,
      "standard_deviation": 1.576
    }
}
```

And, a histogram can be printed with `print_histogram()`. E.g.
`waitingline.length_of_stay.print_histogram(30, 0, 10)`:


```Histogram of: 'Available quantity of fuel_pump'
              bin | entries |  pct |                                         
[146.45, 151.81]  |       1 |  .33 | *************                           
[151.81, 157.16]  |       0 |  .00 |                                         
[157.16, 162.52]  |       0 |  .00 |                                         
[162.52, 167.87]  |       0 |  .00 |                                         
[167.87, 173.23]  |       1 |  .33 | *************                           
[173.23, 178.58]  |       0 |  .00 |                                         
[178.58, 183.94]  |       0 |  .00 |                                         
[183.94, 189.29]  |       0 |  .00 |                                         
[189.29, 194.65]  |       0 |  .00 |                                         
[194.65, 200.00]  |       1 |  .33 | *************    
```

If neither `binCount`, nor `lowerBound` nor `upperBound` are specified, the histogram will be autoscaled.

Histograms can be printed with their values, instead of bins. This is particularly useful for non
numeric tallied values, such as names::

```kotlin
val m = FrequencyMonitor<Car>()

m.addValue(AUDI)
m.addValue(AUDI)
m.addValue(VW)
repeat(4) { m. addValue(PORSCHE)}

m.printHistogram()
```

The output of this:

```
Summary of: 'FrequencyMonitor.2'
# Records: 7
# Levels: 3

Histogram of: 'FrequencyMonitor.2'
              bin | entries |  pct |                                         
AUDI              |       2 |  .29 | ***********                             
VW                |       1 |  .14 | ******                                  
PORSCHE           |       4 |  .57 | ***********************           
```


It is also possible to specify the values to be shown:

```
m.printHistogram(values = listOf(AUDI, TOYOTA)) 
```

This results in a further aggregated histogram view where non-selected values are agregated and listes values are forced in the display even if they were not observed.

```
Summary of: 'FrequencyMonitor.1'
# Records: 7
# Levels: 3

Histogram of: 'FrequencyMonitor.1'
              bin | entries |  pct |                                         
AUDI              |       2 |  .29 | ***********                             
TOYOTA            |       0 |  .00 |                                         
rest              |       5 |  .71 | *****************************
```

It is also possible to sort the histogram on the weight (or number of entries) of the value:

```
m.printHistogram(sortByWeight = true)
```

The output of this:

```
Summary of: 'FrequencyMonitor.1'
# Records: 7
# Levels: 3

Histogram of: 'FrequencyMonitor.1'
              bin | entries |  pct |                                         
PORSCHE           |       4 |  .57 | ***********************                 
AUDI              |       2 |  .29 | ***********                             
VW                |       1 |  .14 | ******
```

## Level monitor

Level monitors tally levels along with the current (simulation) time. E.g. the number of parts a machine is working on.

There are  2 implementations to support categorical and numerical attributes

* `org.kalasim.FrequencyLevelMonitor`
* `org.kalasim.NumericLevelMonitor`


Level monitors allow to query the value at a specific time
```
val nlm = NumericLevelMonitor()
// ... collecting some data ...
nlm[4]  # will print the value at time 4
```

In addition to standard statistics, level monitor support the following statistics

* duration

For all statistics, it is possible to exclude zero entries, e.g. `m.statistics(excludeZeros=true).mean` returns the mean, excluding zero entries.


**{todo}** implement off tallying
<!--When monitoring is disabled, an off value (see table above) will be tallied. All statistics will ignore the periods from this-->
<!--off to a non-off value. This also holds for the xduration() method, but NOT for xt() and tx(). Thus,-->
<!--the x-arrays of xduration() are not necessarily the same as the x-arrays in xt() and tx(). This is-->
<!--the reason why there's no x() or t() method. |n|-->
<!--It is easy to get just the x-array with `xduration()[0]` or `xt()[0]`.-->

<!--It is important that a user *never* tallies an off value! Instead use Monitor.monitor(False)-->

<!--With the monitor method, a level monitor can be enbled or disabled.-->

<!--Also, the current monitor status (enabled/disabled) can be retrieved.-->

Calling `m.reset()` will clear all tallied values and timestamps.

The statistics of a level monitor can be printed with `m.printStatistics()`.

##  Merging of monitors

Monitors can be merged, to create a new monitor, nearly always to collect aggregated data.

The method Monitor.merge() is used for that, like:

```
mc = m0.merge(m1, m2)
```

Then we can just get the mean of the monitors m0, m1 and m2 combined by:

```
mc.mean()
```

,but also directly with :

```
m0.merge(m1, m2).mean()
```

Alternatively, monitors can be merged with the + operator, like:

```
mc = m0 + m1 + m2  
```

And then get the mean of the aggregated monitors with:

```
mc.mean()
```

, but also with

```
(m0 + m1 + m2).mean()
```

It is also possible to use the sum function to merge a number of monitors. So:

```
print(sum((m0, m1, m2)).mean())
```

Finally, if ms = (m0, m1, m2), it is also possible to use:

```
print(sum(ms).mean())
```

A practical example of this is the case where the list waitinglines contains a number of queues.

Then to get the aggregated statistics of the length of all these queues, use:

```
sum(waitingline.length for waitingline in waitinglines).print_statistics()
```


For non level monitors, all of the tallied x-values are copied from the to be merged monitors.
For level monitors, the x-values are summed, for all the periods where all the monitors were on.
Periods where one or more monitors were disabled, are excluded.
Note that the merge only takes place at creation of the (timestamped) monitor and not dynamically later.

Sample usage:

Suppose we have three types of products (a, b, c) and that each have a queue for processing, so
a.processing, b.processing, c.processing.
If we want to print the histogram of the combined (=summed) length of these queues:

```
a.processing.length.merge(b.processing.length, c.processing.length, name='combined processing length')).print_histogram()
```

and to get the minimum of the length_of_stay for all queues:

```
(a.processing.length_of_stay + b.processing.length_of_stay + c.processing.length_of_stay).minimum()
```

Note that it is possible to rename a merged monitor (particularly those created with + or sum) with the rename() method::

```
sum(waitingline.length for waitingline in waitinglines).rename('aggregated length of waitinglines').print_statistics()
```

Merged monitors are disabled and cannot be enabled again.

## Slicing of monitors

It is possible to slice a monitor with Monitor.slice(), which has two applications:

* to get statistics on a monitor with respect to a given time period, most likely a subrun
* to get statistics on a monitor with respect to a recurring time period, like hour 0-1, hour 0-2, etc.

Examples:
```
for i in range(10):
   start = i * 1000
   stop = (i+1) * 1000
   print(f'mean length of q in [{start},{stop})={q.length.slice(start,stop).mean()}'
   print(f'mean length of stay in [{start},{stop})={q.length_of_stay.slice(start,stop).mean()}'

for i in range(24):
   print(f'mean length of q in hour {i}={q.length.slice(i, i+1, 24).mean()}'
   print(f'mean length of stay of q in hour {i}={q.length_of_stay.slice(i, i+1, 24).mean()}'
```

Instead of slice(), a monitor can be sliced as well with the standard slice operator [], like:

```
q.length[1000:2000].print_histogram()
q.length[2:3:24].print_histogram()
print(q.length[1000].mean())
```

Note that it is possible to rename a sliced monitor (particularly those created []) with the rename() method::

    waitingline.length[1000:2000].rename('length of waitingline between t=1000 and t-2000').print_statistics()   

Sliced monitors are disabled and cannot be enabled again.

## Using monitored values in other packages, like matplotlib

For high quality, reproduction ready, graphs, it can be useful to use additional packages, most notably matplotlib.

The sampled values from a non level monitor can be retrieved with Monitor.x(). If the moment of the sample is required as well, either Monitor.xt() or Monitor.tx() can be used.

For level monitors, there is choice of :

* Monitor.xt()
* Monitor.tx()
* Monitor.xduration()

To get a proper display of a level monitor, we advise something like:

```
plt.plot(*waitingline.length.tx(), drawstyle="steps-post") 
```

##  Pickling a monitor

Monitor.freeze() returns a 'frozen' monitor that can be used to store the results not
depending on the current environment.

This is particularly useful for pickling a monitor.

E.g. use:

```
with open("mon.pickle", "wb") as f:
    pickle.dump(f, mon.freeze())
```

to save the monitor mon, and:

```
with open("mon.pickle", "rb") as f:
    mon_retrieved = pickle.load(f)
```

to retrieve the monitor, later.

Both level and non level monitors are supported.
Frozen monitors get the name of the original monitor padded with '.frozen' unless specified differently.

For further background information see <https://www.salabim.org/manual/Monitor.html>

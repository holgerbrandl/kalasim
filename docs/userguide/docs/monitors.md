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


## Non level monitor

Non level monitors collects values which do not reflect a level, e.g. the processing time of a part.

We define the monitor with `processingtime = sim.Monitor('processingtime')` and then
collect values by `processingtime.tally(this_duration)`

By default, the collected values are stored in a list. Alternatively, it is possible to store
the values in an array of one of the following types:

========== =========== ==================== ==================== ===============
type       stored as   lowerbound           upperbound           number of bytes
========== =========== ==================== ==================== ===============
'any'      list        N/A                  N/A                  depends on data
'bool'     integer     False                True                 1
'int8'     integer     -128                 127                  1
'uint8'    integer     0                    255                  1
'int16'    integer     -32768               32767                2
'uint16'   integer     0                    65535                2
'int32'    integer     2147483648           2147483647           4
'uint32'   integer     0                    4294967295           4
'int64'    integer     -9223372036854775808 9223372036854775807  8
'uint64'   integer     0                    18446744073709551615 8
'float'    float       -inf                 inf                  8
========== =========== ==================== ==================== ===============

Monitoring with arrays takes up less space. Particularly when tallying a large
number of values, this is strongly advised.

Note that if non numeric values are stored (only possible with the default setting ('any')),
a tallied value is converted, if required, to a numeric value if possible, or 0 if not.

It is possible to use monitors with weighted data. In that case, just add a second parameter to tally, which defaults to 1.  
All statistics will take the weights into account.

There is set of statistical data available, which will be all weighed according to the tallied weights (1 by default):

* number_of_entries
* number_of_entries_zero
* weight
* weight_zero
* mean
* std
* minimum
* median
* maximum
* percentile
* bin_number_of_entries (number of entries between two given values)
* bin_weight (total weight of entries between two given values)
* value_number_of_entries (number of entries equal to a given value or set of values)
* value_weight (total weight of entries equal to a given value or set of values)

For all these statistics, it is possible to exclude zero entries,
e.g. `m.mean(ex0=True)` returns the mean, excluding zero entries.

Besides, it is possible to get all collected values as an array with x(). In the case of 'any' monitors,
the values might have to be converted. By specifying `force_numeric=False` the collected values will be returned as stored.

With the monitor method, the monitor can be enbled or disabled. Note that a tally is just ignored when
the monitor is disabled.

Also, the current monitor status (enabled/disabled) can be retrieved.

```
proctime.monitor(False)  # disable monitoring
proctime.monitor(True)  # enable monitoring
if proctime.monitor():
    print('proctime is enabled')
```

Calling m.reset() will clear all tallied values.

The statistics of a monitor can be printed with `print_statistics()`.
E.g: `waitingline.length_of_stay.print_statistics()`:

```
Statistics of Length of stay in waitingline at     50000    
                        all    excl.zero         zero
-------------- ------------ ------------ ------------
entries            4995         4933           62    
mean                 84.345       85.405
std.deviation        48.309       47.672

minimum               0            0.006
median               94.843       95.411
90% percentile      142.751      142.975
95% percentile      157.467      157.611
maximum             202.153      202.153    
```

And, a histogram can be printed with `print_histogram()`. E.g.
`waitingline.length_of_stay.print_histogram(30, 0, 10)`:


```
Histogram of Length of stay in waitingline
                        all    excl.zero         zero
-------------- ------------ ------------ ------------
entries            4995         4933           62    
mean                 84.345       85.405
std.deviation        48.309       47.672

minimum               0            0.006
median               94.843       95.411
90% percentile      142.751      142.975
95% percentile      157.467      157.611
maximum             202.153      202.153

           &lt;=       entries     %  cum%
        0            62       1.2   1.2 |                                                                              
       10           169       3.4   4.6 ** |                                                                           
       20           284       5.7  10.3 ****    |                                                                      
       30           424       8.5  18.8 ******         |                                                               
       40           372       7.4  26.2 *****               |                                                          
       50           296       5.9  32.2 ****                     |                                                     
       60           231       4.6  36.8 ***                          |                                                 
       70           192       3.8  40.6 ***                             |                                              
       80           188       3.8  44.4 ***                                |                                           
       90           136       2.7  47.1 **                                   |                                         
      100           352       7.0  54.2 *****                                      |                                   
      110           491       9.8  64.0 *******                                            |                           
      120           414       8.3  72.3 ******                                                   |                     
      130           467       9.3  81.6 *******                                                          |             
      140           351       7.0  88.7 *****                                                                 |        
      150           224       4.5  93.2 ***                                                                       |    
      160           127       2.5  95.7 **                                                                          |  
      170            67       1.3  97.0 *                                                                            | 
      180            59       1.2  98.2                                                                               |
      190            61       1.2  99.4                                                                                |
      200            24       0.5  99.9                                                                                |
      210             4       0.1 100                                                                                   |
      220             0       0   100                                                                                   |
      230             0       0   100                                                                                   |
      240             0       0   100                                                                                   |
      250             0       0   100                                                                                   |
      260             0       0   100                                                                                   |
      270             0       0   100                                                                                   |
      280             0       0   100                                                                                   |
      290             0       0   100                                                                                   |
      300             0       0   100                                                                                   |
          inf         0       0   100     
```

If neither number_of_bins, nor lowerbound nor bin_width are specified, the histogram will be autoscaled.

Histograms can be printed with their values, instead of bins. This is particularly useful for non
numeric tallied values, such as names::

```
import salabim as sim

env = sim.Environment()

monitor_names= sim.Monitor(name='names')
for _ in range(10000):
    name = sim.Pdf(('John', 30, 'Peter', 20, 'Mike', 20, 'Andrew', 20, 'Ruud', 5, 'Jan', 5)).sample()
    monitor_names.tally(name)

monitor_names.print_histogram(values=True)
```

The ouput of this:


```
Histogram of names
entries          10000    

value               entries
Andrew                 2031( 20.3%) ****************                                                                
Jan                     495(  5.0%) ***                                                                             
John                   2961( 29.6%) ***********************                                                         
Mike                   1989( 19.9%) ***************                                                                 
Peter                  2048( 20.5%) ****************                                                                
Ruud                    476(  4.8%) ***        
```


It is also possible to specify the values to be shown:

```
import salabim as sim

env = sim.Environment()

monitor_names= sim.Monitor(name='names')
for _ in range(10000):
    name = sim.Pdf(('John', 30, 'Peter', 20, 'Mike', 20, 'Andrew', 20, 'Ruud', 5, 'Jan', 5)).sample()
    monitor_names.tally(name)

monitor_names.print_histogram(values=('John', 'Andrew', 'Ruud', 'Fred'))
```

The output of this:

```
Histogram of names
entries          10000

value               entries     %
John                   2961  29.6 ***********************
Andrew                 2031  20.3 ****************
Ruud                    476   4.8 ***
Fred                      0   0
&lt;rest&gt;                 4532  45.3 ************************************
```

It is also possible to sort the histogram on the weight (or number of entries) of the value:

```
import salabim as sim

env = sim.Environment()

monitor_names= sim.Monitor(name='names')
for _ in range(10000):
    name = sim.Pdf(('John', 30, 'Peter', 20, 'Mike', 20, 'Andrew', 20, 'Ruud', 5, 'Jan', 5)).sample()
    monitor_names.tally(name)

monitor_names.print_histogram(values=True, sort_on_weight=True)
```

The output of this:

```
Histogram of names
entries          10000

value               entries     %
John                   2961  29.6 ***********************
Peter                  2048  20.5 ****************
Andrew                 2031  20.3 ****************
Mike                   1989  19.9 ***************
Jan                     495   5.0 ***
Ruud                    476   4.8 ***
```

##  Level monitor

Level monitors tally levels along with the current (simulation) time.
e.g. the number of parts a machine is working on.

A level monitor is defined by specifying `level=True` in the initialization of Monitor, e.g.:

```
working_on_parts = sim.Monitor(name='working_on_parts', level=True, initial_tally=0)
```

By default, the collected x-values are stored in a list. Alternatively, it is possible to store
the x-values in an array of one of the following types:

========== =========== ==================== ==================== =============== ====================
type       stored as   lowerbound           upperbound           number of bytes do not tally (=off)
========== =========== ==================== ==================== =============== ====================
'any'      list        N/A                  N/A                  depends on data N/A`
'bool'     integer     False                True                 1               255
'int8'     integer     -127                 127                  1               -128
'uint8'    integer     0                    254                  1               255
'int16'    integer     -32767               32767                2               -32768
'uint16'   integer     0                    65534                2               65535
'int32'    integer     2147483647           2147483647           4               2147483648
'uint32'   integer     0                    4294967294           4               4294967295
'int64'    integer     -9223372036854775807 9223372036854775807  8               -9223372036854775808
'uint64'   integer     0                    18446744073709551614 8               18446744073709551615
'float'    float       -inf                 inf                  8               -inf
========== =========== ==================== ==================== =============== ====================

Monitoring with arrays takes up less space. Particularly when tallying a large
number of values, this is strongly advised.

Note that if non numeric x-values are stored (only possible with the default setting ('any')),
the tallied values are converted, if required, to a numeric value if possible, or 0 if not.

During the simulation run, it is possible to retrieve the last tallied value (which represents the 'current' value)
by calling Monitor.get(). |n|
It's also possible to directly call the level monitor to get the current value, e.g.:

```
mylevel = sim.Monitor('level', level=True, initial_tally=0)
...
mylevel.tally(10)
yield seld.hold(1)
print(mylevel())  # will print 10
```

For the same reason, the standard length monitor of a queue can be used to get the current length of a queue: `q.length()` although
the more Pythonic `len(q)` is prefered.

When Monitor.get() is called with a time parameter or a direct call with a time parameter, the value at that time will be returned:

```
print (mylevel.get(4))  # will print the value at time 4
print (mylevel(4))  # will print the value at time 4
```

There is set of statistical data available, which are all weighted with their duration:

* duration
* duration_zero (time that the value was zero)
* mean
* std
* minimum
* median
* maximum
* percentile
* bin_duration (total duration of entries between two given values)
* value_duration (total duration of entries equal to a given value or set of values)

For all these statistics, it is possible to exclude zero entries, e.g. `m.mean(ex0=True)` returns the mean, excluding zero entries.

The individual x-values and their duration can be retrieved xduration(). By default, the x-values will be returned as an array, even if
the type is 'any'. In case the type is 'any' (stored as a list), the tallied x-values will be converted to a numeric value or 0 if
that's not possible. By specifying `force_numeric=False` the collected x-values will be returned as stored.

The individual x-values and the associated timestamps can be retrieved with xt() or tx(). By default, the x-values will be returned as an array, even if
the type is 'any'. In case the type is 'any' (stored as a list), the tallied x-values will be converted to a numeric value or 0 if
that's not possible. By specifying `force_numeric=False` the collected x-values will be returned as stored.

When monitoring is disabled, an off value (see table above) will be tallied. All statistics will ignore the periods from this
off to a non-off value. This also holds for the xduration() method, but NOT for xt() and tx(). Thus,
the x-arrays of xduration() are not necessarily the same as the x-arrays in xt() and tx(). This is
the reason why there's no x() or t() method. |n|
It is easy to get just the x-array with xduration()[0] or xt()[0].

It is important that a user *never* tallies an off value! Instead use Monitor.monitor(False)

With the monitor method, a level monitor can be enbled or disabled.

Also, the current monitor status (enabled/disabled) can be retrieved.

```
mylevel.monitor(False)  # disable monitoring
mylevel.monitor(True)  # enable monitoring
if mylevel.monitor():
    print('level is enabled')
```

It is strongly advised to keep tallying even when monitoring is off, in order to be able to access the current value at any time. The values tallied when monitoring is off
are not stored.

Calling m.reset() will clear all tallied values and timestamps.

The statistics of a level monitor can be printed with `print_statistics()`.
E.g: `waitingline.length.print_statistics()`:


```
Statistics of Length of waitingline at     50000    
                        all    excl.zero         zero
-------------- ------------ ------------ ------------
duration          50000        48499.381     1500.619
mean                  8.427        8.687
std.deviation         4.852        4.691

minimum               0            1    
median                9           10    
90% percentile       14           14    
95% percentile       16           16    
maximum              21           21    
```

And, a histogram can be printed with `print_histogram()`. E.g.:

```
waitingline.length.print_histogram(30, 0, 1)
```

```
Histogram of Length of waitingline
                        all    excl.zero         zero
-------------- ------------ ------------ ------------
duration          50000        48499.381     1500.619
mean                  8.427        8.687
std.deviation         4.852        4.691

minimum               0            1    
median                9           10    
90% percentile       14           14    
95% percentile       16           16    
maximum              21           21    

           &lt;=      duration     %  cum%
        0          1500.619   3.0   3.0 **|                                                                            
        1          2111.284   4.2   7.2 ***  |                                                                         
        2          3528.851   7.1  14.3 *****      |                                                                   
        3          4319.406   8.6  22.9 ******            |                                                            
        4          3354.732   6.7  29.6 *****                  |                                                       
        5          2445.603   4.9  34.5 ***                        |                                                   
        6          2090.759   4.2  38.7 ***                           |                                                
        7          2046.126   4.1  42.8 ***                               |                                            
        8          1486.956   3.0  45.8 **                                  |                                          
        9          2328.863   4.7  50.4 ***                                     |                                      
       10          4337.502   8.7  59.1 ******                                         |                               
       11          4546.145   9.1  68.2 *******                                               |                        
       12          4484.405   9.0  77.2 *******                                                      |                 
       13          4134.094   8.3  85.4 ******                                                              |          
       14          2813.860   5.6  91.1 ****                                                                    |      
       15          1714.894   3.4  94.5 **                                                                         |   
       16           992.690   2.0  96.5 *                                                                            | 
       17           541.546   1.1  97.6                                                                               |
       18           625.048   1.3  98.8 *                                                                              |
       19           502.291   1.0  99.8                                                                                |
       20            86.168   0.2 100.0                                                                                |
       21             8.162   0.0 100                                                                                   |
       22             0       0   100                                                                                   |
       23             0       0   100                                                                                   |
       24             0       0   100                                                                                   |
       25             0       0   100                                                                                   |
       26             0       0   100                                                                                   |
       27             0       0   100                                                                                   |
       28             0       0   100                                                                                   |
       29             0       0   100                                                                                   |
       30             0       0   100                                                                                   |
          inf         0       0   100  
```

If neither number_of_bins, nor lowerbound nor bin_width are specified, the histogram will be autoscaled.

Histograms can be printed with their values, instead of bins. This is particularly useful for non
numeric tallied values, like names of production stages. For example:

```
Histogram of Status
duration           300    

value                     duration     %
idle                        70      23.3 ******************                                                              
package                     42      14.0 ***********                                                                     
prepare                     48      16   ************                                                                    
stage A                     12       4   ***                                                                             
stage B                     50      16.7 *************                                                                   
stage C                     54      18   **************                                                                  
stage D                     24       8   ****** 
```

##  Merging of monitors

Monitors can be merged, to create a new monitor, nearly always to collect aggregated data.

The method Monitor.merge() is used for that, like:

    mc = m0.merge(m1, m2)

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

        print(sum((m0, m1, m2)).mean())

Finally, if ms = (m0, m1, m2), it is also possible to use:

    print(sum(ms).mean())

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

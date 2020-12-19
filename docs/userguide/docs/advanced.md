## Sync simulation with wall time

In simulation a clear distinction is made between real time and simulation time. The concept real time is used to refer to the wall-clock time. It represents the execution time of the experiment. The simulation time is an attribute of the simulator

To support use cases where a simulation may drive a demonstration or system check, the `kalasim` API allows to run a simulation at a defined clock speed.

Clearly, it may happen that a simulation is too complex to run at a defined clock. In such a situation, it will throw a `ClockOverloadException`. TBD continue this!

## Operational Control

**{Note}** This feature is still in preparation, see https://github.com/holgerbrandl/kalasim/issues/9

Even if `kalasim` tries to provide a simplsitic declarative approach to define a simulation, it may come alonw with computational demands simulation. To allow introspection into time-complexity of the underlying computations, the user may want to use the built-in `env.tickMetrics` [monitor](monitors.md) to analyze how much time is spent per time unit (aka *tick*). This monitor is not enabled by default and need to enabled when the environment is created by passing `tickMetricsEnabled=true`


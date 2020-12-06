# Sync simulation with real-world clock

In simulation a clear distinction is made between real time and simulation time. The concept real time is used to refer to the wall-clock time. It represents the execution time of the experiment. The simulation time is an attribute of the simulator

To support use cases where a simulation may drive a demonstration or system check, the `kalasim` API allows to run a simulation at a defined clock speed.

Clearly, it may happen that a simulation is too complex to run at a defined clock. In such a situation, it will throw a `ClockOverloadException`. TBD continue this!
# Sync simulation with real-world clock

To support use cases where a simulation may drive a demonstration or system check, the `kalasim` API allows to run a simulation at a defined clock speed.

Clearly, it may happen that a simulation is too complex to run at a defined clock. In such a situtation, it will throw a `ClockOverloadException`. TBD continue this!
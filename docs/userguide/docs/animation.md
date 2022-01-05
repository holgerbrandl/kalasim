# Process Animation


[//]: # (https://www.salabim.org/manual/Animation.html)


Animation is a powerful tool to debug, test and demonstrate simulations.

It is possible to show a number of shapes (lines, rectangles, circles, etc), texts as well (images) in a window. These objects can be dynamically updated. Monitors may be animated by showing the current value against the time. Furthermore the components in a queue may be shown in a highly customizable way. As text animation may be dynamically updated, it is even possible to show the current state, (monitor) statistics, etc. in an animation window.

Process animations can be

* [synchronized](advanced.md#clock-synchronization) with the simulation clock and run in real time (synchronized)
* advanced per simulation event (non synchronized)

## Elements

Animation is not part of the core API of kalasim, but support is provided by a decorator types (extending their respective base-type) 

### Animation

* `Component` -> `AnimationComponent`
* `Resource` -> `AnimationResource`
* `ComponentQueue` -> `AnimationResource`

**TODO** for queues consider point circles https://guide.openrndr.org/#/04_Drawing_basics/C05_ComplexShapes?id=shapes-and-contours-from-primitives

The animation support API does not bind to a particular rendering engine. However, only https://openrndr.org/ has been explored for process animation with kalasim.





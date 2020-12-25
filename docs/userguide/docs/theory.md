# Simulation Theory

As defined by Shannon (1975),
> a simulation is the process of designing a model of a real system and conducting experiments with this model for the
purpose either of understanding the behavior of the system or of evaluating various strategies
(within the limits imposed by a criterion or a set of criteria) for the operation of the system.


## What is discrete event simulation?

A discrete event simulation (DES) is a tool that allows studying the dynamic behavior of stochastic, dynamic and discretely evolving systems such as

* Factories
* Ports & Airports
* Traffic
* Supply chains & Logistics
* Controlling

In fact, every process that is founded on discrete state changes is suitable to be simulated with a discrete event simulation such as `kalasim`.

As described by [Urcar, 2019](https://www.jstatsoft.org/article/view/v090i02), the discrete nature of a given system arises as soon as its behavior can be described in terms of events, which is the most fundamental concept in DES. An event is an instantaneous occurrence that may change the state of the system, while, between events, all the state variables remain.

<!-- see Urcar, 2019 page 2-->
There are several main DES paradigms. In *activity-oriented*  DES the simulation clock advances in fixed time increments and all simulation entities are scanned and possibly reevaluated. Clearly, simulation performance degrades quickly with smaller increments and increasingly complex models.

In *event-oriented* DES is built around a list of scheduled events ordered by future execution time. During simulation, the these events are processed seqeuentially to update the state of the model.

Finally, *process-oriented* DES refines the event-oriented approach by defining a vocabulary of interactions to describe the interplay between simulation entities. This vocabulary is used by the modeler to define the component life-cycle processes of each simulation entity.

<!--todo meremaid figure here-->



## Applications of discrete event simulation

Depending on the system in question, DES and `kalasim` in particular can provide insights into the process efficiency, risks or effectiveness. In addition, it allows assessing alternative *what-if* scenarios. Very often planning is all about estimating the effect of changes to a system. such as more/fewer driver, more/fewer machines, more/less repair cycles, more/fewer cargo trolleys.

Typical applications of discrete event simulations are

* Production planning (such as bottleneck analysis)
* Dimensioning (How many drivers are needed? Number of servers?)
* Process automation & visualization
* Digital twin development
* Project management

For  in-depth primers about simulation see [here](https://simulation.tudelft.nl/dsol/manual/simulation-theory/introduction) or [Urcar, 2019](https://www.jstatsoft.org/article/view/v090i02).


## Other Simulation Tools

There are too many to be listed. In generally there are graphical tools and APIs
 . Graphical tools, such as [AnyLogic](https://www.anylogic.com/) excel by providing a flat learning curve, great visuals but often lack interfaces for extensibility or automation. APIs are usually much more flexible but often lack an intuitive approach to actually build simulations.

Out of the great number of APIs, we [pinpoint](about.md#acknowledgements) just  those projects/products which served as source of inspiration when developing `kalasim`.


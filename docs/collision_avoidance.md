Collision avoidance is a crucial part of autonomous vehicle systems. It comprises various algorithms, sensors, and
techniques to predict potential collisions and take appropriate actions to avoid them. If we consider vehicles moving
along defined PathSegment instances like in your code, several methods can be used:

1. Distance Keeping: Use the distance between vehicles to ensure that they maintain a safe separation while moving along
   the path. If a vehicle gets too close to another, reduce its speed or even stop it to prevent collisions. The safe
   distance can be calculated based on vehicle speed, braking capability, road conditions, etc.
2. Speed Control: Limit the maximum speed of vehicles on a given path segment. This can help provide vehicles with
   enough time to detect and react to objects in their vicinity.

## method

In your domain model, we have the definition of PathSegment and Port. Let's assume we have two vehicles, vehicle1 and
vehicle2 which are at two different Port on the same PathSegment, and we want to compute collision point when moving
these vehicles with different speeds.

Let's denote the following:

d1 = initial distance of vehicle1 from the starting point of the PathSegment (can be retrieved from the corresponding
Port's distance field) d2 = initial distance of vehicle2 from the starting point of the PathSegment (can be retrieved
from the corresponding Port's distance field)

s1 = speed of vehicle1 s2 = speed of vehicle2

Collision will happen when the vehicles reach the same position in the same time, i.e. their speeds and distances
satisfy the following equation:

d1 + s1t = d2 + s2t

From this, we can calculate the time until collision (if it happens) by solving the equation for t:

t = (d2 - d1) / (s1 - s2)

Note that this only works if s1 ≠ s2. If the speeds are equal, the vehicles will never collide unless they start at the
same position.

Once you have t, you can plug it back into either vehicle's equation to get the collision distance from the starting
point of the PathSegment:

d = d1 + s1*t

The position can be computed similarly to how it is done in the Port class:

x = segment.from.position.x + d * (segment.to.position.x - segment.from.position.x) y = segment.from.position.y + d * (
segment.to.position.y - segment.from.position.y)
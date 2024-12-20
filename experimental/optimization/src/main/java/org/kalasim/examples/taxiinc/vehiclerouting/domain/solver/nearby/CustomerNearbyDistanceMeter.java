package org.kalasim.examples.taxiinc.vehiclerouting.domain.solver.nearby;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.Location;

public class CustomerNearbyDistanceMeter implements NearbyDistanceMeter<Location, Location> {

    @Override
    public double getNearbyDistance(Location origin, Location destination) {
        long distance = origin.getDistanceTo(destination);
        // If arriving early also inflicts a cost (more than just not using the vehicle more), such as the driver's wage, use this:
        //        if (origin instanceof TimeWindowedCustomer && destination instanceof TimeWindowedCustomer) {
        //            distance += ((TimeWindowedCustomer) origin).getTimeWindowGapTo((TimeWindowedCustomer) destination);
        //        }
        return distance;
    }

}

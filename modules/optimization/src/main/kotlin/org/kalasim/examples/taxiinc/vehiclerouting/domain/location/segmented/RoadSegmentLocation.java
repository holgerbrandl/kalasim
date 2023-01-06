package org.kalasim.examples.taxiinc.vehiclerouting.domain.location.segmented;

import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.DistanceType;
import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.Location;
import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.RoadLocation;

import java.util.*;

/**
 * Like {@link RoadLocation},
 * but for high scale problems to avoid the memory issue of keeping the entire cost matrix in memory.
 * Used with {@link DistanceType#SEGMENTED_ROAD_DISTANCE}.
 */
public class RoadSegmentLocation extends Location {

    // Prefer Map over array or Li`st because customers might be added and removed in real-time planning.
    protected Map<RoadSegmentLocation, Double> nearbyTravelDistanceMap;
    protected Map<HubSegmentLocation, Double> hubTravelDistanceMap;

    public RoadSegmentLocation() {
    }

    public RoadSegmentLocation(long id) {
        super(id);
    }

    public RoadSegmentLocation(long id, double latitude, double longitude) {
        super(id, latitude, longitude);
    }

    public Map<RoadSegmentLocation, Double> getNearbyTravelDistanceMap() {
        return nearbyTravelDistanceMap;
    }

    public void setNearbyTravelDistanceMap(Map<RoadSegmentLocation, Double> nearbyTravelDistanceMap) {
        this.nearbyTravelDistanceMap = nearbyTravelDistanceMap;
    }

    public Map<HubSegmentLocation, Double> getHubTravelDistanceMap() {
        return hubTravelDistanceMap;
    }

    public void setHubTravelDistanceMap(Map<HubSegmentLocation, Double> hubTravelDistanceMap) {
        this.hubTravelDistanceMap = hubTravelDistanceMap;
    }

    @Override
    public long getDistanceTo(Location location) {
        Double distance = getDistanceDouble((RoadSegmentLocation) location);
        // Multiplied by 1000 to avoid floating point arithmetic rounding errors
        return (long) (distance * 1000.0 + 0.5);
    }

    public Double getDistanceDouble(RoadSegmentLocation location) {
        Double distance = nearbyTravelDistanceMap.get(location);
        if (distance == null) {
            // location isn't nearby
            distance = getShortestDistanceDoubleThroughHubs(location);
        }
        return distance;
    }

    protected double getShortestDistanceDoubleThroughHubs(RoadSegmentLocation location) {
        double shortestDistance = Double.MAX_VALUE;
        for (Map.Entry<HubSegmentLocation, Double> entry : hubTravelDistanceMap.entrySet()) {
            double distance = entry.getValue();
            distance += entry.getKey().getDistanceDouble(location);
            if (distance < shortestDistance) {
                shortestDistance = distance;
            }
        }
        return shortestDistance;
    }

}

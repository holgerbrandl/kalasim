package org.kalasim.examples.taxiinc.vehiclerouting.domain.location;

import org.kalasim.examples.taxiinc.vehiclerouting.domain.AbstractPersistable;
import org.kalasim.examples.taxiinc.vehiclerouting.domain.VehicleRoutingSolution;

public abstract class Location extends AbstractPersistable {

    protected String name = null;
    protected double latitude;
    protected double longitude;

    public Location() {
    }

    public Location(long id) {
        super(id);
    }

    public Location(long id, double latitude, double longitude) {
        super(id);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    /**
     * The distance's unit of measurement depends on the {@link VehicleRoutingSolution}'s {@link DistanceType}.
     * It can be in miles or km, but for most cases it's in the TSPLIB's unit of measurement.
     *
     * @param location never null
     * @return a positive number, the distance multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    public abstract long getDistanceTo(Location location);

    public double getAirDistanceDoubleTo(Location location) {
        // Implementation specified by TSPLIB http://www2.iwr.uni-heidelberg.de/groups/comopt/software/TSPLIB95/
        // Euclidean distance (Pythagorean theorem) - not correct when the surface is a sphere
        double latitudeDifference = location.latitude - latitude;
        double longitudeDifference = location.longitude - longitude;
        return Math.sqrt(
                (latitudeDifference * latitudeDifference) + (longitudeDifference * longitudeDifference));
    }

    /**
     * The angle relative to the direction EAST.
     *
     * @param location never null
     * @return in Cartesian coordinates
     */
    public double getAngle(Location location) {
        // Euclidean distance (Pythagorean theorem) - not correct when the surface is a sphere
        double latitudeDifference = location.latitude - latitude;
        double longitudeDifference = location.longitude - longitude;
        return Math.atan2(latitudeDifference, longitudeDifference);
    }

    @Override
    public String toString() {
        if (name == null) {
            return super.toString();
        }
        return name;
    }

}

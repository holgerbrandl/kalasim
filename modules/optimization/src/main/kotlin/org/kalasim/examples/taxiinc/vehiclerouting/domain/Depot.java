package org.kalasim.examples.taxiinc.vehiclerouting.domain;

import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.Location;
import org.kalasim.examples.taxiinc.vehiclerouting.domain.timewindowed.TimeWindowedDepot;

public class Depot extends AbstractPersistable {

    protected Location location;

    public Depot() {
    }

    public Depot(long id, Location location) {
        super(id);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public String toString() {
        if (location.getName() == null) {
            return super.toString();
        }
        return location.getName();
    }

}

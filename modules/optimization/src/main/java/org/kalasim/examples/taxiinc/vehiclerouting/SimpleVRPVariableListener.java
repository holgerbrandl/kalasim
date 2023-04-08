package org.kalasim.examples.taxiinc.vehiclerouting;

import org.kalasim.examples.taxiinc.vehiclerouting.domain.Customer;
import org.kalasim.examples.taxiinc.vehiclerouting.domain.VehicleRoutingSolution;
import org.kalasim.examples.taxiinc.vehiclerouting.domain.timewindowed.TimeWindowedCustomer;
import org.kalasim.examples.taxiinc.vehiclerouting.domain.timewindowed.TimeWindowedDepot;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.util.Objects;

// TODO When this class is added only for TimeWindowedCustomer, use TimeWindowedCustomer instead of Customer
public class SimpleVRPVariableListener implements VariableListener<VehicleRoutingSolution, Customer> {

    @Override
    public void beforeEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
            updateArrivalTime(scoreDirector, customer);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
            updateArrivalTime(scoreDirector, customer);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Customer customer) {
        // Do nothing
    }

    protected void updateArrivalTime(ScoreDirector<VehicleRoutingSolution> scoreDirector,
            Customer customer) {

        if (customer.getVehicle() == null) {
            if (customer.routePostion != null) {
                scoreDirector.beforeVariableChanged(customer, "routePostion");
                customer.routePostion = null;
                scoreDirector.afterVariableChanged(customer, "routePostion");
            }

            return;
        }else{
            customer.routePostion = 10;
        }

        Customer previousCustomer = customer.getPreviousCustomer();

//        Long departureTime;
//        if (previousCustomer == null) {
//            departureTime = ((TimeWindowedDepot) customer.getVehicle().getDepot()).getReadyTime();
//        } else {
//            departureTime = ((TimeWindowedCustomer) previousCustomer).getDepartureTime();
//        }
//        Customer shadowCustomer = customer;
//
//        Long arrivalTime = calculateArrivalTime(shadowCustomer, departureTime);
//
//        while (shadowCustomer != null && !Objects.equals(shadowCustomer.getArrivalTime(), arrivalTime)) {
//            scoreDirector.beforeVariableChanged(shadowCustomer, "arrivalTime");
//            shadowCustomer.setArrivalTime(arrivalTime);
//            scoreDirector.afterVariableChanged(shadowCustomer, "arrivalTime");
//            departureTime = shadowCustomer.getDepartureTime();
//            shadowCustomer = (TimeWindowedCustomer) shadowCustomer.getNextCustomer();
//            arrivalTime = calculateArrivalTime(shadowCustomer, departureTime);
//        }
    }

    private Long calculateArrivalTime(TimeWindowedCustomer customer, Long previousDepartureTime) {
        if (customer == null || previousDepartureTime == null) {
            return null;
        }
        return previousDepartureTime + customer.getDistanceFromPreviousStandstill();
    }

}

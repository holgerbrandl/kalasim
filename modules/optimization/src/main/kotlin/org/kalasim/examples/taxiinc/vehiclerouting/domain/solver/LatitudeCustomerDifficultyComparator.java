package org.kalasim.examples.taxiinc.vehiclerouting.domain.solver;

import org.kalasim.examples.taxiinc.vehiclerouting.domain.Customer;

import java.util.*;

/**
 * On large datasets, the constructed solution looks like a zebra crossing.
 */
public class LatitudeCustomerDifficultyComparator implements Comparator<Customer> {

    private static final Comparator<Customer> COMPARATOR = Comparator
            .comparingDouble((Customer customer) -> customer.getLocation().getLatitude())
            .thenComparingDouble(customer -> customer.getLocation().getLongitude())
            .thenComparingInt(Customer::getDemand)
            .thenComparingLong(Customer::getId);

    @Override
    public int compare(Customer a, Customer b) {
        return COMPARATOR.compare(a, b);
    }

}

package org.freenetproject.routing_simulator.graph.node.peer;

import java.text.DecimalFormat;

import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.FoldingPolicy;
import org.freenetproject.routing_simulator.graph.Location;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;

public class PrecisionLoss extends LoopDetection {

    private final double precisionLoss;

    public PrecisionLoss(final FoldingPolicy policy,
            final RandomGenerator random, final double randomRoutingChance,
            long requestId, double precisionLoss) {
        super(policy, random, randomRoutingChance, requestId);
        this.precisionLoss = precisionLoss;
    }

    @Override
    protected double calculateDifference(SimpleNode n, int lookAhead,
            double target) {
        if (lookAhead < 2) {
            return n.distanceToLoc(target);
        }

        double remainingPrecision = Math.pow(1 - this.precisionLoss,
                lookAhead - 1);

        String locationText = new DecimalFormat("#.#######################")
                .format(n.getLocation());

        int integerPlaces = locationText.indexOf('.');
        int decimalPlaces = locationText.length() - integerPlaces - 1;
        if (decimalPlaces == 1) {
            return n.distanceToLoc(target);
        }

        double reducedPlaces = remainingPrecision * decimalPlaces;
        String reducedLocation = locationText.substring(0, integerPlaces
                + (int) Math.ceil(reducedPlaces) + 1);

        return Location.distance(Double.parseDouble(reducedLocation), target);
    }

}

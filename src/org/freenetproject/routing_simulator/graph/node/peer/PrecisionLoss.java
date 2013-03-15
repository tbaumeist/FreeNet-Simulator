package org.freenetproject.routing_simulator.graph.node.peer;

import java.text.DecimalFormat;

import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.FoldingPolicy;
import org.freenetproject.routing_simulator.graph.Location;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;

public class PrecisionLoss extends LoopDetection {

    private final int significantBitsLookAhead;

    public PrecisionLoss(final FoldingPolicy policy,
            final RandomGenerator random, final double randomRoutingChance,
            final int nLookBack, long requestId, int significantBitsLookAhead) {
        super(policy, random, randomRoutingChance, nLookBack, requestId);
        this.significantBitsLookAhead = significantBitsLookAhead;
    }

    @Override
    protected double calculateDifference(SimpleNode n, int lookAhead,
            double target) {
        if (lookAhead < 2) {
            return n.distanceToLoc(target);
        }



        String locationText = new DecimalFormat("#.#######################")
                .format(n.getLocation());

        int integerPlaces = locationText.indexOf('.');
        int decimalPlaces = locationText.length() - integerPlaces - 1;
        if (decimalPlaces == 1) {
            return n.distanceToLoc(target);
        }

        String reducedLocation = locationText.substring(0, integerPlaces
                + (int) Math.ceil(significantBitsLookAhead) + 1);

        return Location.distance(Double.parseDouble(reducedLocation), target);
    }

}

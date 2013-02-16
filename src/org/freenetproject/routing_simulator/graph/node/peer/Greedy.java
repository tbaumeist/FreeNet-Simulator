package org.freenetproject.routing_simulator.graph.node.peer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.FoldingPolicy;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.DistanceEntry;

public class Greedy extends PeerSelector {

    public Greedy(final FoldingPolicy foldingPolicy,
            final RandomGenerator random, final double randomRoutingChance) {
        super(foldingPolicy, random, randomRoutingChance);
    }

    @Override
    public SimpleNode selectPeer(double target, SimpleNode from,
            final int nLookAhead, final List<SimpleNode> currentPath) {
        
        SimpleNode next = from;
        final double closest = from.distanceToLoc(target);
        ArrayList<DistanceEntry> distances = getDistances(from, target,
                nLookAhead);

        while (!distances.isEmpty()) {
            if (distances.get(0).getDistance() < closest) {
                next = distances.get(0).getNextNode();
                break;
            }
            distances.remove(0);
        }

        return next;
    }
}

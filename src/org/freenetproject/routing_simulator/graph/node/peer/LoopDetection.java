package org.freenetproject.routing_simulator.graph.node.peer;

import java.util.ArrayList;

import org.freenetproject.routing_simulator.FoldingPolicy;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.DistanceEntry;

public class LoopDetection extends PeerSelector {

    private long requestID;

    public LoopDetection(final FoldingPolicy foldingPolicy, long requestId) {
        super(foldingPolicy);
        this.requestID = requestId;
    }

    @Override
    public SimpleNode selectPeer(final double target, final SimpleNode from,
            final int nLookAhead) {
        SimpleNode next = from;
        // final double closest = from.distanceToLoc(target);
        ArrayList<DistanceEntry> distances = getDistances(from, target,
                nLookAhead);

        while (!distances.isEmpty()) {
            if (/* distances.get(0).distance < closest && */distances.get(0)
                    .getNextNode().getLastRouted() != this.requestID) {
                next = distances.get(0).getNextNode();
                break;
            }
            distances.remove(0);
        }

        return next;
    }
}
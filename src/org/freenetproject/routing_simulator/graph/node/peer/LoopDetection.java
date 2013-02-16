package org.freenetproject.routing_simulator.graph.node.peer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.FoldingPolicy;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.DistanceEntry;

public class LoopDetection extends PeerSelector {

    private long requestID;
    protected final int lookBack;

    public LoopDetection(final FoldingPolicy foldingPolicy,
            final RandomGenerator random, final double randomRoutingChance,
            final int lookBack, long requestId) {
        super(foldingPolicy, random, randomRoutingChance);
        this.lookBack = lookBack;
        this.requestID = requestId;
    }

    @Override
    public SimpleNode selectPeer(final double target, final SimpleNode from,
            final int nLookAhead, final List<SimpleNode> currentPath) {

        SimpleNode next = from;
        // final double closest = from.distanceToLoc(target);
        ArrayList<DistanceEntry> distances = getDistances(from, target,
                nLookAhead);

        while (!distances.isEmpty()) {
            if (/* distances.get(0).distance < closest && */
            !this.visited(currentPath, distances.get(0))) {
                next = distances.get(0).getNextNode();
                break;
            }
            distances.remove(0);
        }

        return next;
    }

    protected boolean visited(final List<SimpleNode> currentPath,
            DistanceEntry entry) {
        if( this.lookBack < 1 ) {
            return entry.getNextNode().getLastRouted() == this.requestID;
        }
        
        ListIterator<SimpleNode> iter = currentPath.listIterator(currentPath.size()); 
        for( int i = 0; i < this.lookBack; i++) {
            if( !iter.hasPrevious()) {
                return false;
            }
            if( iter.previous().equals(entry.getNextNode())) {
                return true;
            }
        }
        return false;
    }
}
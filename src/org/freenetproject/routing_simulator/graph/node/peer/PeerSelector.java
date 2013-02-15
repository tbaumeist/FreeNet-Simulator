package org.freenetproject.routing_simulator.graph.node.peer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.freenetproject.routing_simulator.FoldingPolicy;
import org.freenetproject.routing_simulator.graph.Location;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.DistanceEntry;

public abstract class PeerSelector {
    protected final FoldingPolicy foldingPolicy;
    
    public PeerSelector(final FoldingPolicy foldingPolicy) {
        this.foldingPolicy = foldingPolicy;
    }
    
    public abstract SimpleNode selectPeer(final double target,
            final SimpleNode from, final int nLookAhead);

    protected ArrayList<DistanceEntry> getDistances(SimpleNode node,
            final double target, final int nLookAhead) {
        ArrayList<DistanceEntry> peers = new ArrayList<DistanceEntry>();
        
        // only use caching if NONE path folding policy is used.
        if (this.foldingPolicy == FoldingPolicy.NONE && node.getRoutingCache(nLookAhead) != null) {
            peers.addAll(node.getRoutingCache(nLookAhead));
            updateDistances(peers, target);
            return peers;
        }

        for (SimpleNode peer : node.getConnections()) {
            peers.add(new DistanceEntry(peer.distanceToLoc(target), peer, peer,
                    1));
        }

        peers = getDistances(peers, target, nLookAhead, 1);

        Collections.sort(peers);
        if (this.foldingPolicy == FoldingPolicy.NONE)
            node.setRoutingCache(peers, nLookAhead);
        return peers;
    }

    private void updateDistances(ArrayList<DistanceEntry> nodes, double target) {
        for (DistanceEntry e : nodes) {
            e.setDistance(this.calculateDifference(e.getFinalNode(), e.getLookAheadLevel(), target));
        }
        Collections.sort(nodes);
    }
    
    protected double calculateDifference(SimpleNode n, int lookAhead, double target) {
        return n.distanceToLoc(target);
    }

    private ArrayList<DistanceEntry> getDistances(
            ArrayList<DistanceEntry> nodes, final double target,
            final int nLookAhead, int nLevel) {
        if (nLookAhead <= nLevel)
            return nodes;

        // Get all the next level nodes
        Hashtable<SimpleNode, List<DistanceEntry>> nextLevelPeers = new Hashtable<SimpleNode, List<DistanceEntry>>();
        for (DistanceEntry dist : nodes) {
            if (dist.getLookAheadLevel() != nLevel)
                continue;
            for (SimpleNode p : dist.getFinalNode().getConnections()) {
                if (!nextLevelPeers.containsKey(p))
                    nextLevelPeers.put(p, new ArrayList<DistanceEntry>());
                
                double diff = this.calculateDifference(p, nLevel + 1, target);
                nextLevelPeers.get(p).add(
                        new DistanceEntry(diff, dist.getNextNode(), p,
                                nLevel + 1));
            }
        }

        // add the next level entries to the list
        // remove duplicates by randomly selecting one of the entries
        for (List<DistanceEntry> entry : nextLevelPeers.values()) {
            // does the list already have that location, if so it is closer
            // because
            // it will have a smaller level
            if (nodes.contains(entry.get(0)))
                continue;
            // add a random item
            nodes.add(entry.get(entry.get(0).getNextNode().getRandom()
                    .nextInt(entry.size())));
        }

        // get the next level
        nodes = getDistances(nodes, target, nLookAhead, nLevel + 1);

        return nodes;
    }
}

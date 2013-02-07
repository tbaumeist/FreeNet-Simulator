package org.freenetproject.routing_simulator.util;

import org.freenetproject.routing_simulator.graph.node.SimpleNode;

/**
 * Convenience class for ranking nodes by distance.
 */
public class DistanceEntry implements Comparable<DistanceEntry> {
    /**
     * Distance between the current node (relative) and the final node.
     */
    private double distance;
    /**
     * The next node that would actually be routed to.
     */
    private final SimpleNode nextNode;
    /**
     * The final node that we are trying to route towards.
     */
    private final SimpleNode finalNode;
    /**
     * The level (hops) of look ahead we are at.
     */
    private final int lookAheadLevel;

    /**
     * Create a new distance entry.
     * 
     * @param distance
     *            Distance from the relative node to the final node.
     * @param nextNode
     *            Node that would actually be routed to (one hop away).
     * @param finalNode
     *            The node that we are ultimately trying to route to.
     * @param lookAheadLevel
     *            Number of hops of look ahead this entry is from.
     */
    public DistanceEntry(final double distance, final SimpleNode nextNode,
            final SimpleNode finalNode, final int lookAheadLevel) {
        this.distance = distance;
        this.nextNode = nextNode;
        this.finalNode = finalNode;
        this.lookAheadLevel = lookAheadLevel;
    }
    
    public void updateDistance(double target) {
    	double diff = this.getFinalNode().distanceToLoc(target);
    	this.distance = diff;
    }

    /**
     * @return the distance
     */
    public final double getDistance() {
        return distance;
    }

    /**
     * @return the nextNode
     */
    public final SimpleNode getNextNode() {
        return nextNode;
    }

    /**
     * @return the finalnNode
     */
    public final SimpleNode getFinalNode() {
        return finalNode;
    }

    /**
     * @return the lookAheadLevel
     */
    public final int getLookAheadLevel() {
        return lookAheadLevel;
    }

    @Override
    public final int compareTo(final DistanceEntry other) {
        return Double.compare(this.getDistance(), other.getDistance());
    }

    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof DistanceEntry)) {
            return false;
        }
        final DistanceEntry other = (DistanceEntry) o;
        return this.getFinalNode().equals(other.getFinalNode());
    }

    @Override
    public final int hashCode() {
        return ((Double) this.getDistance()).hashCode();
    }

    @Override
    public final String toString() {
        StringBuilder b = new StringBuilder();
        b.append(this.getDistance()).append(" ");
        b.append(this.getNextNode().index).append(" ");
        b.append(this.getFinalNode().index);
        return b.toString();
    }
}

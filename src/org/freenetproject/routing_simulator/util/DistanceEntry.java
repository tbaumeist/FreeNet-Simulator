package org.freenetproject.routing_simulator.util;

import org.freenetproject.routing_simulator.graph.node.SimpleNode;

/**
 * Convenience class for ranking nodes by distance.
 */
public class DistanceEntry implements Comparable<DistanceEntry>{
	public final double distance;
	public final SimpleNode routeToNode, destinationNode;
	public final int nLevel;

	public DistanceEntry(double distance, SimpleNode routeToNode, SimpleNode destinationNode, int nLevel) {
		this.distance = distance;
		this.routeToNode = routeToNode;
		this.destinationNode = destinationNode;
		this.nLevel = nLevel;
	}

	@Override
	public int compareTo(DistanceEntry other) {
		return Double.compare(this.distance, other.distance);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DistanceEntry)) return false;

		final DistanceEntry other = (DistanceEntry)o;

		return this.distance == other.distance;
	}
	
	@Override
	public String toString(){
		return this.distance + " " + this.routeToNode.index + " " + this.destinationNode.index;
	}
}
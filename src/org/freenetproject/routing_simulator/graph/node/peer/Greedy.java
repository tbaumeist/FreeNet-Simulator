package org.freenetproject.routing_simulator.graph.node.peer;

import java.util.ArrayList;

import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.DistanceEntry;

public class Greedy extends PeerSelector {
	@Override
	public SimpleNode selectPeer(double target, SimpleNode from, ArrayList<SimpleNode> chain, final int nLookAhead) {		
		SimpleNode next = from;
		final double closest = from.distanceToLoc(target);
		ArrayList<DistanceEntry> distances = getDistances(from, target, nLookAhead);

		while(!distances.isEmpty()){
			if(distances.get(0).getDistance() < closest){
				next = distances.get(0).getNextNode();
				break;
			}
			distances.remove(0);
		}
		
		return next;
	}
}

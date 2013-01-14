package org.freenetproject.routing_simulator.graph.folding;

import java.util.ArrayList;

import org.freenetproject.routing_simulator.graph.node.SimpleNode;

public class PathFoldingResult {
	private ArrayList<SimpleNode> disconnected = new ArrayList<SimpleNode>();
	private int foldingOperations = 0;
	
	public ArrayList<SimpleNode> getDisconnected(){
		return this.disconnected;
	}
	
	public void folded(){
		this.foldingOperations++;
	}
	
	public int getFoldingOperations(){
		return this.foldingOperations;
	}
	
	public void addDisconnected(SimpleNode n){
		this.disconnected.add(n);
	}
}

package test.org.freenetproject.routing_simulator.graph;

import java.io.File;

import junit.framework.Assert;

import org.freenetproject.routing_simulator.RoutingSim;
import org.junit.Test;

public class Test_Routing {
	
	@Test
	public void largeRouting() throws Exception{
		File tmp = File.createTempFile("Freenet", "test");
		String tmpPath = tmp.getParent() + File.separator + "freenet_test"+ File.separator;
		tmp.delete();
		System.out.println("Writing files to " + tmpPath);
		String[] args = new String[]
				{"--ideal-link", 
				"--fixed-degree", "5", 
				"--size", "4000", 
				"--include-lattice",
				"--route", "144000", 
				"--route-hops", "18",
				"--look-ahead", "1", 
				"--route-policy", "BACKTRACKING",
				"--fold-policy", "FREENET", 
				"--bootstrap", 
				"--output-route", tmpPath + "route", 
				"--save-graph", tmpPath + "graph.g", 
				"--save-graph-dot", tmpPath + "graph.dot", 
				"--output-link", tmpPath + "link.dat", 
				"--output-degree", tmpPath + "degree.dat"};
		RoutingSim.main(args);
		Assert.assertTrue(true);
	}

}

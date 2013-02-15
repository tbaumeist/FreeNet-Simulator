package test.org.freenetproject.routing_simulator.graph;

import static org.junit.Assert.*;

import java.io.File;

import org.freenetproject.routing_simulator.Arguments;
import org.freenetproject.routing_simulator.FoldingPolicy;
import org.freenetproject.routing_simulator.RouteResult;
import org.freenetproject.routing_simulator.RoutingPolicy;
import org.freenetproject.routing_simulator.RoutingSim;
import org.freenetproject.routing_simulator.graph.Graph;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.junit.Test;

import test.org.freenetproject.routing_simulator.Test_Helper;

public class Test_Routing {

    @Test
    public void largeRouting() throws Exception {
        File tmp = File.createTempFile("Freenet", "test");
        String tmpPath = tmp.getParent() + File.separator;
        tmp.delete();
        System.out.println("Writing files to " + tmpPath);
        String[] args = new String[] { "--link-ideal", "--degree-fixed", "5",
                "--graph-size", "1000", "--route", "144000", "--route-hops",
                "18", "--route-bootstrap", "--route-output",
                tmpPath + "route.dat", "--graph-save", tmpPath + "graph.g",
                "--log-level", "detailed" };
        new RoutingSim().run(Arguments.parse(args));
        assertTrue(true);
    }

    @Test
    public void visitedLength() throws Exception {
        // special graph file that I already know certain properties about
        File gmlFile = new File(Test_Helper.getResourcePath("gml-graph-1.gml"));
        final Graph graph = Test_Helper.readFromFileGml(gmlFile);

        SimpleNode nodeA = graph.getNode(0);
        SimpleNode nodeC = graph.getNode(2);

        assertTrue(nodeA.getLocation() == 0.1);
        assertTrue(nodeC.getLocation() == 0.3);

        int maxHTL = 6;
        RouteResult result = nodeA.route(nodeC, maxHTL,
                RoutingPolicy.BACKTRACKING, FoldingPolicy.NONE, 1, true);
        // should take the path 0.1 > 0.5 > 0.2 > 0.7 < 0.2 < 0.5 < 0.1 > 0.6 >
        // 0.3
        // > means routed to new node
        // < means had to back track
        assertTrue(result.isSuccess());
        assertTrue(result.getPathLength() == 3);
        assertTrue(result.getTravelLength() == 6);
        assertTrue(result.getRoutingPath().get(0).getLocation() == 0.1);
        assertTrue(result.getRoutingPath().get(1).getLocation() == 0.6);
        assertTrue(result.getRoutingPath().get(2).getLocation() == 0.3);

        SimpleNode nodeH = graph.getNode(7);
        assertTrue(nodeH.getLocation() == 0.8);
        maxHTL = 7;
        result = nodeA.route(nodeH, maxHTL, RoutingPolicy.BACKTRACKING,
                FoldingPolicy.NONE, 1, true);
        assertTrue(!result.isSuccess());
        assertTrue(result.getTravelLength() == maxHTL);

        maxHTL = 8;
        result = nodeA.route(nodeH, maxHTL, RoutingPolicy.BACKTRACKING,
                FoldingPolicy.NONE, 1, true);
        // path .1 > .7 > .2 > .5 < .2 < .7 < .1 > .6 > .4 > .9 > .8
        assertTrue(result.isSuccess());
        assertTrue(result.getTravelLength() == 8);
        assertTrue(result.getPathLength() == 5);
        assertTrue(result.getRoutingPath().get(0).getLocation() == 0.1);
        assertTrue(result.getRoutingPath().get(1).getLocation() == 0.6);
        assertTrue(result.getRoutingPath().get(2).getLocation() == 0.4);
        assertTrue(result.getRoutingPath().get(3).getLocation() == 0.9);
        assertTrue(result.getRoutingPath().get(4).getLocation() == 0.8);

        // look ahead of 2
        result = nodeA.route(nodeH, maxHTL, RoutingPolicy.BACKTRACKING,
                FoldingPolicy.NONE, 2, true);
        // path .1 > .7 > .2 > .5 < .2 < .7 < .1 > .6 > .3 > .8
        assertTrue(result.isSuccess());
        assertTrue(result.getTravelLength() == 7);
        assertTrue(result.getPathLength() == 4);
        assertTrue(result.getRoutingPath().get(0).getLocation() == 0.1);
        assertTrue(result.getRoutingPath().get(1).getLocation() == 0.6);
        assertTrue(result.getRoutingPath().get(2).getLocation() == 0.3);
        assertTrue(result.getRoutingPath().get(3).getLocation() == 0.8);

        // look ahead of 3
        result = nodeA.route(nodeH, maxHTL, RoutingPolicy.BACKTRACKING,
                FoldingPolicy.NONE, 3, true);
        // path .1 > .6 > .3 > .8
        assertTrue(result.isSuccess());
        assertTrue(result.getTravelLength() == 4);
        assertTrue(result.getPathLength() == 4);
        assertTrue(result.getRoutingPath().get(0).getLocation() == 0.1);
        assertTrue(result.getRoutingPath().get(1).getLocation() == 0.6);
        assertTrue(result.getRoutingPath().get(2).getLocation() == 0.3);
        assertTrue(result.getRoutingPath().get(3).getLocation() == 0.8);

    }

    @Test
    public void routingPathLargeNetwork() throws Exception {
        File dotFile = new File(Test_Helper.getResourcePath("1000node.dot"));
        final Graph graph = Test_Helper.readFromFileDot(dotFile);

        SimpleNode node450 = graph.getNode(450);
        SimpleNode node100 = graph.getNode(100);

        assertTrue(node450.getLocation() == 0.44551104914022455);
        assertTrue(node100.getLocation() == 0.10630535558247556);

        int maxHTL = 100;
        RouteResult result = node450.route(node100, maxHTL,
                RoutingPolicy.BACKTRACKING, FoldingPolicy.NONE, 1, true);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getPathLength() == 10);
        assertTrue(result.getTravelLength() == 10);
        assertTrue(result.getRoutingPath().get(0).index == 450);
        assertTrue(result.getRoutingPath().get(1).index == 205);
        assertTrue(result.getRoutingPath().get(2).index == 175);
        assertTrue(result.getRoutingPath().get(3).index == 168);
        assertTrue(result.getRoutingPath().get(4).index == 130);
        assertTrue(result.getRoutingPath().get(5).index == 105);
        assertTrue(result.getRoutingPath().get(6).index == 95);
        assertTrue(result.getRoutingPath().get(7).index == 99);
        assertTrue(result.getRoutingPath().get(8).index == 98);
        assertTrue(result.getRoutingPath().get(9).index == 100);
    }
    
    @Test
    public void routingPathSmallNetwork() throws Exception {
        File dotFile = new File(Test_Helper.getResourcePath("20node.dot"));
        final Graph graph = Test_Helper.readFromFileDot(dotFile);

        SimpleNode nodeA = graph.getNode(5);
        SimpleNode nodeB = graph.getNode(18);

        assertTrue(nodeA.getLocation() == 0.4136244989486966);
        assertTrue(nodeB.getLocation() == 0.9131614916580988);

        int maxHTL = 100;
        RouteResult result = nodeA.route(nodeB, maxHTL,
                RoutingPolicy.BACKTRACKING, FoldingPolicy.NONE, 1, true);
        
//        [0.4136244989486966 5, 0.474119841114474 8, 0.5860513580630204 9, 
//         0.6457999139358759 12, 0.6687893921306722 13, 0.7589678048955022 14, 
//         0.7606234660001643 15, 0.8562268784404246 16, 0.9063975762556262 17,
//         0.9131614916580988 18]

        assertTrue(result.isSuccess());
        assertTrue(result.getPathLength() == 10);
        assertTrue(result.getTravelLength() == 10);
        assertTrue(result.getRoutingPath().get(0).index == 5);
        assertTrue(result.getRoutingPath().get(1).index == 8);
        assertTrue(result.getRoutingPath().get(2).index == 9);
        assertTrue(result.getRoutingPath().get(3).index == 12);
        assertTrue(result.getRoutingPath().get(4).index == 13);
        assertTrue(result.getRoutingPath().get(5).index == 14);
        assertTrue(result.getRoutingPath().get(6).index == 15);
        assertTrue(result.getRoutingPath().get(7).index == 16);
        assertTrue(result.getRoutingPath().get(8).index == 17);
        assertTrue(result.getRoutingPath().get(9).index == 18);
        
        result = nodeA.route(nodeB, maxHTL,
                RoutingPolicy.BACKTRACKING, FoldingPolicy.NONE, 2, true);
        
        //[0.4136244989486966 5, 0.4615541189612562 6, 0.9410194257801392 19, 0.9131614916580988 18]
        
        assertTrue(result.isSuccess());
        assertTrue(result.getPathLength() == 4);
        assertTrue(result.getTravelLength() == 4);
        assertTrue(result.getRoutingPath().get(0).index == 5);
        assertTrue(result.getRoutingPath().get(1).index == 6);
        assertTrue(result.getRoutingPath().get(2).index == 19);
        assertTrue(result.getRoutingPath().get(3).index == 18);
        
        result = nodeA.route(nodeB, maxHTL,
                RoutingPolicy.BACKTRACKING, FoldingPolicy.NONE, 3, true);
        
        // [0.4136244989486966 5, 0.4615541189612562 6, 0.9410194257801392 19, 0.9131614916580988 18]
        
        assertTrue(result.isSuccess());
        assertTrue(result.getPathLength() == 4);
        assertTrue(result.getTravelLength() == 4);
        assertTrue(result.getRoutingPath().get(0).index == 5);
        assertTrue(result.getRoutingPath().get(1).index == 6);
        assertTrue(result.getRoutingPath().get(2).index == 19);
        assertTrue(result.getRoutingPath().get(3).index == 18);
    }

}

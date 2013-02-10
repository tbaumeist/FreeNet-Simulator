package test.org.freenetproject.routing_simulator.graph;

import java.io.File;

import junit.framework.Assert;

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
                "--graph-size", "2000", "--route", "144000", "--route-hops",
                "18", "--route-bootstrap", "--route-output",
                tmpPath + "route.dat", "--graph-save", tmpPath + "graph.g" };
        new RoutingSim().run(Arguments.parse(args));
        Assert.assertTrue(true);
    }

    @Test
    public void visitedLength() throws Exception {
        // special graph file that I already know certain properties about
        File gmlFile = new File(Test_Helper.getResourcePath("gml-graph-1.gml"));
        final Graph graph = Test_Helper.readFromFileGml(gmlFile);

        SimpleNode nodeA = graph.getNode(0);
        SimpleNode nodeC = graph.getNode(2);

        assert nodeA.getLocation() == 0.1;
        assert nodeC.getLocation() == 0.3;

        int maxHTL = 6;
        RouteResult result = nodeA.route(nodeC, maxHTL,
                RoutingPolicy.BACKTRACKING, FoldingPolicy.NONE, 1, true);
        // should take the path 0.1 > 0.5 > 0.2 > 0.7 < 0.2 < 0.5 < 0.1 > 0.6 >
        // 0.3
        // > means routed to new node
        // < means had to back track
        assert result.isSuccess();
        assert result.getPathLength() == 3;
        assert result.getTravelLength() == 6;

        SimpleNode nodeH = graph.getNode(7);
        assert nodeH.getLocation() == 0.8;
        maxHTL = 7;
        result = nodeA.route(nodeH, maxHTL, RoutingPolicy.BACKTRACKING,
                FoldingPolicy.NONE, 1, true);
        assert !result.isSuccess();
        assert result.getTravelLength() == maxHTL;

        maxHTL = 8;
        result = nodeA.route(nodeH, maxHTL, RoutingPolicy.BACKTRACKING,
                FoldingPolicy.NONE, 1, true);
        // path .1 > .7 > .2 > .5 < .2 < .7 < .1 > .6 > .4 > .9 > .8
        assert result.isSuccess();
        assert result.getTravelLength() == 8;
        assert result.getPathLength() == 5;

        // look ahead of 2
        result = nodeA.route(nodeH, maxHTL, RoutingPolicy.BACKTRACKING,
                FoldingPolicy.NONE, 2, true);
        // path .1 > .7 > .2 > .5 < .2 < .7 < .1 > .6 > .4 > .9 > .8
        assert result.isSuccess();
        assert result.getTravelLength() == 7;
        assert result.getPathLength() == 4;

        // look ahead of 3
        result = nodeA.route(nodeH, maxHTL, RoutingPolicy.BACKTRACKING,
                FoldingPolicy.NONE, 3, true);
        // path .1 > .7 > .2 > .5 < .2 < .7 < .1 > .6 > .4 > .9 > .8
        assert result.isSuccess();
        assert result.getTravelLength() == 4;
        assert result.getPathLength() == 4;

    }

}

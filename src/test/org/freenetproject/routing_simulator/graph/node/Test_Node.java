package test.org.freenetproject.routing_simulator.graph.node;

import static org.junit.Assert.*;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.graph.Graph;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.junit.Test;

import test.org.freenetproject.routing_simulator.TestingBase;
import test.org.freenetproject.routing_simulator.graph.Test_Graph;

/**
 * Tests node equality,
 */
public class Test_Node extends TestingBase {

    @Test
    public void testEquality() {
        final Graph graph = Test_Graph.generateKleinberg();
        final SimpleNode node = graph.getNode(0);
        final SimpleNode second = graph.getNode(1);

        // Reflexive.
        assertTrue( node.equals(node));

        // Symmetric within the class.
        assertTrue( !node.equals(second));
        assertTrue( !second.equals(node));

        // Symmetric with other classes.
        final Object testObject = new Object();
        assertTrue( !node.equals(testObject));
        assertTrue( !testObject.equals(node));

        // Not equal to null.
        assertTrue( !node.equals(null));
    }

    @Test
    public void testDisconnectCandidate() {
        RandomGenerator random = new MersenneTwister(0);

        // Manually make some nodes - easier to follow than graph generation.
        SimpleNode A = new SimpleNode(0.1, random, 0, 0);
        SimpleNode B = new SimpleNode(0.2, random, 0, 1);
        SimpleNode C = new SimpleNode(0.3, random, 0, 2);

        // Connect in a triangle.
        A.connect(C);
        A.connect(B);
        B.connect(C);

        // In A's LRU queue promote C; not B.
        A.getLRUQueue().push(C);

        /*
         * The LRU queue should not be modified by disconnectCandidate().
         */
        SimpleNode before[] = new SimpleNode[A.getLRUQueue().size()];
        A.getLRUQueue().toArrayOrdered(before);

        // B was not promoted, and so should be the least recently used.
        assertTrue( A.disconnectCandidate().equals(B));

        SimpleNode after[] = new SimpleNode[A.getLRUQueue().size()];
        A.getLRUQueue().toArrayOrdered(after);

        assertTrue( before.length == after.length);
        for (int i = 0; i < before.length; i++)
            assertTrue( before[i].equals(after[i]));
    }
}

package org.freenetproject.routing_simulator.experiment;

import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.ArrayUtil;

public class RoutingExp {
    private int successes = 0, disconnectedFolding = 0,
            disconnectedBootstrap = 0, totalSuccessPathLength = 0,
            totalSuccessTravelPathLength = 0, nRequests = 0,
            foldingOperations = 0, maxHTL = 0;
    private int[] pathLengthDist;

    public RoutingExp(int maxHTL, int nRequests) {
        this.maxHTL = maxHTL;
        this.pathLengthDist = new int[maxHTL + 1];
        this.nRequests = nRequests;
    }

    public void record(boolean successful, int pathLength,
            int traveledPathLength) {
        if (successful) {
            this.successes++;
            this.pathLengthDist[pathLength]++;
            this.totalSuccessPathLength += pathLength;
            this.totalSuccessTravelPathLength += traveledPathLength;
        }
    }

    public void disconnectedFolding(int count) {
        this.disconnectedFolding += count;
    }

    public void disconnectBootStrap() {
        this.disconnectedBootstrap++;
    }

    public void foldingOperations(int operations) {
        this.foldingOperations += operations;
    }

    public String toStringHeaders() {
        StringBuilder b = new StringBuilder();
        b.append("routingSuccessRate ");
        b.append("meanSuccessfulDiscoveryRoutingPathLength ");
        b.append("meanSuccessfulRoutingPathLength ");
        b.append("successfulRoutingPathLengthStdDev ");
        b.append("successfulRoutingPathLengthDistribution ");
        return b.toString();
    }

    public String toStringValues() {
        StringBuilder b = new StringBuilder();
        b.append((double) successes / nRequests * 100).append(' ');
        b.append((double) totalSuccessTravelPathLength / successes).append(' ');
        b.append((double) totalSuccessPathLength / successes).append(' ');
        b.append(this.stdDevPathLengths()).append(' ');
        b.append(ArrayUtil.stringArrayPair(this.pathLengthDist)).append(' ');
        return b.toString();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Routing simulation results\n\n");

        b.append("Disconnected from folding :     \t ").append(
                disconnectedFolding);
        b.append("\n");
        b.append("Disconnected from bootstrapping : \t").append(
                disconnectedBootstrap);
        b.append("\n");
        b.append("Path folding operations :         \t").append(
                this.foldingOperations);
        b.append("\n");
        b.append("Routing success rate :            \t").append(
                (double) successes / nRequests * 100);
        b.append("%\n");
        b.append("Routing requests count :          \t").append(nRequests);
        b.append("\n");
        b.append("\tSuccessful routing request count : \t").append(successes);
        b.append("\n");
        b.append("\tFailed routing request count :     \t").append(
                nRequests - successes);
        b.append("\n");

        b.append("\n* Note failed requests are not included in the stats below *\n\n");
        b.append("Maximum path length :             \t").append(this.maxHTL);
        b.append("\n");
        b.append(
                "Mean successful discovery routing path length (back-tracking):    \t")
                .append((double) totalSuccessTravelPathLength / successes);
        b.append("\n");
        b.append("Mean successful routing path length :    \t").append(
                (double) totalSuccessPathLength / successes);
        b.append("\n");
        b.append("Successful routing path length std-dev :    \t").append(
                this.stdDevPathLengths());
        b.append("\n");
        b.append("\n");

        b.append("Successful Routing Path Length Distribution (Length:Count)\n");
        b.append(ArrayUtil.stringArrayPair(this.pathLengthDist));

        return b.toString();
    }
    
    public double stdDevPathLengths() {
        long sumLengths = 0;
        long sumSquareLengths = 0;
        long n = 0;
        
        for (int length = 0; length < this.pathLengthDist.length; length++) {
            int count = this.pathLengthDist[length];
            n += count;
            sumLengths += (length * count);
            sumSquareLengths += (length * length) * count;
        }
        
        if (n == 0)
            return 0;

        double variance = ((double) sumSquareLengths) / ((double) n)
                - ((double) (sumLengths * sumLengths)) / ((double) (n * n));
        return Math.sqrt(variance);
    }
}

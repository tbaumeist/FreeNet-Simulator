package org.freenetproject.routing_simulator;

import java.util.ArrayList;
import java.util.List;

import org.freenetproject.routing_simulator.graph.folding.PathFoldingResult;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;

/**
 * Gives details on the results of routing a request.
 */
public class RouteResult {

    /**
     * List of nodes caused to have zero degree by path folding.
     */
    private final PathFoldingResult foldingResult;
    /**
     * True if and only if the routing arrived at its exact target.
     */
    private final boolean success;
    /**
     * The routing path taken.
     */
    private final List<SimpleNode> routingPath;
    /**
     * The the total path length taken including any back tracking.
     */
    private final int traveledLength;

    /**
     * Construct a new routing result for a failed routing attempt.
     * 
     * @param pathLength
     *            Length of the path.
     */
    public RouteResult(final int pathLength) {
        this(false, new PathFoldingResult(), new ArrayList<SimpleNode>(),
                pathLength);
    }

    /**
     * Construct a new routing result.
     * 
     * @param success
     *            Was routing successful.
     * @param foldingResult
     *            Path folding results produced from routing.
     * @param pathLength
     *            Length of the path.
     */
    public RouteResult(final boolean success,
            final PathFoldingResult foldingResult,
            final List<SimpleNode> routingPath, final int traveledLength) {
        this.success = success;
        this.foldingResult = foldingResult;
        this.routingPath = routingPath;
        this.traveledLength = traveledLength;
    }

    /**
     * @return the folding result
     */
    public final PathFoldingResult getFoldingResult() {
        return foldingResult;
    }

    /**
     * @return the success
     */
    public final boolean isSuccess() {
        return success;
    }

    /**
     * @return the path length
     */
    public final int getPathLength() {
        return this.routingPath.size();
    }

    public final List<SimpleNode> getRoutingPath() {
        return this.routingPath;
    }

    /**
     * @return the traveled path length
     */
    public final int getTravelLength() {
        return this.traveledLength;
    }
}

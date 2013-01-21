package org.freenetproject.routing_simulator;

import org.freenetproject.routing_simulator.graph.folding.PathFoldingResult;

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
     * The length of the path taken, valid if and only if routing was
     * successful.
     */
    private final int pathLength;

    /**
     * Construct a new routing result.
     * 
     * @param success
     *            Was routing successful.
     * @param pathLength
     *            Length of the path.
     */
    public RouteResult(final boolean success, final int pathLength) {
        this(success, new PathFoldingResult(), pathLength);
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
            final PathFoldingResult foldingResult, final int pathLength) {
        this.success = success;
        this.foldingResult = foldingResult;
        this.pathLength = pathLength;
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
        return pathLength;
    }
}

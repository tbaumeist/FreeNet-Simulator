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
	 * The the total path length taken including any back tracking.
	 */
	private final int traveledLength;

	/**
	 * Construct a new routing result for a failed routing attempt.
	 * 
	 * @param success
	 *            Was routing successful.
	 * @param pathLength
	 *            Length of the path.
	 */
	public RouteResult(final int pathLength) {
		this(false, new PathFoldingResult(), pathLength, pathLength);
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
			final PathFoldingResult foldingResult, final int pathLength,
			final int traveledLength) {
		this.success = success;
		this.foldingResult = foldingResult;
		this.pathLength = pathLength;
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
		return pathLength;
	}
	
	/**
	 * @return the traveled path length
	 */
	public final int getTravelLength() {
		return this.traveledLength;
	}
}

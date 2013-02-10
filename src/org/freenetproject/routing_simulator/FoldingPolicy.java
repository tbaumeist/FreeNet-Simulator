package org.freenetproject.routing_simulator;

/**
 * Path folding policies.
 */
public enum FoldingPolicy {
    /**
     * Do not perform path folding.
     */
    NONE,
    /**
     * Old: Path fold with 7% acceptance - each node along the chain, multiple
     * times. New: Path fold when nodes have open spots in peer table.
     */
    FREENET,
    /**
     * Path fold to endpoint only. Undirected network with lattice edges.
     */
    SANDBERG,
    /**
     * Path fold to endpoint only. Directed network with lattice edges.
     */
    SANDBERG_DIRECTED,
    /**
     * Path fold to endpoint only. Undirected network with no lattice edges.
     */
    SANDBERG_NO_LATTICE
}

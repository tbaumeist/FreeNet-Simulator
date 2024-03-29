package org.freenetproject.routing_simulator.graph;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;
import org.freenetproject.routing_simulator.graph.degree.DegreeSource;
import org.freenetproject.routing_simulator.graph.degree.PoissonDegreeSource;
import org.freenetproject.routing_simulator.graph.linklength.KleinbergLinkSource;
import org.freenetproject.routing_simulator.graph.linklength.LinkLengthSource;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.logging.SimLogger;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphModel;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;

import frp.dataFileReaders.TopologyFileReaderDOT;
import frp.dataFileReaders.TopologyFileReaderGML;
import frp.gephi.GephiHelper;
import frp.routing.Node;
import frp.routing.Topology;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class to represent and generate graphs of small world networks. At present
 * only limited Kleinberg graphs are generated. Functions to evaluate the graph
 * topology are also provided.
 */
public class Graph {
    private final static Logger LOGGER = Logger
            .getLogger(Graph.class.getName());
    private final ArrayList<SimpleNode> nodes;

    /**
     * Probability of stopping an attempt to connect a node to its maximum
     * desired degree
     */
    private static final double rejectProbability = 0.05;

    private double diameter = -1;
    private double avgPathLength = -1;

    /**
     * Private constructor; call one of the generator functions instead.
     * 
     * @param nodes
     *            The nodes which make up the network.
     * @see
     */
    private Graph(ArrayList<SimpleNode> nodes) {
        this.nodes = nodes;
    }

    public static ArrayList<SimpleNode> generateNodes(final int nNodes,
            final RandomGenerator rand, boolean fastGeneration,
            DegreeSource source) {
        double[] locations = new double[nNodes];
        if (fastGeneration) {
            for (int i = 0; i < nNodes; i++)
                locations[i] = (1.0 * i) / nNodes;
        } else {
            for (int i = 0; i < nNodes; i++)
                locations[i] = rand.nextDouble();
        }

        // Increasing index should also mean increasing location.
        Arrays.sort(locations);

        final ArrayList<SimpleNode> nodes = new ArrayList<SimpleNode>(nNodes);

        for (int i = 0; i < nNodes; i++) {
            nodes.add(new SimpleNode(locations[i], rand, source.getDegree(), i));
        }

        return nodes;
    }

    /**
     * Connects a directed graph with lattice links between X and X - 1 mod N.
     * Each node has a single shortcut edge with an endpoint determined by the
     * link length source.
     * 
     * @param nodes
     *            Nodes which make up the network.
     * @param linkLengthSource
     *            Provides shortcut endpoints.
     * @param shortcuts
     *            Number of shortcut edges. Must be non-negative.
     */
    public static Graph connectSandberg(ArrayList<SimpleNode> nodes,
            int shortcuts, LinkLengthSource linkLengthSource) {
        Graph g = new Graph(nodes);

        // Base graph of lattice edges: Edge from X to X - 1 mod N for all nodes
        // 0 to N - 1.
        g.addLatticeLinks(true);

        // Shortcuts: Edges from each node to an endpoint.
        for (SimpleNode origin : g.nodes) {
            SimpleNode endpoint;
            // -1 to account for the single lattice edge.
            while (origin.degree() - 1 < shortcuts) {
                do {
                    endpoint = linkLengthSource.getPeer(origin);
                } while (origin.isConnected(endpoint));
                origin.connectOutgoing(endpoint);
            }
        }

        return g;
    }

    /**
     * Adds lattice links. Should be the first thing to add edges to a network.
     * 
     * Connects node X to X - 1 mod N for all X, where X is node index and N is
     * the network size.
     * 
     * @param directed
     *            If true, the lattice links will be directed. If false,
     *            undirected.
     */
    private void addLatticeLinks(final boolean directed) {
        assert nEdges() == 0;

        for (int i = 0; i < size(); i++) {
            /*
             * From X to X - 1, wrapped to the network size. Java implements
             * modulus such that it produces -1 for -1 % N, not N - 1 as it does
             * in the definition of the lattice links. Going from X + 1 to X is
             * equivalent.
             */
            final SimpleNode from = getNode((i + 1) % size());
            final SimpleNode to = getNode(i);
            if (from.isConnected(to) || to.isConnected(from)) {
                throw new IllegalStateException("Connection already existed.");
            }

            if (directed)
                from.connectOutgoing(to);
            else
                from.connect(to);
        }

        // There is an edge between every node in a circle.
        assert nEdges() == size();
    }

    /**
     * Gives the node in question random connections until it meets its desired
     * degree. Does not add disconnected peers.
     * 
     * @param node
     *            node to add connections to.
     * @param random
     *            source of entropy for selecting which nodes to connect to.
     */
    public List<SimpleNode> bootstrap(final SimpleNode node,
            final RandomGenerator random) {
        List<SimpleNode> disconnectedNodes = new ArrayList<SimpleNode>();
        SimpleNode peer;
        do {
            peer = getNode(random.nextInt(size()));

            /*
             * Do not connect to self - reference comparison should be
             * sufficient, or make a duplicate connection. Avoid connecting to
             * disconnected nodes lest it fragment the network.
             */
            if (node == peer || node.isConnected(peer) || peer.degree() == 0)
                continue;

            // Reference comparison should be sufficient.
            assert !node.equals(peer);

            /*
             * If the peer is already at its degree, connect only if not
             * rejected. Drop a random connection to keep the connection count
             * invariant.
             */
            if (!peer.atDegree() || peer.atDegree()
                    && random.nextDouble() > rejectProbability) {
                SimpleNode disconnected = peer.swapConnections(node);
                if (disconnected.degree() == 0)
                    disconnectedNodes.add(disconnected);
            }
        } while (!node.atDegree());

        return disconnectedNodes;
    }

    /**
     * Connects a graph such that all nodes have a single (non-lattice, if
     * possible) undirected connection to a single super node. Ignores nodes'
     * desired degree.
     * 
     * @param nodes
     *            Nodes which make up the network.
     * @param lattice
     *            If true, adds lattice edges. If false, does not.
     * 
     * @return Graph with the specified edges added.
     */
    public static Graph connectSuperNode(ArrayList<SimpleNode> nodes,
            boolean lattice) {
        Graph graph = new Graph(nodes);
        assert nodes.size() > 1;
        if (lattice)
            graph.addLatticeLinks(false);

        final SimpleNode superNode = graph.getNode(0);
        for (int i = 1; i < graph.size(); i++) {
            SimpleNode peer = graph.getNode(i);
            if (!superNode.isConnected(peer))
                superNode.connect(peer);
        }

        return graph;
    }

    /**
     * Adds links to a graph which conform to the link length distribution and
     * peer count distribution given.
     * 
     * @param g
     *            Graph to add edges to.
     * @param rand
     *            Provides probabilities for whether to connect to a node which
     *            is already at its desired degree.
     * @param linkLengthSource
     *            Provides peers which give conforming connections.
     * 
     * @return Graph with specified edges added.
     */
    public static Graph connectGraph(Graph g, RandomGenerator rand,
            LinkLengthSource linkLengthSource) {

        // TODO: Modifications to this have caused an increase in resources
        // required to run simulator
        SimpleNode destination;
        for (SimpleNode src : g.nodes) {
            if (src.atDegree())
                continue;

            // Make connections until at desired degree.
            double stopProbStep = rejectProbability / src.getDesiredDegree();
            double stopProb = 0;
            // Random probability to stop trying (increases the more we try)
            while (!src.atDegree() && rand.nextDouble() > stopProb) {
                stopProb += stopProbStep;
                destination = linkLengthSource.getPeer(src);
                if (src == destination || src.isConnected(destination)
                        || destination.atDegree())
                    continue;
                src.connect(destination);
            }
        }

        return g;
    }

    public static Graph connectGraph(ArrayList<SimpleNode> nodes,
            RandomGenerator rand, LinkLengthSource linkLengthSource,
            boolean lattice) {
        Graph graph = new Graph(nodes);
        if (lattice)
            graph.addLatticeLinks(false);
        return Graph.connectGraph(graph, rand, linkLengthSource);
    }

    /**
     * Writes graph to a file. Format:
     * <ul>
     * <li>Number of nodes.</li>
     * <li>SimpleNodes.</li>
     * <li>Connections: index from, index to</li>
     * </ul>
     * 
     * @param output
     *            stream to write graph to.
     * @throws Exception
     */
    public void write(DataOutputStream output) throws Exception {
        try {
            // Number of nodes.
            output.writeInt(nodes.size());

            // Nodes.
            for (SimpleNode node : nodes)
                node.write(output);

            /*
             * Write every connection; undirected edges are two directed edges.
             */
            final ArrayList<Integer> connectionIndexes = new ArrayList<Integer>();
            int writtenConnections = 0;
            for (SimpleNode from : nodes) {
                for (SimpleNode to : from.getConnections()) {
                    writtenConnections++;
                    connectionIndexes.add(from.index);
                    connectionIndexes.add(to.index);
                }
            }

            output.writeInt(writtenConnections);
            LOGGER.info("Writing " + writtenConnections
                    + " connections to output stream.");
            assert connectionIndexes.size() == writtenConnections * 2;
            for (Integer index : connectionIndexes) {
                output.writeInt(index);
            }

            output.flush();
            output.close();
            
        } catch (IOException e) {
            throw new Exception("Could not write graph to output stream:");
        }
    }

    /**
     * Writes graph to a file. DOT format: "Node_A_Location Node_A_ID" ->
     * "Node_B_Location Node_B_ID"
     * 
     * @param output
     *            stream to write graph to.
     * @throws Exception
     */
    public void writeDot(OutputStream output) throws Exception {
        try {
            /*
             * Write every connection; undirected edges are two directed edges.
             */
            final ArrayList<SimpleNode[]> connectionIndexes = new ArrayList<SimpleNode[]>();
            for (SimpleNode from : nodes) {
                for (SimpleNode to : from.getConnections()) {
                    SimpleNode[] connection = new SimpleNode[2];
                    connection[0] = from;
                    connection[1] = to;
                    connectionIndexes.add(connection);
                }
            }

            LOGGER.info("Writing " + connectionIndexes.size()
                    + " connections to output stream.");

            output.write("digraph G {\n".getBytes("UTF-8"));
            for (SimpleNode[] pair : connectionIndexes) {
                output.write(("\"" + pair[0].getLocation() + " "
                        + pair[0].index + "\" -> \"" + pair[1].getLocation()
                        + " " + pair[1].index + "\"\n").getBytes());
            }
            output.write("}\n".getBytes("UTF-8"));

            output.flush();
            output.close();
        } catch (Exception e) {
            throw new Exception("Could not write DOT graph to output stream:");
        }
    }
    
    public void writeDot_Wait(FileOutputStream file) throws Exception {
        try {
            writeDot(new DataOutputStream(file));
            file.flush();
            file.getFD().sync();
            file.close();
        } catch (Exception e) {
            throw new Exception("Could not write DOT graph to output stream:");
        }
    }

    /**
     * Load graph from DOT file.
     * 
     * @param input
     *            DOT file stream
     * @param random
     *            Random generator
     * @return loaded graph
     * @throws Exception
     *             Error loading the graph
     */
    public static Graph readDot(InputStream input, RandomGenerator random)
            throws Exception {
        TopologyFileReaderDOT topReader = new TopologyFileReaderDOT();
        Topology top = topReader.readFromFile(input);
        if (top == null)
            throw new Exception("Unable to read the input graph file.");

        return convertTopologyToGraph(top, random);
    }

    /**
     * Load graph from GML file.
     * 
     * @param input
     *            DOT file stream
     * @param random
     *            Random generator
     * @return loaded graph
     * @throws Exception
     *             Error loading the graph
     */
    public static Graph readGml(InputStream input, RandomGenerator random)
            throws Exception {
        TopologyFileReaderGML topReader = new TopologyFileReaderGML();
        Topology top = topReader.readFromFile(input);
        if (top == null)
            throw new Exception("Unable to read the input graph file.");

        return convertTopologyToGraph(top, random);
    }

    /**
     * Converts a topology object into a graph object
     * 
     * @param top
     *            The topology
     * @param random
     *            Random generator
     * @return converted graph
     */
    private static Graph convertTopologyToGraph(Topology top,
            RandomGenerator random) {
        // Number of nodes.
        final List<Node> topNodes = top.getAllNodes();
        final int networkSize = topNodes.size();
        final Graph graph = new Graph(new ArrayList<SimpleNode>(networkSize));

        int largestDegree = 0;
        for (Node n : topNodes) {
            int nodeDegree = n.getDirectNeighbors().size();
            if (nodeDegree > largestDegree) {
                largestDegree = nodeDegree;
            }
        }

        // Nodes.
        for (int i = 0; i < networkSize; i++) {
            Node n = topNodes.get(i);
            graph.nodes.add(new SimpleNode(n.getLocation(), random,
                    largestDegree, i));
        }

        // Connections
        for (int i = 0; i < networkSize; i++) {
            Node n = topNodes.get(i);
            for (Node peer : n.getDirectNeighbors()) {
                int peerIndex = topNodes.indexOf(peer);
                graph.nodes.get(i).connectOutgoing(graph.nodes.get(peerIndex));
            }
        }

        return graph;
    }

    /**
     * Constructs the graph from a file.
     * 
     * @param input
     *            stream to read the graph from.
     * @param random
     *            Randomness source to give to nodes.
     * @return graph defined by the file.
     * @throws Exception
     */
    public static Graph read(DataInputStream input, RandomGenerator random)
            throws Exception {
        try {
            // Number of nodes.
            final int networkSize = input.readInt();
            final Graph graph = new Graph(
                    new ArrayList<SimpleNode>(networkSize));

            // Nodes.
            for (int i = 0; i < networkSize; i++) {
                graph.nodes.add(new SimpleNode(input, i, random));
            }

            final int writtenConnections = input.readInt();
            LOGGER.info("Reading " + writtenConnections + " connections.");
            // Each connection consists of two indexes in a pair.
            for (int i = 0; i < writtenConnections; i++) {
                final int from = input.readInt();
                final int to = input.readInt();
                graph.nodes.get(from).connectOutgoing(graph.nodes.get(to));
            }

            return graph;
        } catch (IOException e) {
            throw new Exception("Could not read graph from input stream:");
        }
    }

    /**
     * Get a node by index.
     * 
     * @param i
     *            Index of node to get
     * @return Node at index i
     */
    public SimpleNode getNode(int i) {
        return nodes.get(i);
    }

    /**
     * Print some topology statistics.
     * 
     * @throws Exception
     */
    public String printGraphStats() throws Exception {
        int nEdges = nEdges();
        double meanDegree = ((double) (2 * nEdges)) / nodes.size();

        StringBuilder b = new StringBuilder();
        b.append("Graph stats:");
        b.append("\nSize:					" + size());
        b.append("\nEdges:					" + nEdges);
        b.append("\nMin degree:				" + minDegree());
        b.append("\nMax degree:				" + maxDegree());
        b.append("\nMean degree:				" + meanDegree);
        b.append("\nDegree stddev:				" + Math.sqrt(degreeVariance()));
        b.append("\nNetwork diameter:			" + this.getNetworkDiameter());
        b.append("\nAverage path length:			" + this.getAveragePathLength());
        b.append("\nMean local clustering coefficient:	"
                + meanLocalClusterCoeff());
        b.append("\nGlobal clustering coefficient:		" + globalClusterCoeff());
        return b.toString();
    }

    public String toStringHeaders() {
        StringBuilder b = new StringBuilder();
        b.append("size ");
        b.append("edges ");
        b.append("minDegree ");
        b.append("maxDegree ");
        b.append("meanDegree ");
        b.append("degreeStddev ");
        b.append("networkDiameter ");
        b.append("averagePathLength ");
        b.append("meanLocalClusteringCoefficient ");
        b.append("globalClusteringCoefficient ");
        return b.toString();
    }
    
    public double getMeanDegree() {
        return ((double) (2 * this.nEdges())) / nodes.size();
    }

    public String toStringValues() throws Exception {
        StringBuilder b = new StringBuilder();
        b.append(this.size()).append(' ');
        b.append(this.nEdges()).append(' ');
        b.append(this.minDegree()).append(' ');
        b.append(this.maxDegree()).append(' ');
        b.append(this.getMeanDegree()).append(' ');
        b.append(Math.sqrt(degreeVariance())).append(' ');
        b.append(this.getNetworkDiameter()).append(' ');
        b.append(this.getAveragePathLength()).append(' ');
        b.append(this.meanLocalClusterCoeff()).append(' ');
        b.append(this.globalClusterCoeff()).append(' ');

        return b.toString();
    }

    /**
     * Edge length distribution. Treats edges as directed.
     * 
     * @param includeLatticeLinks
     *            If true, links from nodes with adjacent indexes will be
     *            included. If false they will not.
     */
    public ArrayList<Double> edgeLengths(final boolean excludeLatticeLinks) {
        ArrayList<Double> lengths = new ArrayList<Double>();
        for (SimpleNode node : nodes) {
            for (SimpleNode peer : node.getConnections()) {
                if (excludeLatticeLinks) {
                    if (node.index == (peer.index + 1) % size()
                            || peer.index == (node.index + 1) % size())
                        continue;
                }
                lengths.add(node.distanceToLoc(peer.getLocation()));
            }
        }
        return lengths;
    }

    /**
     * Get the number of nodes in this graph.
     * 
     * @return Size of the graph
     */
    public int size() {
        return nodes.size();
    }

    public void updateGraphStats() throws Exception {
        /*
         * Hack to hide system.out prints from Gephi library
         */
        PrintStream original = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // DO NOTHING
            }
        }));

        // Gephi stats
        ByteArrayOutputStream dotOutput = new ByteArrayOutputStream();
        this.writeDot(dotOutput);
        DirectedGraph graph = new GephiHelper().loadGraphFile(
                new ByteArrayInputStream(dotOutput.toByteArray()), ".dot");
        AttributeModel attributeModel = Lookup.getDefault()
                .lookup(AttributeController.class).getModel();
        GraphModel graphModel = graph.getGraphModel();
        GraphDistance distance = new GraphDistance();
        distance.execute(graphModel, attributeModel);
        this.diameter = distance.getDiameter();
        this.avgPathLength = distance.getPathLength();

        /*
         * Restore system.out
         */
        System.setOut(original);
    }

    public double getNetworkDiameter() throws Exception {
        return this.diameter;
    }

    public double getAveragePathLength() throws Exception {
        return this.avgPathLength;
    }

    /**
     * Count edges in this graph.
     * 
     * @return Total number of edges
     */
    public int nEdges() {
        // Indexes in <lesser, greater> order of a connection.
        HashSet<Pair<Integer, Integer>> connections = new HashSet<Pair<Integer, Integer>>();
        int undirected = 0;

        for (SimpleNode origin : nodes) {
            for (SimpleNode peer : origin.getConnections()) {
                /*
                 * If the set already contained the element the connection is
                 * two mutual directed edges, which in this case is considered
                 * one directed edge.
                 */
                if (origin.index < peer.index) {
                    if (!connections.add(new Pair<Integer, Integer>(
                            origin.index, peer.index)))
                        undirected++;
                } else {
                    if (!connections.add(new Pair<Integer, Integer>(peer.index,
                            origin.index)))
                        undirected++;
                }
            }
        }

        // LOGGER.fine("Out of the edges " + undirected + " are undirected.");
        return connections.size();
    }

    /**
     * Find the minimum degree of any node in the graph.
     * 
     * @return Minimum node degree
     */
    public int minDegree() {
        int n = size();
        if (n == 0)
            return 0;
        int min = nodes.get(0).degree();
        for (int i = 1; i < n; i++) {
            min = Math.min(min, nodes.get(i).degree());
        }
        return min;
    }

    /**
     * Find the maximum degree of any node in the graph.
     * 
     * @return Maximum node degree
     */
    public int maxDegree() {
        int n = size();
        if (n == 0)
            return 0;
        int max = nodes.get(0).degree();
        for (int i = 1; i < n; i++) {
            max = Math.max(max, nodes.get(i).degree());
        }
        return max;
    }

    /**
     * Compute the variance in the degree of the nodes.
     * 
     * @return Variance of node degree
     */
    public double degreeVariance() {
        long sumDegrees = 0;
        long sumSquareDegrees = 0;
        long n = nodes.size();
        if (n == 0)
            return 0;
        for (SimpleNode node : nodes) {
            int d = node.degree();
            sumDegrees += d;
            sumSquareDegrees += d * d;
        }

        return ((double) sumSquareDegrees) / ((double) n)
                - ((double) (sumDegrees * sumDegrees)) / ((double) (n * n));
    }

    /**
     * Calculate the mean clustering coefficients of nodes in the graph. See
     * http://en.wikipedia.org/wiki/Clustering_coefficient This is *not* the
     * same as the global clustering coefficient described there; this is the
     * unweighted mean of the local coefficients, which gives undue weight to
     * low-degree nodes.
     * 
     * @return Mean local clustering coefficient
     */
    public double meanLocalClusterCoeff() {
        double sumCoeff = 0.0;
        int n = nodes.size();
        if (n == 0)
            return 0;
        for (SimpleNode node : nodes) {
            sumCoeff += node.localClusterCoeff();
        }
        double mean = sumCoeff / n;
        assert mean >= 0.0 && mean <= 1.0;
        return mean;
    }

    public int[] degrees() {
        int n = nodes.size();
        int[] d = new int[n];
        for (int i = 0; i < n; i++)
            d[i] = nodes.get(i).degree();
        return d;
    }

    /**
     * Calculate the global clustering coefficient. See
     * http://en.wikipedia.org/wiki/Clustering_coefficient
     * 
     * @return Global clustering coefficient
     */
    private double globalClusterCoeff() {
        int nClosed = 0;
        int nTotal = 0;

        for (SimpleNode n : nodes) {
            int degree = n.degree();
            nClosed += n.closedTriplets();
            nTotal += (degree * (degree - 1)) / 2;
        }

        return ((double) (nClosed)) / ((double) (nTotal));
    }

    private int[] randomWalkDistTest(int nWalks, int hopsPerWalk,
            boolean uniform, RandomGenerator rand) {
        int[] choiceFreq = new int[size()];
        int dupCount = 0;
        for (int i = 0; i < nWalks; i++) {
            SimpleNode origin = nodes.get(rand.nextInt(size()));
            SimpleNode dest = origin.randomWalk(hopsPerWalk, uniform, rand);
            choiceFreq[dest.index]++;
            if (origin == dest)
                dupCount++;
        }
        LOGGER.info("Origin selected as dest on " + dupCount + " walks out of "
                + nWalks);
        return choiceFreq;
    }

    /**
     * Create some graphs; test them for statistical properties of interest.
     */
    public static void main(String[] args) {
        try {
            SimLogger.setup();

            int nNodes = 4000;

            int nWalks = 10 * 1000 * 1000;
            int nBuckets = 400;
            int hopsPerWalkUniform = 20;
            int hopsPerWalkCorrected = 40;

            int nTrials = 4;
            int[][][] pdfs = new int[nTrials][3][nBuckets];

            for (int trial = 0; trial < nTrials; trial++) {
                LOGGER.info("Creating test graph...");
                RandomGenerator rand = new MersenneTwister(trial);
                final ArrayList<SimpleNode> nodes = Graph.generateNodes(nNodes,
                        rand, true, new PoissonDegreeSource(12));
                Graph g = connectGraph(nodes, rand, new KleinbergLinkSource(
                        rand, nodes), false);
                LOGGER.info(g.printGraphStats());
                int[] uniformWalkDist;
                int[] weightedWalkDist;
                int[] refDist = new int[nNodes];
                LOGGER.info("Computing reference distribution...");
                for (int i = 0; i < nWalks; i++)
                    refDist[rand.nextInt(nNodes)]++;
                LOGGER.info("Computing uniform walks...");
                uniformWalkDist = g.randomWalkDistTest(nWalks,
                        hopsPerWalkUniform, true, rand);
                LOGGER.info("Computing weighted walks...");
                weightedWalkDist = g.randomWalkDistTest(nWalks,
                        hopsPerWalkCorrected, false, rand);

                Arrays.sort(refDist);
                Arrays.sort(uniformWalkDist);
                Arrays.sort(weightedWalkDist);
                int nodesPerBucket = nNodes / nBuckets;
                assert nBuckets * nodesPerBucket == nNodes;

                for (int i = 0; i < nNodes; i++) {
                    pdfs[trial][0][i / nodesPerBucket] += refDist[i];
                    pdfs[trial][1][i / nodesPerBucket] += uniformWalkDist[i];
                    pdfs[trial][2][i / nodesPerBucket] += weightedWalkDist[i];
                }
            }

            StringBuilder b = new StringBuilder("Distribution PDFs:\n");
            for (int i = 0; i < nTrials; i++) {
                b.append("Reference\tUniform\tWeighted\t");
            }
            b.append("\n");
            for (int i = 0; i < nBuckets; i++) {
                for (int trial = 0; trial < nTrials; trial++) {
                    b.append(pdfs[trial][0][i] + "\t" + pdfs[trial][1][i]
                            + "\t" + pdfs[trial][2][i] + "\t");
                }
                b.append("\n");
            }
            LOGGER.info(b.toString());
        } catch (Exception e) {
            LOGGER.severe("Error running Graph: " + e.getMessage());
        }
    }
}

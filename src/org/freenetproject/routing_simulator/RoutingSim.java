package org.freenetproject.routing_simulator;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.experiment.RoutingExp;
import org.freenetproject.routing_simulator.graph.Graph;
import org.freenetproject.routing_simulator.graph.linklength.LinkLengthSource;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.ArrayUtil;
import org.freenetproject.routing_simulator.util.logging.SimLogger;

import frp.utils.Progresser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Class to perform routing simulations on Graphs.
 */
public final class RoutingSim {
	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = Logger.getLogger(RoutingSim.class
			.getName());

	/**
	 * Main simulator program. Generate a set of graphs of different parameters,
	 * run a set of requests on them, and print assorted stats.
	 * 
	 * @param args
	 *            Command-line arguments.
	 */
	public static void main(final String[] args) {
		try {
			new RoutingSim().run(Arguments.parse(args));
		} catch (Exception e) {
			LOGGER.severe("Error running simulator: " + e.getMessage());
		}
	}

	/**
	 * Start the simulator.
	 * 
	 * @param arguments
	 *            Interpreted CLI parameters
	 * @throws Exception
	 *             Any errors while running the simulator will throw an
	 *             exception.
	 */
	public void run(final Arguments arguments) throws Exception {
		ByteArrayOutputStream memory;

		SimLogger.setup();

		if (arguments == null) {
			System.exit(1);
		}

		SimLogger.setup(arguments.logLevel);
		LOGGER.info(arguments.toString());

		if (arguments.pause) {
			LOGGER.severe("Press enter to continue...");
			Scanner scan = new Scanner(System.in);
			scan.nextLine();
			scan.close();
		}

		// Time tracking: report time taken for each graph setting upon
		// completion.
		long startTime = System.currentTimeMillis();
		long lastTime = startTime;

		// Load the graph; otherwise generate.
		Graph g = this.generateGraph(arguments);

		LOGGER.warning("Initial graph stats\n" + g.printGraphStats());
		memory = new ByteArrayOutputStream();
		this.writeDegreeOutput(g, memory);
		LOGGER.info("Initial degree distribution\nDegree Count\n"
				+ memory.toString());
		LOGGER.info("Graph generation took (ms): "
				+ (System.currentTimeMillis() - lastTime));
		lastTime = System.currentTimeMillis();

		if (arguments.runProbe) {
			// Re-initialize random number source so behavior here does not
			// depend on previous usage, only the seed.
			RandomGenerator rand = new MersenneTwister(arguments.seed);
			// Uniform probes if --metropolis-hastings is not specified.
			// TODO: Pass in checked directory.
			this.probeDistribution(g, rand, arguments.maxHopsProbe,
					arguments.outputProbe, !arguments.metropolisHastings);
		}

		if (arguments.runRoute) {
			RandomGenerator rand = new MersenneTwister(arguments.seed);
			this.simulate(g, rand, arguments);
		}

		LOGGER.warning("Final graph stats\n" + g.printGraphStats());

		this.writeDegreeOutput(g, arguments.degreeOutput);
		memory = new ByteArrayOutputStream();
		this.writeDegreeOutput(g, memory);
		LOGGER.info("Final degree distribution\nDegree Count\n"
				+ memory.toString());

		this.writeLinkOutput(g, arguments.linkOutput, arguments.excludeLattice);

		if (arguments.graphOutput != null) {
			g.write(arguments.graphOutput);
		}

		if (arguments.graphOutputText != null) {
			g.writeDot(arguments.graphOutputText);
		}

		LOGGER.info("Route/Probe time taken (ms): "
				+ (System.currentTimeMillis() - lastTime));
		LOGGER.info("Total time taken (ms): "
				+ (System.currentTimeMillis() - startTime));
	}

	/**
	 * Write the graph's link length distribution to an output stream.
	 * 
	 * @param graph
	 *            The graph to get the link lengths from.
	 * @param linkOutputStream
	 *            The output stream to write link lengths to.
	 * @param excludeLattice
	 *            If true don't write links that are part of the lattice.
	 * @throws IOException
	 *             Error writing to the output stream.
	 */
	private void writeLinkOutput(final Graph graph,
			final OutputStream linkOutputStream, final boolean excludeLattice)
			throws IOException {
		if (linkOutputStream == null) {
			return;
		}

		ArrayList<Double> lengths = graph.edgeLengths(excludeLattice);
		// Output is intended for gnuplot CDF - second value is Y and should
		// sum to 1.
		double normalized = 1.0 / lengths.size();
		for (double length : lengths) {
			linkOutputStream.write((length + " " + normalized + "\n")
					.getBytes("UTF-8"));
		}
	}

	/**
	 * Write the graph's degree distribution to an output stream.
	 * 
	 * @param graph
	 *            The graph to get the degree sizes from.
	 * @param degreeOutputStream
	 *            The output stream to write the degrees to.
	 * @throws IOException
	 *             Error writing to the output stream.
	 */
	private void writeDegreeOutput(final Graph graph,
			final OutputStream degreeOutputStream) throws IOException {
		if (degreeOutputStream == null) {
			return;
		}
		int[] degrees = new int[graph.maxDegree() + 1];
		for (int degree : graph.degrees()) {
			degrees[degree]++;
		}
		degreeOutputStream.write(ArrayUtil.stringArray(degrees).getBytes(
				"UTF-8"));
	}

	/**
	 * Generate a graph by either loading it from file or creating it randomly.
	 * 
	 * @param arguments
	 *            The CLI program arguments.
	 * @return a complete graph.
	 * @throws Exception
	 *             Error creating the graph object.
	 */
	private Graph generateGraph(final Arguments arguments) throws Exception {
		RandomGenerator rand = new MersenneTwister(arguments.seed);
		Graph g = null;

		if (arguments.graphGenerator == GraphGenerator.LOAD) {
			g = Graph.read(arguments.graphInput, rand);
		} else if (arguments.graphGenerator == GraphGenerator.LOAD_DOT) {
			g = Graph.readDot(arguments.graphInput, rand);
		} else if (arguments.graphGenerator == GraphGenerator.LOAD_GML) {
			g = Graph.readGml(arguments.graphInput, rand);
		} else {
			final ArrayList<SimpleNode> nodes = Graph.generateNodes(
					arguments.networkSize, rand, arguments.fastGeneration,
					arguments.getDegreeSource(rand));
			final LinkLengthSource linkLengthSource = arguments
					.getLinkLengthSource(rand, nodes);

			switch (arguments.graphGenerator) {
			case SANDBERG:
				g = Graph.connectSandberg(nodes, arguments.shortcuts,
						linkLengthSource);
				break;
			case SUPER_NODE:
				g = Graph.connectSuperNode(nodes, arguments.lattice);
				break;
			case STANDARD:
				g = Graph.connectGraph(nodes, rand, linkLengthSource,
						arguments.lattice);
				break;
			default:
				StringBuilder b = new StringBuilder(
						"Missing implementation piece for graph generation method ");
				b.append(arguments.graphGenerator.name());
				throw new IllegalStateException(b.toString());
			}
		}
		g.updateGraphStats();
		return g;
	}

	/**
	 * TODO.
	 * 
	 * @param graph
	 *            The graph.
	 * @param rand
	 *            Random generator.
	 * @param maxHops
	 *            Maximum hops.
	 * @param containingPath
	 *            Output directory.
	 * @param uniform
	 *            Metropolis Hastings.
	 * @throws IOException
	 *             Error writing results to the output.
	 */
	private void probeDistribution(final Graph graph,
			final RandomGenerator rand, final int maxHops,
			final String containingPath, final boolean uniform)
			throws IOException {
		File output = new File(containingPath);
		assert output.isDirectory();
		if (!output.exists()) {
			if (!output.mkdirs()) {
				StringBuilder b = new StringBuilder(
						"Unable to create requested output directory \"");
				b.append(containingPath);
				throw new IOException(b.toString());
			}
		}

		// TODO: nTrials and nProbes configurable on command line.
		final int nTrials = 100;
		final int nProbes = graph.size() * 30;
		LOGGER.warning("Determining baseline");
		int[] baselineOccurrences = new int[graph.size()];
		/*
		 * Find baseline for visibility by selecting the same number of nodes
		 * from the entire network at random as endpoints at each HTL. Sort
		 * occurrences each run, then add to final result array to represent
		 * actual spread from each run and avoid node index influence.
		 */
		for (int i = 0; i < nTrials; i++) {
			int[] trialOccurrences = new int[graph.size()];
			for (int walk = 0; walk < nProbes; walk++) {
				trialOccurrences[graph.getNode(rand.nextInt(graph.size())).index]++;
			}
			Arrays.sort(trialOccurrences);
			assert baselineOccurrences.length == trialOccurrences.length;
			for (int j = 0; j < trialOccurrences.length; j++) {
				baselineOccurrences[j] += trialOccurrences[j];
			}
		}

		output = new File(containingPath + File.separator + "reference.dat");
		ArrayUtil.writeArray(baselineOccurrences, output);

		LOGGER.warning("Simulating HTL");
		// Find distribution of nodes reached with random walk for increasing
		// hops from all nodes.
		// maxHops + 1 is because the starting node is at zero hops.
		int[][] hopOccurrences = new int[maxHops + 1][graph.size()];
		ArrayList<SimpleNode> trace;
		for (int nodeIndex = 0; nodeIndex < nTrials; nodeIndex++) {
			SimpleNode source = graph.getNode(rand.nextInt(graph.size()));
			SimpleNode alongTrace;
			int[][] trialOccurrences = new int[maxHops + 1][graph.size()];
			for (int walk = 0; walk < nProbes; walk++) {
				trace = source.randomWalkList(maxHops, uniform, rand);
				// Traces: starting point (zero hops), then maxHops hops from
				// there.
				assert trace.size() == maxHops + 1;
				for (int fromEnd = 0; fromEnd <= maxHops; fromEnd++) {
					// fromEnd of trace: hops along. 0 is starting node.
					alongTrace = trace.get(fromEnd);
					trialOccurrences[fromEnd][alongTrace.index]++;
				}
			}
			assert hopOccurrences.length == trialOccurrences.length;
			for (int i = 0; i < trialOccurrences.length; i++) {
				assert hopOccurrences[i].length == trialOccurrences[i].length;
				Arrays.sort(trialOccurrences[i]);
				for (int j = 0; j < trialOccurrences[i].length; j++) {
					hopOccurrences[i][j] += trialOccurrences[i][j];
				}
			}
		}

		LOGGER.warning("Sorting results.");
		for (int hops = 0; hops <= maxHops; hops++) {
			output = new File(containingPath + File.separator + "probe-" + hops
					+ ".dat");
			ArrayUtil.writeArray(hopOccurrences[hops], output);
		}
	}

	/**
	 * Simulate routing in the graph.
	 * 
	 * @param graph
	 *            The graph.
	 * @param rand
	 *            Random generator.
	 * @param arguments
	 *            The CLI arguments.
	 * @throws Exception
	 *             Error running the routing simulation.
	 */
	private void simulate(final Graph graph, final RandomGenerator rand,
			final Arguments arguments) throws Exception {
		final int nRequests = arguments.nRouteRequests;
		final int maxHTL = arguments.maxHopsRoute;
		final RoutingPolicy routingPolicy = arguments.routingPolicy;
		final FoldingPolicy foldingPolicy = arguments.foldingPolicy;
		final int nLookAhead = arguments.nLookAhead;
		final boolean newFoldingMethod = !arguments.oldPathFolding;
		final boolean bootstrap = arguments.bootstrap;
		final OutputStream outputRoute = arguments.routingSimOutput;
		final boolean scriptOutput = arguments.scriptOutput;

		// Print out current run progress for users benefit
		System.err.println("\n\tRouting Simulation");
		Progresser prog = new Progresser(System.err, nRequests);

		String beforeStats = "\nGraph initial stats\n"
				+ graph.printGraphStats();

		RoutingExp experiment = new RoutingExp(maxHTL, nRequests);
		for (int i = 0; i < nRequests; i++) {

			prog.hit();

			final SimpleNode origin = graph.getNode(rand.nextInt(graph.size()));
			/*
			 * It causes distortion to select among node locations for
			 * destinations as they may be less evenly distributed, but it
			 * allows determining if a request was routed exactly based on
			 * whether the target location is equal.
			 */
			final SimpleNode destination = graph.getNode(rand.nextInt(graph
					.size()));
			final RouteResult result = origin.route(destination, maxHTL,
					routingPolicy, foldingPolicy, nLookAhead, newFoldingMethod);

			experiment.record(result.isSuccess(), result.getPathLength(),
					result.getTravelLength());

			/*
			 * Bootstrap all nodes which became disconnected during path
			 * folding. Bootstrapping will not connect to disconnected nodes.
			 * Bootstrapping will drop connections when making new ones so that
			 * the total connection count remains the same; additional nodes may
			 * become disconnected in the process.
			 */
			Queue<SimpleNode> disconnected = new LinkedList<SimpleNode>(result
					.getFoldingResult().getDisconnected());
			experiment.disconnectedFolding(disconnected.size());
			experiment.foldingOperations(result.getFoldingResult()
					.getFoldingOperations());

			while (bootstrap && !disconnected.isEmpty()) {
				for (SimpleNode additional : graph.bootstrap(
						disconnected.remove(), rand)) {
					disconnected.offer(additional);
					experiment.disconnectBootStrap();
				}
			}
		}
		
		graph.updateGraphStats();
		/*
		 * Output to the file.
		 */
		if (outputRoute != null) {
			outputRoute.write(experiment.toString().getBytes("UTF-8"));
			outputRoute.write(beforeStats.getBytes("UTF-8"));
			outputRoute.write("\n\nFinal graph stats\n".getBytes("UTF-8"));
			outputRoute.write(graph.printGraphStats().getBytes("UTF-8"));
		}
		/*
		 * Script output
		 */
		if(scriptOutput){
			StringBuilder b = new StringBuilder();
			b.append(graph.toStringHeaders());
			b.append(experiment.toStringHeaders());
			b.append('\n');
			
			b.append(graph.toStringValues());
			b.append(experiment.toStringValues());
			b.append('\n');
			
			// Only use System.out for script outputs
			System.out.print(b.toString());
		}
		LOGGER.info(experiment.toString());
	}
}
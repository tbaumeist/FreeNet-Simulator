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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Class to perform routing simulations on Graphs.
 */
public class RoutingSim {
	private final static Logger LOGGER = Logger.getLogger(RoutingSim.class
			.getName());

	/**
	 * Main simulator program. Generate a set of graphs of different parameters,
	 * run a set of requests on them, and print assorted stats.
	 * 
	 * @param args
	 *            Command-line arguments; not used.
	 * @throws Exception
	 */
	public static void main(String[] args) {
		try{
			new RoutingSim().run(Arguments.parse(args));
		}catch(Exception e){
			LOGGER.severe("Error running simulator: " + e.getMessage());
		}
	}

	private void run(Arguments arguments) throws Exception {
		ByteArrayOutputStream memory;
		
		SimLogger.setup();

		if (arguments == null)
			System.exit(1);

		SimLogger.setup(arguments.logLevel);
		LOGGER.fine(arguments.toString());

		// Time tracking: report time taken for each graph setting if verbose;
		// upon completion otherwise.
		long startTime = System.currentTimeMillis();
		long lastTime = startTime;

		// Load the graph; otherwise generate.
		Graph g = this.generateGraph(arguments);

		LOGGER.info("Initial graph stats\n" + g.printGraphStats());
		memory = new ByteArrayOutputStream();
		this.writeDegreeOutput(g, memory);
		LOGGER.fine("Initial degree distribution\nDegree Count\n"+memory.toString());
		LOGGER.fine("Graph generation took (ms): "
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
			this.simulate(g, rand, arguments.nRouteRequests,
					arguments.foldingPolicy, arguments.routingPolicy,
					arguments.bootstrap, arguments.routingSimOutput,
					arguments.maxHopsRoute, arguments.nLookAhead);
		}
		
		LOGGER.info("Final graph stats\n" + g.printGraphStats());

		this.writeDegreeOutput(g, arguments.degreeOutput);
		memory = new ByteArrayOutputStream();
		this.writeDegreeOutput(g, memory);
		LOGGER.fine("Final degree distribution\nDegree Count\n"+memory.toString());

		this.writeLinkOutput(g, arguments.linkOutput, arguments.excludeLattice);

		if (arguments.graphOutput != null)
			g.write(arguments.graphOutput);

		if (arguments.graphOutputText != null)
			g.writeText(arguments.graphOutputText);

		LOGGER.fine("Route/Probe time taken (ms): "
				+ (System.currentTimeMillis() - lastTime));
		LOGGER.fine("Total time taken (ms): "
				+ (System.currentTimeMillis() - startTime));		
	}

	private void writeLinkOutput(Graph g, OutputStream linkOutput,
			boolean excludeLattice) throws IOException {
		if (linkOutput == null)
			return;

		ArrayList<Double> lengths = g.edgeLengths(excludeLattice);
		// Output is intended for gnuplot CDF - second value is Y and should
		// sum to 1.
		double normalized = 1.0 / lengths.size();
		for (double length : lengths) {
			linkOutput.write((length + " " + normalized + "\n").getBytes());
		}
	}

	private void writeDegreeOutput(Graph g, OutputStream degreeOutput)
			throws IOException {
		if (degreeOutput == null)
			return;
		int[] degrees = new int[g.maxDegree() + 1];
		for (int degree : g.degrees()) {
			degrees[degree]++;
		}
		degreeOutput.write(ArrayUtil.stringArray(degrees).getBytes());
	}

	private Graph generateGraph(Arguments arguments) throws Exception {
		RandomGenerator rand = new MersenneTwister(arguments.seed);
		Graph g = null;

		if (arguments.graphGenerator == GraphGenerator.LOAD) {
			g = Graph.read(arguments.graphInput, rand);
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
				throw new IllegalStateException(
						"Missing implementation piece for graph generation method "
								+ arguments.graphGenerator.name());
			}
		}
		return g;
	}

	/**
	 * @param array
	 *            to convert
	 * @return string in which "[index] [value]" pairs are newline-delimited.
	 */
	private static String stringArray(int[] array) {
		String s = "";
		for (int i = 0; i < array.length; i++)
			s += i + " " + array[i] + "\n";
		return s;
	}

	private static void writeArray(int[] array, File target) {
		try {
			FileOutputStream outputStream = new FileOutputStream(target);
			outputStream.write(stringArray(array).getBytes());
			outputStream.close();
		} catch (FileNotFoundException e) {
			System.out.println("Cannot open file \"" + target.getAbsolutePath()
					+ "\" for writing.");
			System.out.println(e);
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	private void probeDistribution(Graph g, RandomGenerator rand,
			int maxHops, final String containingPath, boolean uniform) {
		File output = new File(containingPath);
		assert output.isDirectory();
		if (!output.exists()) {
			if (!output.mkdirs()) {
				System.out
						.println("Unable to create requested output directory \""
								+ containingPath + "\".");
				System.exit(1);
			}
		}

		// TODO: nTrials and nProbes configurable on command line.
		final int nTrials = 100;
		final int nProbes = g.size() * 30;
		System.out.println("Determining baseline");
		int[] baselineOccurrences = new int[g.size()];
		/*
		 * Find baseline for visibility by selecting the same number of nodes
		 * from the entire network at random as endpoints at each HTL. Sort
		 * occurrences each run, then add to final result array to represent
		 * actual spread from each run and avoid node index influence.
		 */
		for (int i = 0; i < nTrials; i++) {
			int[] trialOccurrences = new int[g.size()];
			for (int walk = 0; walk < nProbes; walk++) {
				trialOccurrences[g.getNode(rand.nextInt(g.size())).index]++;
			}
			Arrays.sort(trialOccurrences);
			assert baselineOccurrences.length == trialOccurrences.length;
			for (int j = 0; j < trialOccurrences.length; j++) {
				baselineOccurrences[j] += trialOccurrences[j];
			}
		}

		output = new File(containingPath + File.separator + "reference.dat");
		writeArray(baselineOccurrences, output);

		System.out.println("Simulating HTL");
		// Find distribution of nodes reached with random walk for increasing
		// hops from all nodes.
		// maxHops + 1 is because the starting node is at zero hops.
		int[][] hopOccurrences = new int[maxHops + 1][g.size()];
		ArrayList<SimpleNode> trace;
		for (int nodeIndex = 0; nodeIndex < nTrials; nodeIndex++) {
			SimpleNode source = g.getNode(rand.nextInt(g.size()));
			SimpleNode alongTrace;
			int[][] trialOccurrences = new int[maxHops + 1][g.size()];
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

		System.out.println("Sorting results.");
		for (int hops = 0; hops <= maxHops; hops++) {
			output = new File(containingPath + File.separator + "probe-" + hops
					+ ".dat");
			writeArray(hopOccurrences[hops], output);
		}
	}

	private void simulate(Graph g, RandomGenerator rand, int nRequests,
			final FoldingPolicy foldingPolicy,
			final RoutingPolicy routingPolicy, final boolean bootstrap,
			final OutputStream outputRoute, final int maxHTL, final int nLookAhead) throws IOException {

		// Print out current run progress for users benefit
		System.out.println("\tRouting Simulation");
		Progresser prog = new Progresser(System.out, nRequests);
		
		String beforeStats = "\nGraph initial stats\n" + g.printGraphStats();

		RoutingExp experiment = new RoutingExp(maxHTL, nRequests);
		for (int i = 0; i < nRequests; i++) {

			prog.hit();

			final SimpleNode origin = g.getNode(rand.nextInt(g.size()));
			/*
			 * It causes distortion to select among node locations for
			 * destinations as they may be less evenly distributed, but it
			 * allows determining if a request was routed exactly based on
			 * whether the target location is equal.
			 */
			final SimpleNode destination = g.getNode(rand.nextInt(g.size()));
			final RouteResult result = origin.route(destination, maxHTL,
					routingPolicy, foldingPolicy, nLookAhead);

			experiment.record(result.success, result.pathLength);

			/*
			 * Bootstrap all nodes which became disconnected during path
			 * folding. Bootstrapping will not connect to disconnected nodes.
			 * Bootstrapping will drop connections when making new ones so that
			 * the total connection count remains the same; additional nodes may
			 * become disconnected in the process.
			 */
			Queue<SimpleNode> disconnected = new LinkedList<SimpleNode>(
					result.foldingResult.getDisconnected());
			experiment.disconnectedFolding(disconnected.size());
			experiment.foldingOperations(result.foldingResult.getFoldingOperations());
			
			while (bootstrap && !disconnected.isEmpty()) {
				for (SimpleNode additional : g.bootstrap(disconnected.remove(),
						rand)) {
					disconnected.offer(additional);
					experiment.disconnectBootStrap();
				}
			}
		}
		outputRoute.write(experiment.toString().getBytes());
		outputRoute.write(beforeStats.getBytes());
		outputRoute.write("\n\nFinal graph stats\n".getBytes());
		outputRoute.write(g.printGraphStats().getBytes());
		LOGGER.fine(experiment.toString());
	}

	/**
	 * Utility method to summarize a distribution. Assumes array is sorted, as
	 * per Arrays.sort.
	 * 
	 * @param a
	 *            The array to summarize
	 * @param verbose
	 *            Whether to print the long or short version
	 * @return A String summarizing the array
	 */
	public static String printArraySummary(int[] a, boolean verbose) {
		if (!isSorted(a))
			throw new IllegalArgumentException("Array must be sorted.");
		double m = mean(a);
		double stdDev = stdDev(a);
		int pct50 = a[((int) (a.length * 0.5))];
		int pct90 = a[((int) (a.length * 0.9))];
		int pct97 = a[((int) (a.length * 0.97))];
		int pct99 = a[((int) (a.length * 0.99))];
		final StringBuilder summary = new StringBuilder();
		if (verbose) {
			summary.append("Mean:").append(m).append("\n");
			summary.append("Std Dev:").append(stdDev).append("\n");
			summary.append("50th percentile:").append(pct50).append("\n");
			summary.append("50th percentile:").append(pct50).append("\n");
			summary.append("90th percentile:").append(pct90).append("\n");
			summary.append("97th percentile:").append(pct97).append("\n");
			summary.append("99th percentile:").append(pct99).append("\n");
		} else {
			summary.append(m).append("\t").append(stdDev).append("\t")
					.append(pct50).append("\t").append(pct90).append("\t")
					.append(pct97).append("\t").append(pct99).append("\t");
		}
		return summary.toString();
	}

	private static double mean(int[] a) {
		double m = 0.0;
		for (int anA : a)
			m += anA;
		m /= a.length;
		return m;
	}

	private static double sumSquares(int[] a) {
		double ss = 0.0;
		for (int anA : a)
			ss += ((double) anA) * ((double) anA);
		return ss;
	}

	private static double variance(int[] a) {
		double ss = sumSquares(a);
		double m = mean(a);
		return (ss / a.length) - m * m;
	}

	private static double stdDev(int[] a) {
		return Math.sqrt(variance(a));
	}

	/**
	 * @param a
	 *            The array to check.
	 * @return True if the array is sorted in non-decreasing order.
	 */
	private static boolean isSorted(int[] a) {
		for (int i = 0; i < a.length - 1; i++)
			if (a[i] > a[i + 1])
				return false;
		return true;
	}

	/*
	 * TODO: Is there an alternative to copy-pasting to get array summary for
	 * both doubles and ints? Using Generics with <? extends Number> doesn't
	 * seem helpful as it wouldn't support operators and there are no common
	 * methods in Number.
	 */

	/**
	 * Utility method to summarize a distribution. Assumes array is sorted, as
	 * per Arrays.sort.
	 * 
	 * @param a
	 *            The array to summarize
	 * @param verbose
	 *            Whether to print the long or short version
	 * @return A String summarizing the array
	 */
	public static String printArraySummary(double[] a, boolean verbose) {
		if (!isSorted(a))
			throw new IllegalArgumentException("Array must be sorted.");
		double m = mean(a);
		double stdDev = stdDev(a);
		double pct50 = a[((int) (a.length * 0.5))];
		// double pct80 = a[((int) (a.length * 0.8))];
		double pct90 = a[((int) (a.length * 0.9))];
		double pct97 = a[((int) (a.length * 0.97))];
		double pct99 = a[((int) (a.length * 0.99))];
		String s;
		if (verbose) {
			s = "Mean:			" + m + "\n";
			s = s + "Std Dev:		" + stdDev + "\n";
			s = s + "50th percentile:	" + pct50 + "\n";
			s = s + "90th percentile:	" + pct90 + "\n";
			s = s + "97th percentile:	" + pct97 + "\n";
			s = s + "99th percentile:	" + pct99 + "\n";
		} else {
			s = m + "\t" + stdDev + "\t" + pct50 + "\t" + pct90 + "\t" + pct97
					+ "\t" + pct99 + "\t";
		}
		return s;
	}

	private static double mean(double[] a) {
		double m = 0.0;
		for (double anA : a)
			m += anA;
		m /= a.length;
		return m;
	}

	private static double sumSquares(double[] a) {
		double ss = 0.0;
		for (double anA : a)
			ss += anA * anA;
		return ss;
	}

	private static double variance(double[] a) {
		double ss = sumSquares(a);
		double m = mean(a);
		return (ss / a.length) - m * m;
	}

	private static double stdDev(double[] a) {
		return Math.sqrt(variance(a));
	}

	private static boolean isSorted(double[] a) {
		for (int i = 0; i < a.length - 1; i++)
			if (a[i] > a[i + 1])
				return false;
		return true;
	}
}

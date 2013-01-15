package org.freenetproject.routing_simulator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.graph.degree.ConformingDegreeSource;
import org.freenetproject.routing_simulator.graph.degree.DegreeSource;
import org.freenetproject.routing_simulator.graph.degree.FixedDegreeSource;
import org.freenetproject.routing_simulator.graph.degree.PoissonDegreeSource;
import org.freenetproject.routing_simulator.graph.linklength.ConformingLinkSource;
import org.freenetproject.routing_simulator.graph.linklength.KleinbergLinkSource;
import org.freenetproject.routing_simulator.graph.linklength.LinkLengthSource;
import org.freenetproject.routing_simulator.graph.linklength.UniformLinkSource;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;
import org.freenetproject.routing_simulator.util.logging.SimLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.freenetproject.routing_simulator.util.File.readableFile;
import static org.freenetproject.routing_simulator.util.File.writableDirectory;
import static org.freenetproject.routing_simulator.util.File.writableFile;

/**
 * Parses arguments, determines any errors, and provides access to the values.
 *
 * Static methods parse and validate arguments; an instance stores the values on its fields.
 */
public class Arguments {
	
	/*
	 * Public attributes of the arguments class
	 */
	public final boolean lattice, fastGeneration, runProbe, metropolisHastings, runRoute, excludeLattice, bootstrap, pause;
	public final int seed, networkSize, shortcuts, maxHopsProbe, maxHopsRoute, nRouteRequests, nLookAhead;
	public final GraphGenerator graphGenerator;
	public final DataInputStream degreeInput, linkInput, graphInput;
	public final DataOutputStream degreeOutput, linkOutput, graphOutput, graphOutputText, routingSimOutput;
	public final String outputProbe, logLevel;
	public final FoldingPolicy foldingPolicy;
	public final RoutingPolicy routingPolicy;

	private final CommandLine cmd;
	private static final RoutingPolicy ROUTING_DEFAULT = RoutingPolicy.BACKTRACKING;
	private static final FoldingPolicy FOLDING_DEFAULT = FoldingPolicy.FREENET;
	private static final SimLogger.LogLevel LOGGING_DEFAULT = SimLogger.LogLevel.REGULAR;
	private final static Logger LOGGER = Logger.getLogger(RoutingSim.class.getName());
	/*
	 * Program options
	 */
	private final static Option opt_help = new Option("h", "help", false, "Display this message.");
	private final static Option opt_version = new Option("v", "version", false, "Display software version.");
	private final static Option opt_logLevel = new Option("l", "log-level", true, "Level of logging: ");
	private final static Option opt_seed = new Option("s", "seed", true, "Seed used by psuedorandom number generator.");
	private final static Option opt_pause = new Option("p", "pause", false, "Pause the program before execution. (Allows external programs to be attached).");
	
	/*
	 * Graph generation options
	 */
	private final static Option opt_loadGraph = new Option("gl", "graph-load", true, "Path to load a saved graph from.");
	private final static Option opt_saveGraphDot = new Option("gsd", "graph-save-dot", true, "Path to save a graph after simulation is run on it in plain text (DOT format).");
	private final static Option opt_saveGraph = new Option("gs", "graph-save", true, "Path to save a graph after simulation is run on it.");
	private final static Option opt_sandbergGraph = new Option("gS", "graph-sandberg", true, "Generate a directed graph with an edge from x to x -1 mod N for all x = 0 ... N - 1 as described in early section 2.2.1 in \"Searching in a Small World.\" Takes the number of shortcuts to make; the paper specifies 1 shortcut.");
	private final static Option opt_supernodeGraph = new Option("gU", "graph-supernode", false, "Generate a graph with all nodes connected to a single supernode.");
	private final static Option opt_size = new Option("gn", "graph-size", true, "Number of nodes in the network.");
	private final static Option opt_lattice = new Option("gL", "graph-lattice", false, "Generate a graph with undirected lattice links, the given degree distribution, and the given link length distribution. e.g. Topology starts out as a circle.");
	private final static Option opt_fastGeneration = new Option("gfl", "graph-fast-location", false, "If present, the simulator will assign locations with even spacing and, when using --ideal-link, take shortcuts to speed up graph generation.");
	
	private final static Option opt_outputDegree = new Option("do", "degree-output", true, "Output file for degree distribution.");
	private final static Option opt_fixeDegree = new Option("df", "degree-fixed", true, "All nodes are as close to the specified degree as practical.");
	private final static Option opt_conformingDegree = new Option("dc", "degree-conforming", true, "Distribution conforming to a file. Takes a path to a degree distribution file of the format \"[degree] [number of occurrences]\\n\"");
	private final static Option opt_poissonDegree = new Option("dp", "degree-poisson", true, "Distribution conforming to a Poisson distribution with the given mean.");
	
	private final static Option opt_outputLink = new Option("lo", "link-output", true, "Output file for link length distribution.");
	private final static Option opt_excludeLattice = new Option("lel", "link-exclude-lattice", false, "Exclude links from index X to X - 1 mod N when outputting link lengths. If the graph does not actually have lattice connections this is not recommended. If not specified any lattice links will be included when outputing link length.");
	private final static Option opt_idealLink = new Option("li", "link-ideal", false, "Kleinberg's ideal distribution: proportional to 1/d.");
	private final static Option opt_flatLink = new Option("lf", "link-flat", false, "Intentionally terrible distribution: uniformly random.");
	private final static Option opt_conformingLink = new Option("lc", "link-conforming", true, "Distribution conforming to a file. Takes a path to a degree distribution file of the format \"[degree] [number of occurrences]\\n\"\"");
	
	private final static Option opt_route = new Option("r", "route", true, "Simulate routing the given number of requests. Requires that --route-output and --route-hops be specified.");
	private final static Option opt_routeHops = new Option("rh", "route-hops", true, "The maximum number of hops to route with.");
	private final static Option opt_outputRoute = new Option("ro", "route-output", true, "The file to which routing information is output.");
	private final static Option opt_foldPolicy = new Option("rfp", "route-fold-policy", true,  "Path folding policy:");
	private final static Option opt_lookAhead = new Option("rla", "route-look-ahead", true, "When routing look ahead n hops before routing from a node. Default = 1.");
	private final static Option opt_routePolicy = new Option("rp", "route-policy", true, "Routing policy used.");
	private final static Option opt_bootstrap = new Option("rb", "route-bootstrap", false, "If specified, nodes which lose all their connections due to path folding will be connected to random nodes.");
	
	private final static Option opt_probe = new Option("p", "probe", true, "Simulate running probes from random locations for the specified number of maximum hops. Requires that --probe-output be specified.");
	private final static Option opt_metropolisHastings = new Option("pmh", "probe-metropolis-hastings", false, "If present, probes will be routed with Metropolis-Hastings correction. If not, peers will be selected entirely at random.");
	private final static Option opt_outputProbe = new Option("po", "probe-output", true, "Directory to which probe distribution is output as \"[node ID] [times seen]\\n\" for a reference of random selection from the whole and at each hop up to the specified maximum hops.");
	
		
	private Arguments(boolean lattice, boolean fastGeneration, boolean runProbe, boolean metropolisHastings, boolean runRoute, boolean excludeLattice, boolean bootstrap,
	                  int seed, int networkSize, int shortcuts, int maxHopsProbe, int maxHopsRequest, int nRequests,
	                  GraphGenerator graphGenerator,
	                  DataInputStream degreeInput, DataInputStream linkInput, DataInputStream graphInput,
	                  DataOutputStream degreeOutput, DataOutputStream linkOutput, DataOutputStream graphOutput, DataOutputStream graphOutputText,
	                  String outputProbe, DataOutputStream outputRoute,
	                  FoldingPolicy foldingPolicy,
	                  RoutingPolicy routingPolicy, int nLookAhead, String logLevel, boolean pause,
	                  CommandLine cmd) {
		this.lattice = lattice;
		this.fastGeneration = fastGeneration;
		this.runProbe = runProbe;
		this.metropolisHastings = metropolisHastings;
		this.runRoute = runRoute;
		this.bootstrap = bootstrap;
		this.seed = seed;
		this.networkSize = networkSize;
		this.shortcuts = shortcuts;
		this.maxHopsProbe = maxHopsProbe;
		this.maxHopsRoute = maxHopsRequest;
		this.nRouteRequests = nRequests;
		this.graphGenerator = graphGenerator;
		this.degreeInput = degreeInput;
		this.linkInput = linkInput;
		this.graphInput = graphInput;
		this.degreeOutput = degreeOutput;
		this.linkOutput = linkOutput;
		this.graphOutput = graphOutput;
		this.graphOutputText = graphOutputText;
		this.outputProbe = outputProbe;
		this.routingSimOutput = outputRoute;
		this.foldingPolicy = foldingPolicy;
		this.routingPolicy = routingPolicy;
		this.excludeLattice = excludeLattice;
		this.nLookAhead = nLookAhead;
		this.logLevel = logLevel;
		this.pause = pause;
		this.cmd = cmd;
	}
	
	@Override
	public String toString(){
		StringBuilder b = new StringBuilder();
		for(Field f : getClass().getFields()){
			b.append(f.getName());
			b.append(" : ");
			try {
				b.append(f.get(this));
			} catch (Exception e) {
				LOGGER.severe(e.getMessage());
			}
			b.append("\n");
		}

		return b.toString();
	}

	public DegreeSource getDegreeSource(RandomGenerator random) {
		final DegreeSource degreeSource;

		if (cmd.hasOption(opt_conformingDegree.getLongOpt())) degreeSource = new ConformingDegreeSource(degreeInput, random);
		else if (cmd.hasOption(opt_poissonDegree.getLongOpt())) degreeSource = new PoissonDegreeSource(Integer.valueOf(cmd.getOptionValue(opt_poissonDegree.getLongOpt())));
		else if (cmd.hasOption(opt_fixeDegree.getLongOpt())) degreeSource = new FixedDegreeSource(Integer.valueOf(cmd.getOptionValue(opt_fixeDegree.getLongOpt())));
		else /* if (cmd.hasOption("sandberg-graph" || cmd.hasOption("supernode-graph") */ degreeSource = new FixedDegreeSource(0);

		return degreeSource;
	}

	public LinkLengthSource getLinkLengthSource(RandomGenerator random, ArrayList<SimpleNode> nodes) {
		final LinkLengthSource linkLengthSource;

		if (cmd.hasOption(opt_conformingLink.getLongOpt())) linkLengthSource = new ConformingLinkSource(linkInput, random, nodes);
		else if (cmd.hasOption(opt_idealLink.getLongOpt())) linkLengthSource = new KleinbergLinkSource(random, nodes);
		else if (cmd.hasOption(opt_flatLink.getLongOpt())) linkLengthSource = new UniformLinkSource(random, nodes);
		else /* if cmd.hasOption("supernode-graph") */ linkLengthSource = null;

		return linkLengthSource;
	}

	private static Options generateOptions() {
		Options options = new Options();

		options.addOption(opt_outputDegree);
		options.addOption(opt_outputLink);
		options.addOption(opt_excludeLattice);
		options.addOption(opt_help);
		options.addOption(opt_version);
		options.addOption(opt_seed);
		options.addOption(opt_pause);
		//Graphs: General generation options
		options.addOption(opt_size);
		// TODO: Reinstate or possibly remove.
		options.addOption(opt_fastGeneration);
		options.addOption(opt_loadGraph);
		options.addOption(opt_saveGraphDot);
		options.addOption(opt_saveGraph);
		options.addOption(opt_sandbergGraph);
		options.addOption(opt_lattice);
		options.addOption(opt_supernodeGraph);

		//Graphs: link length distribution
		options.addOption(opt_idealLink);
		options.addOption(opt_flatLink);
		options.addOption(opt_conformingLink);

		//Graphs: degree distribution-
		options.addOption(opt_fixeDegree);
		options.addOption(opt_conformingDegree);
		options.addOption(opt_poissonDegree);

		//Simulations: Routing policies
		options.addOption(opt_route);
		options.addOption(opt_routeHops);
		options.addOption(opt_outputRoute);
		StringBuilder description = new StringBuilder("Path folding policy used. Default is " + FOLDING_DEFAULT.name() +". Possible policies:");
		for (FoldingPolicy policy : FoldingPolicy.values()) description.append(" ").append(policy);
		opt_foldPolicy.setDescription(description.toString());
		options.addOption(opt_foldPolicy);
		options.addOption(opt_lookAhead);

		description = new StringBuilder("Routing policy used. Default is " + ROUTING_DEFAULT.name() +". Possible policies:");
		for (RoutingPolicy policy : RoutingPolicy.values()) description.append(" ").append(policy.name());
		opt_routePolicy.setDescription(description.toString());
		options.addOption(opt_routePolicy);

		//options.addOption("H", "output-hops", true, "Base filename to output hop histograms for each sink policy. Appended with -<policy-num> for each.");
		options.addOption(opt_bootstrap);

		//Simulations: Probe distribution
		options.addOption(opt_probe);
		options.addOption(opt_metropolisHastings);
		options.addOption(opt_outputProbe);

		description = new StringBuilder("Log level used. Default is " + LOGGING_DEFAULT.name() +". Possible levels:");
		for (SimLogger.LogLevel level : SimLogger.LogLevel.values()) description.append(" ").append(level.name());
		opt_logLevel.setDescription(description.toString());
		options.addOption(opt_logLevel);
		
		return options;
	}

	/**
	 * Parses command line arguments and validates them.
	 *
	 * @param args Arguments to parse.
	 * @return An Arguments instance with the parsed arguments, or null in the case of an error.
	 * @throws Exception 
	 */
	public static Arguments parse(String[] args) throws Exception {
		final Options options = generateOptions();
		final CommandLineParser parser = new GnuParser();
		final CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption(opt_help.getLongOpt())) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "java -jar freenet-simulator.jar", options );
			return null;
		}

		if (cmd.hasOption(opt_version.getLongOpt())) {
			System.out.println("Freenet Routing Simulator v 0.0.2-dev");
			return null;
		}

		int degreeOptions = 0;
		for (String option : new String[] { opt_fixeDegree.getLongOpt(), opt_conformingDegree.getLongOpt(), opt_poissonDegree.getLongOpt() }) {
			if (cmd.hasOption(option)) degreeOptions++;
		}

		int linkOptions = 0;
		for (String option : new String[] { opt_idealLink.getLongOpt(), opt_flatLink.getLongOpt(), opt_conformingLink.getLongOpt() }) {
			if (cmd.hasOption(option)) linkOptions++;
		}

		if (degreeOptions > 1 || linkOptions > 1) {
			System.out.println("Graph cannot be generated with multiple methods at once.");
			return null;
		}

		/*
		 * Determine graph generation method to use, and if exactly one was specified correctly. Allowing more
		 * than one generation method to be specified would make it ambiguous which one was used.
		 */
		int validGeneration = 0;
		GraphGenerator graphGenerator = null;
		if (cmd.hasOption(opt_loadGraph.getLongOpt())) {
			validGeneration++;
			graphGenerator = GraphGenerator.LOAD;
		}

		if (cmd.hasOption(opt_supernodeGraph.getLongOpt())) {
			validGeneration++;
			graphGenerator = GraphGenerator.SUPER_NODE;
		}

		if (degreeOptions == 0 && linkOptions == 1 && cmd.hasOption(opt_sandbergGraph.getLongOpt())) {
			validGeneration++;
			graphGenerator = GraphGenerator.SANDBERG;
		}

		if (degreeOptions == 1 && linkOptions == 1) {
			validGeneration++;
			graphGenerator = GraphGenerator.STANDARD;
		}

		if (!cmd.hasOption(opt_size.getLongOpt()) && graphGenerator != GraphGenerator.LOAD) {
			System.out.println("Network size not specified. (--"+opt_size.getLongOpt()+")");
			return null;
		}

		/*
		 * - Sandberg-graph requires link.
		 * - Load-graph does not require link or degree.
		 * - Without these two, degree and link must be specified; optionally lattice too.
		 */
		if (validGeneration == 0 || validGeneration > 1) {
			System.out.println("Either zero or too many graph generation methods specified.");
			System.out.println("Valid graph generators are:");
			System.out.println(" * --" + opt_loadGraph.getLongOpt());
			System.out.println(" * --"+opt_sandbergGraph.getLongOpt()+" with --link-* and --"+opt_size.getLongOpt());
			System.out.println(" * --degree-*, --link-*, --"+opt_size.getLongOpt()+", and optionally --" + opt_lattice.getLongOpt());
			System.out.println(" * --"+opt_supernodeGraph.getLongOpt()+" with --"+opt_size.getLongOpt()+" and optionally --"+ opt_lattice.getLongOpt());
			return null;
		}

		// By this point a single valid graph generator should be specified.
		assert graphGenerator != null;

		if (cmd.hasOption(opt_route.getLongOpt()) && !cmd.hasOption(opt_routeHops.getLongOpt())) {
			System.out.println("--"+opt_route.getLongOpt()+" was specified, but not --"+opt_routeHops.getLongOpt()+".");
			return null;
		}
		if (cmd.hasOption(opt_route.getLongOpt()) && !cmd.hasOption(opt_outputRoute.getLongOpt())) {
			System.out.println("--"+opt_route.getLongOpt()+" was specified, but not --"+opt_outputRoute.getLongOpt()+".");
			return null;
		}
		
		if (cmd.hasOption(opt_probe.getLongOpt()) && !cmd.hasOption(opt_outputProbe.getLongOpt())) {
			System.out.println("--"+opt_probe.getLongOpt()+" was specified, but not --"+opt_outputProbe.getLongOpt()+".");
			return null;
		}

		final FoldingPolicy foldingPolicy;
		if (cmd.hasOption(opt_foldPolicy.getLongOpt())) {
			try {
				foldingPolicy = FoldingPolicy.valueOf(cmd.getOptionValue(opt_foldPolicy.getLongOpt()));
			} catch (IllegalArgumentException e) {
				StringBuilder b= new StringBuilder();
				b.append("The folding policy \"");
				b.append(cmd.getOptionValue(opt_foldPolicy.getLongOpt()));
				b.append("\" is invalid.\n");
				b.append("Possible values are:");
				for (FoldingPolicy policy : FoldingPolicy.values()) {
					b.append(" ").append(policy.toString());
				}
				throw new Exception(b.toString());
			}
		} else {
			foldingPolicy = FOLDING_DEFAULT;
		}

		final RoutingPolicy routingPolicy;
		if (cmd.hasOption(opt_routePolicy.getLongOpt())) {
			final String policy = cmd.getOptionValue(opt_routePolicy.getLongOpt());
			try {
				routingPolicy = RoutingPolicy.valueOf(policy);
			} catch (IllegalArgumentException e) {
				StringBuilder b = new StringBuilder();
				b.append("The routing policy \"");
				b.append(policy);
				b.append("\" is invalid.\n");
				b.append("Possible values are:");
				for (RoutingPolicy policyName : RoutingPolicy.values()) {
					b.append(" ").append(policyName.toString());
				}
				throw new Exception(b.toString());
			}
		} else {
			routingPolicy = ROUTING_DEFAULT;
		}

		//Check for problems with specified paths.
		//Check if input files can be read.
		final DataInputStream degreeInput, linkInput, graphInput;
		try {
			degreeInput = readableFile(opt_conformingDegree.getLongOpt(), cmd);
			linkInput = readableFile(opt_conformingLink.getLongOpt(), cmd);
			graphInput = readableFile(opt_loadGraph.getLongOpt(), cmd);
		} catch (FileNotFoundException e) {
			return null;
		}

		//Check if output paths are directories that can be written to, and create them if they do not exist.
		if (cmd.hasOption(opt_outputProbe.getLongOpt()) && (writableDirectory(cmd.getOptionValue(opt_outputProbe.getLongOpt()))) == null) return null;

		//Check that output files exist and are writable or can be created.
		final DataOutputStream degreeOutput, linkOutput, graphOutput, graphOutputText, routingSimOutput;
		try {
			degreeOutput = writableFile(opt_outputDegree.getLongOpt(), cmd);
			linkOutput = writableFile(opt_outputLink.getLongOpt(), cmd);
			graphOutput = writableFile(opt_saveGraph.getLongOpt(), cmd);
			graphOutputText = writableFile(opt_saveGraphDot.getLongOpt(), cmd);
			routingSimOutput = writableFile(opt_outputRoute.getLongOpt(), cmd);
		} catch (FileNotFoundException e) {
			return null;
		}

		final boolean lattice = cmd.hasOption(opt_lattice.getLongOpt());
		final boolean fastGeneration = cmd.hasOption(opt_fastGeneration.getLongOpt());
		final boolean pause = cmd.hasOption(opt_pause.getLongOpt());
		final int seed = cmd.hasOption(opt_seed.getLongOpt()) ? Integer.valueOf(cmd.getOptionValue(opt_seed.getLongOpt())) : (int)System.currentTimeMillis();
		final int networkSize = cmd.hasOption(opt_size.getLongOpt()) ? Integer.valueOf(cmd.getOptionValue(opt_size.getLongOpt())) : 0;
		final int nRequests = cmd.hasOption(opt_route.getLongOpt()) ? Integer.valueOf(cmd.getOptionValue(opt_route.getLongOpt())) : 0;
		final int maxHopsProbe = cmd.hasOption(opt_probe.getLongOpt()) ? Integer.valueOf(cmd.getOptionValue(opt_probe.getLongOpt())) : 0;
		final int maxHopsRequest = cmd.hasOption(opt_routeHops.getLongOpt()) ? Integer.valueOf(cmd.getOptionValue(opt_routeHops.getLongOpt())) : 0;
		final int shortcuts = cmd.hasOption(opt_sandbergGraph.getLongOpt()) ? Integer.valueOf(cmd.getOptionValue(opt_sandbergGraph.getLongOpt())) : 0;
		final int nLookAhead = cmd.hasOption(opt_lookAhead.getLongOpt()) ? Integer.valueOf(cmd.getOptionValue(opt_lookAhead.getLongOpt())) : 1;
		final String logLevel = cmd.hasOption(opt_logLevel.getLongOpt()) ? cmd.getOptionValue(opt_logLevel.getLongOpt()) : LOGGING_DEFAULT.name();

		return new Arguments(lattice, fastGeneration,
				cmd.hasOption(opt_probe.getLongOpt()),
				cmd.hasOption(opt_metropolisHastings.getLongOpt()),
				cmd.hasOption(opt_route.getLongOpt()),
				cmd.hasOption(opt_excludeLattice.getLongOpt()),
				cmd.hasOption(opt_bootstrap.getLongOpt()), seed, networkSize,
				shortcuts, maxHopsProbe, maxHopsRequest, nRequests,
				graphGenerator, degreeInput, linkInput, graphInput,
				degreeOutput, linkOutput, graphOutput, graphOutputText,
				cmd.getOptionValue(opt_outputProbe.getLongOpt()),
				routingSimOutput,
				foldingPolicy, routingPolicy, nLookAhead, logLevel, 
				pause, cmd);
	}
}

package org.freenetproject.routing_simulator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Logger;

import static org.freenetproject.routing_simulator.util.File.readableFile;
import static org.freenetproject.routing_simulator.util.File.writableDirectory;
import static org.freenetproject.routing_simulator.util.File.writableFile;

/**
 * Parses arguments, determines any errors, and provides access to the values.
 * 
 * Static methods parse and validate arguments; an instance stores the values on
 * its fields.
 */
public final class Arguments {

    /*
     * Public attributes of the arguments class
     */

    /**
     * Build a lattice before constructing graph (a circle).
     */
    public final boolean lattice;
    /**
     * Use fast generation for graph generation.
     */
    public final boolean fastGeneration;
    /**
     * Run the probe on the topology.
     */
    public final boolean runProbe;
    /**
     * Use Metropolis Hastings when running the probe.
     */
    public final boolean metropolisHastings;
    /**
     * Run the route simulation.
     */
    public final boolean runRoute;
    /**
     * Exclude lattice links from link length output.
     */
    public final boolean excludeLattice;
    /**
     * Reconnect any disconnected nodes during routing simulation.
     */
    public final boolean bootstrap;
    /**
     * Pause the simulator execution at the start.
     */
    public final boolean pause;
    /**
     * Print program output in script readable format
     */
    public final boolean scriptOutput;
    /**
     * Use the old path folding method (%7 chance to path fold).
     */
    public final boolean oldPathFolding;
    /**
     * The seed value for the random generator.
     */
    public final int seed;
    /**
     * Graph size to use.
     */
    public final int networkSize;
    /**
     * Number of shortcuts to use in Sandberg graph.
     */
    public final int shortcuts;
    /**
     * Maximum hops for probing.
     */
    public final int maxHopsProbe;
    /**
     * Maximum hops for routing.
     */
    public final int maxHopsRoute;
    /**
     * Number of routing requests to perform.
     */
    public final int nRouteRequests;
    /**
     * Number of hops a node can look ahead before routing.
     */
    public final int nLookAhead;
    /**
     * Type of graph to generate.
     */
    public final GraphGenerator graphGenerator;
    /**
     * Degree distribution input stream.
     */
    public final DataInputStream degreeInput;
    /**
     * Link distribution input stream.
     */
    public final DataInputStream linkInput;
    /**
     * Graph input stream.
     */
    public final DataInputStream graphInput;
    /**
     * Degree distribution output stream.
     */
    public final DataOutputStream degreeOutput;
    /**
     * Link distribution output stream.
     */
    public final DataOutputStream linkOutput;
    /**
     * Graph output stream.
     */
    public final DataOutputStream graphOutput;
    /**
     * DOT format graph output stream.
     */
    public final DataOutputStream graphOutputText;
    /**
     * Routing simulation output stream.
     */
    public final DataOutputStream routingSimOutput;
    /**
     * Probe output directory.
     */
    public final String outputProbe;
    /**
     * Logging level to use.
     */
    public final String logLevel;
    /**
     * Path folding policy to use.
     */
    public final FoldingPolicy foldingPolicy;
    /**
     * Routing policy to use.
     */
    public final RoutingPolicy routingPolicy;
    /**
     * Rate of precision loss to use.
     */
    public final double lookAheadPrecisionLoss;
    /**
     * Probability to randomly route a request.
     */
    public final double routingRandomChance;

    /*
     * Private attributes of the arguments class
     */

    /**
     * Command line object.
     */
    private final CommandLine cmd;
    /**
     * Default routing policy.
     */
    private static final RoutingPolicy ROUTING_DEFAULT = RoutingPolicy.BACKTRACKING;
    /**
     * Default path folding policy to use.
     */
    private static final FoldingPolicy FOLDING_DEFAULT = FoldingPolicy.FREENET;
    /**
     * Default logging level to use.
     */
    private static final SimLogger.LogLevel LOGGING_DEFAULT = SimLogger.LogLevel.REGULAR;
    /**
     * This classes' logger.
     */
    private final static Logger LOGGER = Logger.getLogger(RoutingSim.class
            .getName());

    /*
     * Program options
     */
    private static final Option OPT_HELP = new Option("h", "help", false,
            "Display this message.");
    private static final Option OPT_VERSION = new Option("v", "version", false,
            "Display software version.");
    private static final Option OPT_LOG_LEVEL = new Option("l", "log-level",
            true, "Level of logging: ");
    private static final Option OPT_SEED = new Option("s", "seed", true,
            "Seed used by psuedorandom number generator.");
    private static final Option OPT_PAUSE = new Option(
            "p",
            "pause",
            false,
            "Pause the program before execution. (Allows external programs to be attached).");
    private static final Option OPT_SCRIPT_OUTPUT = new Option("so",
            "script-output", false,
            "Format script output such that it can be read by script.");

    /*
     * Graph generation options
     */
    private static final Option OPT_GRAPH_LOAD = new Option("gl", "graph-load",
            true, "Path to load a saved graph from.");
    private static final Option OPT_GRAPH_LOAD_DOT = new Option("gld",
            "graph-load-dot", true, "Path to load a saved DOT graph from.");
    private static final Option OPT_GRAPH_LOAD_GML = new Option("glg",
            "graph-load-gml", true, "Path to load a saved GML graph from.");
    private static final Option OPT_GRAPH_SAVE_DOT = new Option(
            "gsd",
            "graph-save-dot",
            true,
            "Path to save a graph after simulation is run on it in plain text (DOT format).");
    private static final Option OPT_GRAPH_SAVE = new Option("gs", "graph-save",
            true, "Path to save a graph after simulation is run on it.");
    private static final Option OPT_GRAPH_SANDBERG = new Option(
            "gS",
            "graph-sandberg",
            true,
            "Generate a directed graph with an edge from x to x -1 mod N for all x = 0 ... N - 1 as described in early section 2.2.1 in \"Searching in a Small World.\" Takes the number of shortcuts to make; the paper specifies 1 shortcut.");
    private static final Option OPT_GRAPH_SUPER_NODE = new Option("gU",
            "graph-supernode", false,
            "Generate a graph with all nodes connected to a single supernode.");
    private static final Option OPT_GRAPH_SIZE = new Option("gn", "graph-size",
            true, "Number of nodes in the network.");
    private static final Option OPT_GRAPH_LATTICE = new Option(
            "gL",
            "graph-lattice",
            false,
            "Generate a graph with undirected lattice links, the given degree distribution, and the given link length distribution. e.g. Topology starts out as a circle.");
    private static final Option OPT_GRAPH_FAST_LOCATION = new Option(
            "gfl",
            "graph-fast-location",
            false,
            "If present, the simulator will assign locations with even spacing and, when using --ideal-link, take shortcuts to speed up graph generation.");

    /*
     * Degree options
     */
    private static final Option OPT_DEGREE_OUTPUT = new Option("do",
            "degree-output", true, "Output file for degree distribution.");
    private static final Option OPT_DEGREE_FIXED = new Option("df",
            "degree-fixed", true,
            "All nodes are as close to the specified degree as practical.");
    private static final Option OPT_DEGREE_CONFORMING = new Option(
            "dc",
            "degree-conforming",
            true,
            "Distribution conforming to a file. Takes a path to a degree distribution file of the format \"[degree] [number of occurrences]\\n\"");
    private static final Option OPT_DEGREE_POISSON = new Option("dp",
            "degree-poisson", true,
            "Distribution conforming to a Poisson distribution with the given mean.");

    /*
     * Link options
     */
    private static final Option OPT_LINK_OUTPUT = new Option("lo",
            "link-output", true, "Output file for link length distribution.");
    private static final Option OPT_LINK_EXCLUDE_LATTICE = new Option(
            "lel",
            "link-exclude-lattice",
            false,
            "Exclude links from index X to X - 1 mod N when outputting link lengths. If the graph does not actually have lattice connections this is not recommended. If not specified any lattice links will be included when outputing link length.");
    private static final Option OPT_LINK_IDEAL = new Option("li", "link-ideal",
            false, "Kleinberg's ideal distribution: proportional to 1/d.");
    private static final Option OPT_LINK_FLAT = new Option("lf", "link-flat",
            false, "Intentionally terrible distribution: uniformly random.");
    private static final Option OPT_LINK_CONFORMING = new Option(
            "lc",
            "link-conforming",
            true,
            "Distribution conforming to a file. Takes a path to a degree distribution file of the format \"[degree] [number of occurrences]\\n\"\"");

    /*
     * GRouting options
     */
    private static final Option OPT_ROUTE = new Option(
            "r",
            "route",
            true,
            "Simulate routing the given number of requests. Requires that --route-output and --route-hops be specified.");
    private static final Option OPT_ROUTE_HOPS = new Option("rh", "route-hops",
            true, "The maximum number of hops to route with.");
    private static final Option OPT_ROUTE_OUTPUT = new Option("ro",
            "route-output", true,
            "The file to which routing information is output.");
    private static final Option OPT_ROUTE_FOLDING_POLICY = new Option("rfp",
            "route-fold-policy", true, "Path folding policy:");
    private static final Option OPT_ROUTE_LOOK_AHEAD = new Option("rla",
            "route-look-ahead", true,
            "When routing look ahead n hops before routing from a node. Default = 1.");
    private static final Option OPT_ROUTE_POLICY = new Option("rp",
            "route-policy", true, "Routing policy used.");
    private static final Option OPT_ROUTE_BOOTSTRAP = new Option(
            "rb",
            "route-bootstrap",
            false,
            "If specified, nodes which lose all their connections due to path folding will be connected to random nodes.");
    private static final Option OPT_ROUTE_OLD_FOLDING = new Option("rop",
            "route-old-path-fold", false,
            "Use the old path folding mechanism. (7% chance to randomly path fold).");
    private static final Option OPT_ROUTE_LOOK_PREC = new Option(
            "rlp",
            "route-look-precision",
            true,
            "The precision loss rate for look ahead information. Specified as a floating point.");
    private static final Option OPT_ROUTE_RANDOM_CHANCE = new Option(
            "rrc",
            "route-random-chance",
            true,
            "The probability any given node will randomly route instead of using the default routing algorithm.");

    /*
     * Probing options
     */
    private static final Option OPT_PROBE = new Option(
            "p",
            "probe",
            true,
            "Simulate running probes from random locations for the specified number of maximum hops. Requires that --probe-output be specified.");
    private static final Option OPT_PROBE_METROPOLIS_HASTINGS = new Option(
            "pmh",
            "probe-metropolis-hastings",
            false,
            "If present, probes will be routed with Metropolis-Hastings correction. If not, peers will be selected entirely at random.");
    private static final Option OPT_PROBE_OUTPUT = new Option(
            "po",
            "probe-output",
            true,
            "Directory to which probe distribution is output as \"[node ID] [times seen]\\n\" for a reference of random selection from the whole and at each hop up to the specified maximum hops.");

    private Arguments(final boolean lattice, final boolean fastGeneration,
            final boolean runProbe, final boolean metropolisHastings,
            final boolean runRoute, final boolean excludeLattice,
            final boolean bootstrap, final int seed, final int networkSize,
            final int shortcuts, final int maxHopsProbe,
            final int maxHopsRequest, final int nRequests,
            final double precisionLoss, final double routeRandomChance,
            final GraphGenerator graphGenerator,
            final DataInputStream degreeInput, final DataInputStream linkInput,
            final DataInputStream graphInput,
            final DataOutputStream degreeOutput,
            final DataOutputStream linkOutput,
            final DataOutputStream graphOutput,
            final DataOutputStream graphOutputText, final String outputProbe,
            final DataOutputStream outputRoute,
            final FoldingPolicy foldingPolicy,
            final RoutingPolicy routingPolicy, final int nLookAhead,
            final String logLevel, final boolean pause,
            final boolean scriptOutput, final boolean oldPathFolding,
            final CommandLine cmd) {
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
        this.lookAheadPrecisionLoss = precisionLoss;
        this.routingRandomChance = routeRandomChance;
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
        this.scriptOutput = scriptOutput;
        this.oldPathFolding = oldPathFolding;
        this.cmd = cmd;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Field f : getClass().getFields()) {
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

    /**
     * Get the degree source from the CLI.
     * 
     * @param random
     *            Random generator.
     * @return degree source.
     */
    public DegreeSource getDegreeSource(final RandomGenerator random) {
        final DegreeSource degreeSource;

        if (cmd.hasOption(OPT_DEGREE_CONFORMING.getLongOpt())) {
            degreeSource = new ConformingDegreeSource(degreeInput, random);
        } else if (cmd.hasOption(OPT_DEGREE_POISSON.getLongOpt())) {
            degreeSource = new PoissonDegreeSource(Integer.valueOf(cmd
                    .getOptionValue(OPT_DEGREE_POISSON.getLongOpt())));
        } else if (cmd.hasOption(OPT_DEGREE_FIXED.getLongOpt())) {
            degreeSource = new FixedDegreeSource(Integer.valueOf(cmd
                    .getOptionValue(OPT_DEGREE_FIXED.getLongOpt())));
        } else {
            /*
             * if (cmd.hasOption("sandberg-graph" ||
             * cmd.hasOption("supernode-graph")
             */degreeSource = new FixedDegreeSource(0);
        }

        return degreeSource;
    }

    /**
     * Get the link length source from CLI.
     * 
     * @param random
     *            Random generator.
     * @param nodes
     *            List of graph nodes.
     * @return Link length source.
     */
    public LinkLengthSource getLinkLengthSource(final RandomGenerator random,
            final ArrayList<SimpleNode> nodes) {
        final LinkLengthSource linkLengthSource;

        if (cmd.hasOption(OPT_LINK_CONFORMING.getLongOpt())) {
            linkLengthSource = new ConformingLinkSource(linkInput, random,
                    nodes);
        } else if (cmd.hasOption(OPT_LINK_IDEAL.getLongOpt())) {
            linkLengthSource = new KleinbergLinkSource(random, nodes);
        } else if (cmd.hasOption(OPT_LINK_FLAT.getLongOpt())) {
            linkLengthSource = new UniformLinkSource(random, nodes);
        } else {
            /* if cmd.hasOption("supernode-graph") */linkLengthSource = null;
        }

        return linkLengthSource;
    }

    /**
     * Generate the list of CLI options.
     * 
     * @return list of CLI options.
     */
    private static Options generateOptions() {
        Options options = new Options();

        options.addOption(OPT_DEGREE_OUTPUT);
        options.addOption(OPT_LINK_OUTPUT);
        options.addOption(OPT_LINK_EXCLUDE_LATTICE);
        options.addOption(OPT_HELP);
        options.addOption(OPT_VERSION);
        options.addOption(OPT_SEED);
        options.addOption(OPT_PAUSE);
        options.addOption(OPT_SCRIPT_OUTPUT);
        // Graphs: General generation options
        options.addOption(OPT_GRAPH_SIZE);
        // TODO: Reinstate or possibly remove.
        options.addOption(OPT_GRAPH_FAST_LOCATION);
        options.addOption(OPT_GRAPH_LOAD);
        options.addOption(OPT_GRAPH_LOAD_DOT);
        options.addOption(OPT_GRAPH_LOAD_GML);
        options.addOption(OPT_GRAPH_SAVE_DOT);
        options.addOption(OPT_GRAPH_SAVE);
        options.addOption(OPT_GRAPH_SANDBERG);
        options.addOption(OPT_GRAPH_LATTICE);
        options.addOption(OPT_GRAPH_SUPER_NODE);

        // Graphs: link length distribution
        options.addOption(OPT_LINK_IDEAL);
        options.addOption(OPT_LINK_FLAT);
        options.addOption(OPT_LINK_CONFORMING);

        // Graphs: degree distribution-
        options.addOption(OPT_DEGREE_FIXED);
        options.addOption(OPT_DEGREE_CONFORMING);
        options.addOption(OPT_DEGREE_POISSON);

        // Simulations: Routing policies
        options.addOption(OPT_ROUTE);
        options.addOption(OPT_ROUTE_HOPS);
        options.addOption(OPT_ROUTE_OUTPUT);
        StringBuilder description = new StringBuilder(
                "Path folding policy used. Default is "
                        + FOLDING_DEFAULT.name() + ". Possible policies:");
        for (FoldingPolicy policy : FoldingPolicy.values()) {
            description.append(" ").append(policy);
        }
        OPT_ROUTE_FOLDING_POLICY.setDescription(description.toString());
        options.addOption(OPT_ROUTE_FOLDING_POLICY);
        options.addOption(OPT_ROUTE_LOOK_AHEAD);
        options.addOption(OPT_ROUTE_OLD_FOLDING);

        description = new StringBuilder("Routing policy used. Default is "
                + ROUTING_DEFAULT.name() + ". Possible policies:");
        for (RoutingPolicy policy : RoutingPolicy.values()) {
            description.append(" ").append(policy.name());
        }
        OPT_ROUTE_POLICY.setDescription(description.toString());
        options.addOption(OPT_ROUTE_POLICY);

        // options.addOption("H", "output-hops", true,
        // "Base filename to output hop histograms for each sink policy.
        // Appended with -<policy-num> for each.");
        options.addOption(OPT_ROUTE_BOOTSTRAP);
        options.addOption(OPT_ROUTE_LOOK_PREC);
        options.addOption(OPT_ROUTE_RANDOM_CHANCE);

        // Simulations: Probe distribution
        options.addOption(OPT_PROBE);
        options.addOption(OPT_PROBE_METROPOLIS_HASTINGS);
        options.addOption(OPT_PROBE_OUTPUT);

        description = new StringBuilder("Log level used. Default is "
                + LOGGING_DEFAULT.name() + ". Possible levels:");
        for (SimLogger.LogLevel level : SimLogger.LogLevel.values()) {
            description.append(" ").append(level.name());
        }
        OPT_LOG_LEVEL.setDescription(description.toString());
        options.addOption(OPT_LOG_LEVEL);

        return options;
    }

    /**
     * Parses command line arguments and validates them.
     * 
     * @param args
     *            Arguments to parse.
     * @return An Arguments instance with the parsed arguments, or null in the
     *         case of an error.
     * @throws Exception
     *             Error parsing arguments.
     */
    public static Arguments parse(final String[] args) throws Exception {
        final Options options = generateOptions();
        final CommandLineParser parser = new GnuParser();
        final CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(OPT_HELP.getLongOpt())) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar freenet-simulator.jar", options);
            return null;
        }

        if (cmd.hasOption(OPT_VERSION.getLongOpt())) {
            LOGGER.warning("Freenet Routing Simulator v 0.0.2-dev");
            return null;
        }

        int degreeOptions = 0;
        for (String option : new String[] { OPT_DEGREE_FIXED.getLongOpt(),
                OPT_DEGREE_CONFORMING.getLongOpt(),
                OPT_DEGREE_POISSON.getLongOpt() }) {
            if (cmd.hasOption(option)) {
                degreeOptions++;
            }
        }

        int linkOptions = 0;
        for (String option : new String[] { OPT_LINK_IDEAL.getLongOpt(),
                OPT_LINK_FLAT.getLongOpt(), OPT_LINK_CONFORMING.getLongOpt() }) {
            if (cmd.hasOption(option)) {
                linkOptions++;
            }
        }

        if (degreeOptions > 1 || linkOptions > 1) {
            LOGGER.severe("Graph cannot be generated with multiple methods at once.");
            return null;
        }

        /*
         * Determine graph generation method to use, and if exactly one was
         * specified correctly. Allowing more than one generation method to be
         * specified would make it ambiguous which one was used.
         */
        int validGeneration = 0;
        GraphGenerator graphGenerator = null;
        if (cmd.hasOption(OPT_GRAPH_LOAD.getLongOpt())) {
            validGeneration++;
            graphGenerator = GraphGenerator.LOAD;
        }

        if (cmd.hasOption(OPT_GRAPH_LOAD_DOT.getLongOpt())) {
            validGeneration++;
            graphGenerator = GraphGenerator.LOAD_DOT;
        }

        if (cmd.hasOption(OPT_GRAPH_LOAD_GML.getLongOpt())) {
            validGeneration++;
            graphGenerator = GraphGenerator.LOAD_GML;
        }

        if (cmd.hasOption(OPT_GRAPH_SUPER_NODE.getLongOpt())) {
            validGeneration++;
            graphGenerator = GraphGenerator.SUPER_NODE;
        }

        if (degreeOptions == 0 && linkOptions == 1
                && cmd.hasOption(OPT_GRAPH_SANDBERG.getLongOpt())) {
            validGeneration++;
            graphGenerator = GraphGenerator.SANDBERG;
        }

        if (degreeOptions == 1 && linkOptions == 1) {
            validGeneration++;
            graphGenerator = GraphGenerator.STANDARD;
        }

        if (!cmd.hasOption(OPT_GRAPH_SIZE.getLongOpt())
                && graphGenerator != GraphGenerator.LOAD
                && graphGenerator != GraphGenerator.LOAD_DOT
                && graphGenerator != GraphGenerator.LOAD_GML) {
            LOGGER.severe("Network size not specified. (--"
                    + OPT_GRAPH_SIZE.getLongOpt() + ")");
            return null;
        }

        /*
         * - Sandberg-graph requires link. - Load-graph does not require link or
         * degree. - Without these two, degree and link must be specified;
         * optionally lattice too.
         */
        if (validGeneration == 0 || validGeneration > 1) {
            StringBuilder b = new StringBuilder();
            b.append("Either zero or too many graph generation methods specified.\n");
            b.append("Valid graph generators are:\n");
            b.append(" * --" + OPT_GRAPH_LOAD.getLongOpt() + "\n");
            b.append(" * --" + OPT_GRAPH_SANDBERG.getLongOpt()
                    + " with --link-* and --" + OPT_GRAPH_SIZE.getLongOpt()
                    + "\n");
            b.append(" * --degree-*, --link-*, --"
                    + OPT_GRAPH_SIZE.getLongOpt() + ", and optionally --"
                    + OPT_GRAPH_LATTICE.getLongOpt() + "\n");
            b.append(" * --" + OPT_GRAPH_SUPER_NODE.getLongOpt() + " with --"
                    + OPT_GRAPH_SIZE.getLongOpt() + " and optionally --"
                    + OPT_GRAPH_LATTICE.getLongOpt() + "\n");
            LOGGER.severe(b.toString());
            return null;
        }

        // By this point a single valid graph generator should be specified.
        assert graphGenerator != null;

        if (cmd.hasOption(OPT_ROUTE.getLongOpt())
                && !cmd.hasOption(OPT_ROUTE_HOPS.getLongOpt())) {
            LOGGER.severe("--" + OPT_ROUTE.getLongOpt()
                    + " was specified, but not --"
                    + OPT_ROUTE_HOPS.getLongOpt() + ".");
            return null;
        }

        if (cmd.hasOption(OPT_PROBE.getLongOpt())
                && !cmd.hasOption(OPT_PROBE_OUTPUT.getLongOpt())) {
            LOGGER.severe("--" + OPT_PROBE.getLongOpt()
                    + " was specified, but not --"
                    + OPT_PROBE_OUTPUT.getLongOpt() + ".");
            return null;
        }

        final FoldingPolicy foldingPolicy;
        if (cmd.hasOption(OPT_ROUTE_FOLDING_POLICY.getLongOpt())) {
            try {
                foldingPolicy = FoldingPolicy.valueOf(cmd
                        .getOptionValue(OPT_ROUTE_FOLDING_POLICY.getLongOpt()));
            } catch (IllegalArgumentException e) {
                StringBuilder b = new StringBuilder();
                b.append("The folding policy \"");
                b.append(cmd.getOptionValue(OPT_ROUTE_FOLDING_POLICY
                        .getLongOpt()));
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
        if (cmd.hasOption(OPT_ROUTE_POLICY.getLongOpt())) {
            final String policy = cmd.getOptionValue(OPT_ROUTE_POLICY
                    .getLongOpt());
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

        // Check for problems with specified paths.
        // Check if input files can be read.
        final DataInputStream degreeInput, linkInput, graphInput;
        try {
            degreeInput = readableFile(OPT_DEGREE_CONFORMING.getLongOpt(), cmd);
            linkInput = readableFile(OPT_LINK_CONFORMING.getLongOpt(), cmd);
            if (cmd.hasOption(OPT_GRAPH_LOAD.getLongOpt())) {
                graphInput = readableFile(OPT_GRAPH_LOAD.getLongOpt(), cmd);
            } else if (cmd.hasOption(OPT_GRAPH_LOAD_DOT.getLongOpt())) {
                graphInput = readableFile(OPT_GRAPH_LOAD_DOT.getLongOpt(), cmd);
            } else {
                graphInput = readableFile(OPT_GRAPH_LOAD_GML.getLongOpt(), cmd);
            }
        } catch (FileNotFoundException e) {
            return null;
        }

        // Check if output paths are directories that can be written to, and
        // create them if they do not exist.
        if (cmd.hasOption(OPT_PROBE_OUTPUT.getLongOpt())
                && (writableDirectory(cmd.getOptionValue(OPT_PROBE_OUTPUT
                        .getLongOpt()))) == null) {
            return null;
        }

        // Check that output files exist and are writable or can be created.
        final DataOutputStream degreeOutput, linkOutput, graphOutput;
        final DataOutputStream graphOutputText, routingSimOutput;
        try {
            degreeOutput = writableFile(OPT_DEGREE_OUTPUT.getLongOpt(), cmd);
            linkOutput = writableFile(OPT_LINK_OUTPUT.getLongOpt(), cmd);
            graphOutput = writableFile(OPT_GRAPH_SAVE.getLongOpt(), cmd);
            graphOutputText = writableFile(OPT_GRAPH_SAVE_DOT.getLongOpt(), cmd);
            routingSimOutput = writableFile(OPT_ROUTE_OUTPUT.getLongOpt(), cmd);
        } catch (FileNotFoundException e) {
            return null;
        }

        final boolean lattice = cmd.hasOption(OPT_GRAPH_LATTICE.getLongOpt());
        final boolean fastGeneration = cmd.hasOption(OPT_GRAPH_FAST_LOCATION
                .getLongOpt());
        final boolean pause = cmd.hasOption(OPT_PAUSE.getLongOpt());
        final boolean scriptOutput = cmd.hasOption(OPT_SCRIPT_OUTPUT
                .getLongOpt());
        final boolean oldPathFolding = cmd.hasOption(OPT_ROUTE_OLD_FOLDING
                .getLongOpt());
        final int seed = cmd.hasOption(OPT_SEED.getLongOpt()) ? Integer
                .valueOf(cmd.getOptionValue(OPT_SEED.getLongOpt()))
                : (int) System.currentTimeMillis();
        final int networkSize = cmd.hasOption(OPT_GRAPH_SIZE.getLongOpt()) ? Integer
                .valueOf(cmd.getOptionValue(OPT_GRAPH_SIZE.getLongOpt())) : 0;
        final int nRequests = cmd.hasOption(OPT_ROUTE.getLongOpt()) ? Integer
                .valueOf(cmd.getOptionValue(OPT_ROUTE.getLongOpt())) : 0;
        final int maxHopsProbe = cmd.hasOption(OPT_PROBE.getLongOpt()) ? Integer
                .valueOf(cmd.getOptionValue(OPT_PROBE.getLongOpt())) : 0;
        final int maxHopsRequest = cmd.hasOption(OPT_ROUTE_HOPS.getLongOpt()) ? Integer
                .valueOf(cmd.getOptionValue(OPT_ROUTE_HOPS.getLongOpt())) : 0;
        final int shortcuts = cmd.hasOption(OPT_GRAPH_SANDBERG.getLongOpt()) ? Integer
                .valueOf(cmd.getOptionValue(OPT_GRAPH_SANDBERG.getLongOpt()))
                : 0;
        final int nLookAhead = cmd.hasOption(OPT_ROUTE_LOOK_AHEAD.getLongOpt()) ? Integer
                .valueOf(cmd.getOptionValue(OPT_ROUTE_LOOK_AHEAD.getLongOpt()))
                : 1;
        final double precisionLoss = cmd.hasOption(OPT_ROUTE_LOOK_PREC
                .getLongOpt()) ? Double.valueOf(cmd
                .getOptionValue(OPT_ROUTE_LOOK_PREC.getLongOpt())) : 0;
        final double randomRouteChance = cmd.hasOption(OPT_ROUTE_RANDOM_CHANCE
                .getLongOpt()) ? Double.valueOf(cmd
                .getOptionValue(OPT_ROUTE_RANDOM_CHANCE.getLongOpt())) : 0;
        final String logLevel = cmd.hasOption(OPT_LOG_LEVEL.getLongOpt()) ? cmd
                .getOptionValue(OPT_LOG_LEVEL.getLongOpt()) : LOGGING_DEFAULT
                .name();

        return new Arguments(lattice, fastGeneration, cmd.hasOption(OPT_PROBE
                .getLongOpt()), cmd.hasOption(OPT_PROBE_METROPOLIS_HASTINGS
                .getLongOpt()), cmd.hasOption(OPT_ROUTE.getLongOpt()),
                cmd.hasOption(OPT_LINK_EXCLUDE_LATTICE.getLongOpt()),
                cmd.hasOption(OPT_ROUTE_BOOTSTRAP.getLongOpt()), seed,
                networkSize, shortcuts, maxHopsProbe, maxHopsRequest,
                nRequests, precisionLoss, randomRouteChance, graphGenerator,
                degreeInput, linkInput, graphInput, degreeOutput, linkOutput,
                graphOutput, graphOutputText,
                cmd.getOptionValue(OPT_PROBE_OUTPUT.getLongOpt()),
                routingSimOutput, foldingPolicy, routingPolicy, nLookAhead,
                logLevel, pause, scriptOutput, oldPathFolding, cmd);
    }
}

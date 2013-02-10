package org.freenetproject.routing_simulator.graph.linklength;

import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.graph.node.SimpleNode;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Logger;

public class ConformingLinkSource extends LinkLengthSource {
    private static final Logger LOGGER = Logger
            .getLogger(ConformingLinkSource.class.getName());
    private final ArrayList<Double> lengths;

    /**
     * @see LinkLengthSource#LinkLengthSource(org.apache.commons.math3.random.
     *      RandomGenerator, java.util.ArrayList
     */
    public ConformingLinkSource(DataInputStream input, RandomGenerator random,
            ArrayList<SimpleNode> nodes) {
        super(random, nodes);

        lengths = new ArrayList<Double>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    input));
            // TODO: Read all, put into ArrayList, get a link length selects
            // from that.
            String line;
            // TODO: This seems like a C++ way of doing things. What's the Java
            // way?
            while ((line = reader.readLine()) != null) {
                // File format has link length as first value, separated by a
                // space.
                lengths.add(Double.valueOf(line.split(" ")[0]));
            }
        } catch (IOException e) {
            LOGGER.severe(e.toString());
            System.exit(2);
        }
    }

    @Override
    public SimpleNode getPeer(SimpleNode from) {
        return closestTo(from, lengths.get(random.nextInt(lengths.size())));
    }
}

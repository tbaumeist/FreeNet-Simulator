package test.org.freenetproject.routing_simulator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.freenetproject.routing_simulator.graph.Graph;

public class Test_Helper {
    private final static String resourcePath = "bin/test/org/freenetproject/routing_simulator/resources/";
    
    private Test_Helper() {
    }
    
    public static String getResourcePath(){
            return new java.io.File("").getAbsolutePath() + File.separator + resourcePath;
    }
    
    public static String getResourcePath(String resource){
            return getResourcePath() + resource;
    }
    
    /**
     * Deletes the destination file, if it exists, and writes the graph to it.
     * 
     * @param graph
     *            Graph to write.
     * @param destination
     *            File to write the graph to.
     * @throws IOException
     */
    public static void writeToFile(final Graph graph, final File destination)
            throws Exception {
        assert !destination.exists() || destination.delete();
        final DataOutputStream outputStream = new DataOutputStream(
                new FileOutputStream(destination));
        graph.write(outputStream);
    }
    
    public static void writeToFileDot(final Graph graph, final File destination)
            throws Exception {
        assert !destination.exists() || destination.delete();
        final OutputStream outputStream = new FileOutputStream(destination);
        graph.writeDot(outputStream);
    }

    public static Graph readFromFile(final File source) throws Exception {
        assert source.exists();
        final DataInputStream inputStream = new DataInputStream(
                new FileInputStream(source));
        return Graph.read(inputStream, getRandom());
    }
    
    public static Graph readFromFileDot(final File source) throws Exception {
        assert source.exists();
        final InputStream inputStream = new FileInputStream(source);
        return Graph.readDot(inputStream, getRandom());
    }
    
    public static Graph readFromFileGml(final File source) throws Exception {
        assert source.exists();
        final InputStream inputStream = new FileInputStream(source);
        return Graph.readGml(inputStream, getRandom());
    }
    
    /**
     * @return A consistent, fresh randomness source.
     */
    public static RandomGenerator getRandom() {
        return new MersenneTwister(0);
    }
}

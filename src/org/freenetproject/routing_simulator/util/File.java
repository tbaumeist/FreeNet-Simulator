package org.freenetproject.routing_simulator.util;

import org.apache.commons.cli.CommandLine;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Logger;

/**
 * Convenience file methods.
 */
public final class File {
    /**
     * Message logger
     */
    private static final Logger LOGGER = Logger.getLogger(File.class.getName());

    /**
     * Private constructor.
     */
    private File() {
    }

    /**
     * Checks that a path is a directory which can be written to, and attempts
     * to create it if it does not exist. Outputs descriptive messages if given
     * anything other than an existing writable directory.
     * 
     * @param path
     *            path to check
     * @return The directory if it (now) exists and is writable; null in the
     *         case of an error.
     */
    public static java.io.File writableDirectory(final String path) {
        final java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                LOGGER.severe("Unable to create the output directory \""
                        + file.getAbsolutePath() + "\".");
                return null;
            } else {
                LOGGER.warning("The output directory \""
                        + file.getAbsolutePath()
                        + "\" did not exist, so it was created.");
            }
        } else if (!file.isDirectory()) {
            LOGGER.severe("The output path \"" + file.getAbsolutePath()
                    + "\" is not a directory as expected.");
            return null;
        } else if (!file.canWrite()) {
            LOGGER.severe("No write access to the output directory \""
                    + file.getAbsolutePath() + "\".");
            return null;
        }
        return file;
    }

    /**
     * Wrapper for readableFile.
     * 
     * @return an input stream from the file, or null if the option is not
     *         specified.
     * @throws FileNotFoundException
     *             if the option was specified but the file was not found.
     */
    public static DataInputStream readableFile(final String option,
            final CommandLine cmd) throws FileNotFoundException {
        if (!cmd.hasOption(option)) {
            return null;
        }
        final java.io.File file = new java.io.File(cmd.getOptionValue(option));
        try {
            return new DataInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            LOGGER.severe("Cannot read \"" + file.getAbsolutePath()
                    + "\" as a file.");
            throw e;
        }
    }

    /**
     * Wrapper for writableFile.
     * 
     * @param option
     *            option to check for a path to a file.
     * @param cmd
     *            command line to check for the given option.
     * @return an output stream to the file, or null if the option is not
     *         specified.
     */
    public static DataOutputStream writableFile(final String option,
            final CommandLine cmd) throws FileNotFoundException {
        if (!cmd.hasOption(option)) {
            return null;
        }
        final java.io.File file = new java.io.File(cmd.getOptionValue(option));
        try {
            return new DataOutputStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            LOGGER.severe("Unable to open \"" + file.getAbsolutePath()
                    + "\" for output:");
            throw e;
        }
    }
    
    public static FileOutputStream writableFile2(final String option,
            final CommandLine cmd) throws FileNotFoundException {
        if (!cmd.hasOption(option)) {
            return null;
        }
        final java.io.File file = new java.io.File(cmd.getOptionValue(option));
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            LOGGER.severe("Unable to open \"" + file.getAbsolutePath()
                    + "\" for output:");
            throw e;
        }
    }
}

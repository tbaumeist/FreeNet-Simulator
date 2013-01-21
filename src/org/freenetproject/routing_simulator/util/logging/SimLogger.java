package org.freenetproject.routing_simulator.util.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to configure the logger.
 */
public final class SimLogger {

    /**
     * Private constructor.
     */
    private SimLogger() {
    }

    /**
     * Logging levels.
     */
    public enum LogLevel {
        /**
         * Normal level of logging.
         */
        REGULAR,
        /**
         * More detailed log messages.
         */
        DETAILED
    }

    /**
     * Setup the logger.
     * 
     * @throws Exception
     *             Error configuring the logger.
     */
    public static void setup() throws Exception {
        setup(LogLevel.REGULAR);
    }

    /**
     * Setup the logger.
     * 
     * @param level
     *            Loggging level.
     * @throws Exception
     *             Error configuring the logger.
     */
    public static void setup(final String level) throws Exception {
        String cleanLevel = level.toUpperCase().trim();
        setup(LogLevel.valueOf(cleanLevel));
    }

    /**
     * Setup the logger.
     * 
     * @param level
     *            Logging level to use.
     * @throws Exception
     *             Error setting up the logger.
     */
    public static void setup(final LogLevel level) throws Exception {
        Logger logger = Logger.getLogger("");

        switch (level) {
        case DETAILED:
            logger.setLevel(Level.INFO);
            break;
        default:
            logger.setLevel(Level.WARNING);
        }

        logger.getHandlers()[0].setFormatter(new SimFormatter());
        logger.getHandlers()[0].setLevel(Level.ALL);

        // FileHandler file = new FileHandler("log.txt");
        // file.setFormatter(new SimpleFormatter());
        // logger.addHandler(file);
    }
}

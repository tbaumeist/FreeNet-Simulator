package org.freenetproject.routing_simulator.util.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SimLogger {
	
	public enum LogLevel{
		REGULAR,
		DETAILED
	}

	public static void setup() throws Exception {
		setup(LogLevel.REGULAR);
	}
	
	public static void setup(String level) throws Exception {
		level = level.toUpperCase().trim();
		setup(LogLevel.valueOf(level));
	}

	public static void setup(LogLevel level) throws Exception {
		Logger logger = Logger.getLogger("");
		
		switch(level){
		case DETAILED:
			logger.setLevel(Level.ALL);
			break;
		default:
			logger.setLevel(Level.INFO);
		}

		logger.getHandlers()[0].setFormatter(new SimFormatter());
		logger.getHandlers()[0].setLevel(Level.ALL);

		// FileHandler file = new FileHandler("log.txt");
		// file.setFormatter(new SimpleFormatter());
		// logger.addHandler(file);
	}
}

package org.freenetproject.routing_simulator.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Collection of generic array utility functions.
 * 
 */
public final class ArrayUtil {

	/**
	 * Private constructor.
	 * 
	 * @throws Exception
	 *             Do not call this constructor.
	 */
	private ArrayUtil() throws Exception {
		throw new Exception("Not implemented");
	}

	/**
	 * To String an array's contents.
	 * 
	 * @param array
	 *            The array to convert to a string.
	 * @return String representations of the array.
	 */
	public static String stringArray(final int[] array) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			s.append(i).append("\t").append(array[i]).append("\n");
		}
		return s.toString();
	}

	/**
	 * Write an array's contents to a file.
	 * 
	 * @param array
	 *            Array to write to file.
	 * @param target
	 *            File to write array to.
	 * @throws IOException
	 *             Error writing the file.
	 */
	public static void writeArray(final int[] array, final File target)
			throws IOException {
		FileOutputStream outputStream = new FileOutputStream(target);
		outputStream.write(stringArray(array).getBytes("UTF-8"));
		outputStream.close();
	}
}

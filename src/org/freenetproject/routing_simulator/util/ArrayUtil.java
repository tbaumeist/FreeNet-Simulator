package org.freenetproject.routing_simulator.util;

public class ArrayUtil {
	public static String stringArray(int[] array) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < array.length; i++)
			s.append(i).append("\t").append(array[i]).append("\n");
		return s.toString();
	}
}

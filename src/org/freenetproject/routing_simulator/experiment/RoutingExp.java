package org.freenetproject.routing_simulator.experiment;

import org.freenetproject.routing_simulator.util.ArrayUtil;

public class RoutingExp {
	private int successes = 0, disconnectedFolding = 0,
			disconnectedBootstrap = 0, totalSuccessPathLength = 0,
			totalSuccessTravelPathLength = 0, nRequests = 0,
			foldingOperations = 0, maxHTL = 0;
	private int[] pathLengthDist;

	public RoutingExp(int maxHTL, int nRequests) {
		this.maxHTL = maxHTL;
		this.pathLengthDist = new int[maxHTL + 1];
		this.nRequests = nRequests;
	}

	public void record(boolean successful, int pathLength,
			int traveledPathLength) {
		if (successful) {
			this.successes++;
			this.pathLengthDist[pathLength]++;
			this.totalSuccessPathLength += pathLength;
			this.totalSuccessTravelPathLength += traveledPathLength;
		}
	}

	public void disconnectedFolding(int count) {
		this.disconnectedFolding += count;
	}

	public void disconnectBootStrap() {
		this.disconnectedBootstrap++;
	}

	public void foldingOperations(int operations) {
		this.foldingOperations += operations;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("Routing simulation results\n\n");

		b.append("Disconnected from folding :     \t ").append(
				disconnectedFolding);
		b.append("\n");
		b.append("Disconnected from bootstrapping : \t").append(
				disconnectedBootstrap);
		b.append("\n");
		b.append("Path folding operations :         \t").append(
				this.foldingOperations);
		b.append("\n");
		b.append("Routing success rate :            \t").append(
				(double) successes / nRequests * 100);
		b.append("%\n");
		b.append("Routing requests count :          \t").append(nRequests);
		b.append("\n");
		b.append("\tSuccessful routing request count : \t").append(successes);
		b.append("\n");
		b.append("\tFailed routing request count :     \t").append(
				nRequests - successes);
		b.append("\n");

		b.append("\n* Note failed requests are not included in the stats below *\n\n");
		b.append("Maximum path length :             \t").append(this.maxHTL);
		b.append("\n");
		b.append("Mean successful path length :     \t").append(
				(double) totalSuccessPathLength / successes);
		b.append("\n");
		b.append("Mean successful traveled path length (back-tracking):     \t").append(
				(double) totalSuccessTravelPathLength / successes);
		b.append("\n");
		b.append("\n");

		b.append("Successful Request Path Length Distribution\nLength Count\n");
		b.append(ArrayUtil.stringArray(this.pathLengthDist));

		return b.toString();
	}
}

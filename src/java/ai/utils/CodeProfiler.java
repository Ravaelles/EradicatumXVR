package ai.utils;

import java.util.HashMap;

public class CodeProfiler {

	private static HashMap<String, Long> aspectsStart = new HashMap<>();
	private static HashMap<String, Double> aspectsLength = new HashMap<>();

	public static void startMeasuring(String title) {
		measureAspect(title);
	}

	private static void measureAspect(String title) {
		// if (!aspectsStart.containsKey(title)) {
		// aspectsStart.put(title, now());
		// } else {
		aspectsStart.put(title, now());
		// }
	}

	private static long now() {
		return System.nanoTime();
	}

	public static void endMeasuring(String title) {
		long measured = now() - aspectsStart.get(title);

		if (!aspectsLength.containsKey(title)) {
			aspectsLength.put(title, (double) measured);
		} else {
			aspectsLength.put(title, aspectsLength.get(title) * 0.6 + measured * 0.4);
		}
	}

	public static HashMap<String, Double> getAspectsTimeConsumption() {
		return aspectsLength;
	}

}

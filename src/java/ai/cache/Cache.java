package ai.cache;

import ai.handling.map.MapPoint;

public class Cache {

	public static MapPoint safePoint;

	// =========================================================

	public static void clearCache() {
		safePoint = null;
	}

}

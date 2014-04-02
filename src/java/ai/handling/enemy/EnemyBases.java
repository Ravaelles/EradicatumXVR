package ai.handling.enemy;

import jnibwapi.model.BaseLocation;
import jnibwapi.model.Map;
import ai.core.XVR;
import ai.handling.map.MapPoint;

public class EnemyBases {

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static BaseLocation getNearestBaseLocationForEnemy(MapPoint enemyBase) {
		MapPoint expansionCenter = enemyBase;
		// if (!xvr.getLastBase().equals(xvr.getFirstBase())) {
		// expansionCenter = xvr.getLastBase();
		// }
		if (expansionCenter == null) {
			return null;
		}
		Map map = xvr.getBwapi().getMap();
		BaseLocation nearestFreeBaseLocation = null;
		double nearestDistance = 999999;
		for (BaseLocation location : xvr.getBwapi().getMap().getBaseLocations()) {
			if (isLocationStartLocation(location)) {
				continue;
			}

			// Check if the new base is connected to the main base by land.
			// Region newBaseRegion = xvr.getBwapi().getMap()
			// .getRegion(location.getX(), location.getY());
			// if (!map.isConnected(location, expansionCenter)) {
			// continue;
			// }

			// Look for for the closest base and remember it.
			double distance = map.getGroundDistance(location, expansionCenter) / 32;
			if (distance < 0) { // -1 means there's no path
				continue;
			}

			// double distance = xvr.getDistanceBetween(location.getX(),
			// location.getY(), mainBase.getX(), mainBase.getY());
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestFreeBaseLocation = location;
			}
		}

		return nearestFreeBaseLocation;
	}

	private static boolean isLocationStartLocation(BaseLocation location) {
		for (MapPoint point : xvr.getBwapi().getMap().getStartLocations()) {
			if (location.distanceTo(point) < 7) {
				return true;
			}
		}
		return false;
	}

}

package ai.managers.units;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;

public class FlyerManager {

	private static final int MIN_DIST_BETWEEN_FLYERS = 3;
	private static XVR xvr = XVR.getInstance();

	// =========================

	public static void act(Unit unit) {
		if (unit.isHidden()) {

			// TOP PRIORITY: Act when enemy detector or some AA building is
			// nearby: just run away, no matter what.
			if (UnitActions.runFromEnemyDetectorOrDefensiveBuildingIfNecessary(unit, true, true,
					true)) {
				return;
			}
		} else {
			if (UnitActions.runFromEnemyDetectorOrDefensiveBuildingIfNecessary(unit, false, true,
					true)) {
				return;
			}
		}

		// =====================
		// Avoid units like: photon cannons, missile turrets, goliaths, marines.
		boolean isAABuildingNearby = xvr.isEnemyDefensiveAirBuildingNear(unit);
		boolean isAAUnitNearby = isAAUnitNearby(unit);
		boolean shouldRunFromHere = isAABuildingNearby || isAAUnitNearby;
		if (shouldRunFromHere && !unit.isStartingAttack()) {
			UnitActions.moveToSafePlace(unit);
		}

		// =====================
		// Move away from other flyers, thus enhancing the range
		Unit otherFlyer = getNearestFlyerToFlyer(unit);
		if (otherFlyer != null && otherFlyer.distanceTo(unit) < MIN_DIST_BETWEEN_FLYERS) {
			UnitActions.moveAwayFromUnitIfPossible(unit, otherFlyer, 4);
		}
	}

	private static Unit getNearestFlyerToFlyer(Unit unit) {
		ArrayList<Unit> airUnits = xvr.getUnitsArmyFlyers();
		airUnits.remove(unit);
		return xvr.getUnitNearestFromList(unit, airUnits);
	}

	private static boolean isAAUnitNearby(MapPoint point) {
		Unit nearestAntiAirUnit = xvr.getUnitNearestFromList(point, xvr.getEnemyAntiAirUnits());
		if (nearestAntiAirUnit != null) {
			int maxEnemyRange = nearestAntiAirUnit.getType().getAirWeapon().getMaxRangeInTiles();
			if (point.distanceTo(nearestAntiAirUnit) <= maxEnemyRange + 1.5) {
				return true;
			}
		}
		return false;
	}

}

package ai.managers.units;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.army.ArmyPlacing;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.terran.TerranWraith;

public class FlyerManager {

	private static final int MIN_DIST_BETWEEN_FLYERS = 3;
	private static XVR xvr = XVR.getInstance();

	// =========================

	public static void act(Unit unit) {
		UnitType type = unit.getType();

		// ==============================
		// SPECIAL units

		// Wraith
		if (type.isWraith()) {
			TerranWraith.act(unit);
		}

		// ==============================

		// Define place where to be
		MapPoint point = ArmyPlacing.getFlyersGatheringPoint();
		UnitActions.attackTo(unit, point);

		// ==============================
		// Move away from other flyers, thus enhancing the range
		boolean isAAUnitNearby = isAAUnitNearby(unit);
		Unit otherFlyer = getNearestFlyerToFlyer(unit);
		if (!isAAUnitNearby && otherFlyer != null
				&& otherFlyer.distanceTo(unit) < MIN_DIST_BETWEEN_FLYERS) {
			UnitActions.moveAwayFromUnitIfPossible(unit, otherFlyer, 4);
		}

		// ==============================
		// Avoid units like: photon cannons, missile turrets, goliaths, marines.
		boolean isAABuildingNearby = xvr.isEnemyDefensiveAirBuildingNear(unit);
		boolean shouldRunFromHere = isAABuildingNearby || isAAUnitNearby;
		if (shouldRunFromHere && !unit.isStartingAttack()) {
			UnitActions.moveToSafePlace(unit);
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

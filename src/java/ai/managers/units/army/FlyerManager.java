package ai.managers.units.army;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.army.ArmyPlacing;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.terran.TerranWraith;

public class FlyerManager {

	private static final int MIN_DIST_BETWEEN_FLYERS = 4;
	private static XVR xvr = XVR.getInstance();

	// =========================================================

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

		// =========================================================
		// Avoid anti-air units
		if (tryAvoidingAntiAirUnits(unit)) {
			return;
		}

		// ==============================
		// Move away from other flyers, thus enhancing the range
		Unit aaUnitNearby = getAAUnitNearby(unit);
		unit.setEnemyNearbyAA(aaUnitNearby);
		Unit otherFlyer = getNearestFlyerToFlyer(unit);
		if (aaUnitNearby == null && otherFlyer != null
				&& otherFlyer.distanceTo(unit) < MIN_DIST_BETWEEN_FLYERS) {
			UnitActions.moveAwayFromUnitIfPossible(unit, otherFlyer, 5);
		}

		// ==============================
		// Avoid units like: photon cannons, missile turrets, goliaths, marines.
		boolean isAABuildingNearby = xvr.isEnemyDefensiveAirBuildingNear(unit);
		boolean shouldRunFromHere = isAABuildingNearby || aaUnitNearby != null;
		if (shouldRunFromHere) {
			UnitActions.moveAwayFromUnit(unit, aaUnitNearby);
			return;
		}
	}

	private static boolean tryAvoidingAntiAirUnits(Unit unit) {
		Unit aaUnitNearby = getAAUnitNearby(unit);
		if (aaUnitNearby != null) {
			UnitActions.moveAwayFromUnit(unit, aaUnitNearby);
			unit.setAiOrder("Fly away from AA");
			return true;
		}
		return false;
	}

	// =========================================================

	private static Unit getNearestFlyerToFlyer(Unit unit) {
		ArrayList<Unit> airUnits = xvr.getUnitsArmyFlyers();
		airUnits.remove(unit);
		return xvr.getUnitNearestFromList(unit, airUnits);
	}

	private static Unit getAAUnitNearby(MapPoint point) {
		Unit nearestAntiAirUnit = xvr.getUnitNearestFromList(point, xvr.getEnemyAntiAirUnits());
		if (nearestAntiAirUnit != null) {
			int maxEnemyRange = nearestAntiAirUnit.getType().getAirWeapon().getMaxRangeInTiles();
			if (point.distanceTo(nearestAntiAirUnit) <= maxEnemyRange + 2.2) {
				return nearestAntiAirUnit;
			}
		}
		return null;
	}

}

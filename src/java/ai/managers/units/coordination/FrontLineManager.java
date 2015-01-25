package ai.managers.units.coordination;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.strength.StrengthComparison;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitManager;

public class FrontLineManager {

	public static final int MODE_FRONT_GUARD = 8;
	public static final int MODE_VANGUARD = 15;

	private static final int VANGUARD_SEPARATION_DISTANCE = 3;

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static void actOffensively(Unit unit, int mode) {
		if (mode == MODE_VANGUARD) {
			actInBack(unit);
		} else {
			actInFront(unit);
		}
	}

	// =========================================================

	private static void actInFront(Unit unit) {
		proceedButKeepTheFrontline(unit, MODE_FRONT_GUARD, VANGUARD_SEPARATION_DISTANCE);
	}

	private static void actInBack(Unit unit) {
		proceedButKeepTheFrontline(unit, MODE_VANGUARD, 0);
	}

	// =========================================================

	private static void proceedButKeepTheFrontline(Unit unit, int mode, int maxDistBonus) {
		MapPoint offensivePoint = ArmyRendezvousManager.getOffensivePoint();

		// If target is invalid or we're very close to target, spread out.
		if (offensivePoint == null || offensivePoint.distanceTo(unit) < 4.5) {
			UnitActions.spreadOutRandomly(unit);
		}

		// Target is valid, but it's still far. Proceed forward.
		unit.setAiOrder("Proceed");
		UnitActions.attackTo(unit, offensivePoint);

		if (unit.isTank() && unit.getGroundWeaponCooldown() < 1) {
			unit.unsiege();
		}

		if (StrategyManager.FORCE_CRAZY_ATTACK) {
			return;
		}

		// =========================================================
		// KEEP THE LINE, ADVANCE PROGRESSIVELY
		double allowedMaxDistance = StrategyManager.getAllowedDistanceFromSafePoint();

		// Include bonus to max distance for front guard.
		allowedMaxDistance += maxDistBonus;

		// =========================================================
		// Unit has advanced, but is too far behind the front line.
		if (isUnitOutOfLine(unit, allowedMaxDistance)) {
			actionUnitTooFarBehindTheFrontLine(unit, mode, allowedMaxDistance);
		}
	}

	private static boolean isUnitOutOfLine(Unit unit, double allowedMaxDistance) {
		int maxDistToTanks = 4;

		MapPoint defensivePoint = ArmyRendezvousManager.getDefensivePointForTanks();
		double distanceToDefensivePoint = defensivePoint.distanceTo(unit);
		if (distanceToDefensivePoint > allowedMaxDistance && distanceToDefensivePoint > 10
				&& isFarFromSafePoint(unit)) {
			int tanksNear = xvr.countUnitsOfGivenTypeInRadius(
					UnitTypes.Terran_Siege_Tank_Siege_Mode, maxDistToTanks, unit, true)
					+ xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Tank_Mode,
							maxDistToTanks, unit, true);
			if (tanksNear == 0 && StrengthComparison.getEnemySupply() >= 35
					&& xvr.getSuppliesFree() <= 20) {
				return true;
			}
		}

		Unit nearestTank = xvr.getNearestTankTo(unit);
		if (nearestTank != null && nearestTank.distanceTo(unit) >= maxDistToTanks) {
			return true;
		}

		return false;
	}

	// =========================================================

	private static boolean isFarFromSafePoint(Unit unit) {
		return unit.distanceTo(xvr.getFirstBase()) > 36
				|| xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Bunker, 6, unit, true) == 0;
	}

	// =========================================================

	private static void actionUnitTooFarBehindTheFrontLine(Unit unit, int mode,
			double allowedMaxDistance) {
		if (unit.isSieged()) {
			unit.unsiege();
		}

		if (!UnitManager._forceSpreadOut) {
			actionKeepTheLine(unit);
		}

		unit.setAiOrder("Back off");

		// // If unit is way too far than allowed, go back
		// if (allowedMaxDistance - distanceToDefensivePoint > 4) {
		// actionKeepTheLine(unit);
		// unit.setAiOrder("Back off");
		// }
		//
		// // Unit isn't too far behind the line
		// else {
		//
		// // If this unit is in vanguard, make it wait
		// if (mode == MODE_VANGUARD) {
		// UnitActions.holdPosition(unit);
		// unit.setAiOrder("Wait");
		// }
		//
		// // If unit is in front guard, it should back off a little bit
		// else {
		// if (unit.isSieged()) {
		// unit.unsiege();
		// }
		// UnitActions.moveToSafePlace(unit);
		// unit.setAiOrder(null);
		// }
		// }
	}

	private static void actionKeepTheLine(Unit unit) {
		if (StrengthComparison.getEnemySupply() < 40) {
			return;
		}

		MapPoint rendezvousTankForGroundUnits = ArmyRendezvousManager
				.getRendezvousTankForGroundUnits();

		double minDistToTanks = 1.5;
		double maxDistToTanks = 4.5;
		// int tanksNear =
		// xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode,
		// maxDistToTanks, unit, true)
		// +
		// xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Tank_Mode,
		// maxDistToTanks, unit, true);

		if (rendezvousTankForGroundUnits != null) {

			// Check if we're too far
			if (unit.distanceTo(rendezvousTankForGroundUnits) > maxDistToTanks) {
				UnitActions.attackTo(unit, rendezvousTankForGroundUnits);
			}

			// Check if we're too close
			else if (unit.distanceTo(rendezvousTankForGroundUnits) < minDistToTanks) {
				UnitActions.attackTo(unit, rendezvousTankForGroundUnits);
			}
		}
	}

	// if (rendezvousTankForGroundUnits != null) {
	// if (tanksNear < 1 || (unit.isTank() && tanksNear < 2)) {
	// int ourUnitsAround = xvr.countUnitsOursInRadius(unit, 4);
	// if (ourUnitsAround <= 4 && !isLuckyLibero(unit)
	// && unit.distanceTo(rendezvousTankForGroundUnits) > 5) {
	// MapPoint location = rendezvousTankForGroundUnits.translate(
	// -130 + RUtilities.rand(0, 260), -130 + RUtilities.rand(0, 260));
	// UnitActions.attackTo(unit, location);
	// } else if (!unit.isMoving() && !unit.isAttacking() &&
	// !unit.isBeingRepaired()) {
	// UnitActions.spreadOutRandomly(unit);
	// }
	// }
	// } else {
	// UnitActions.spreadOutRandomly(unit);
	// }
	// }

	// =========================================================

	// private static boolean isLuckyLibero(Unit unit) {
	// return unit.getID() % 7 == 0;
	// }
}

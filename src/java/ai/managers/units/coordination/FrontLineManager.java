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

	private static final double VANGUARD_SEPARATION_DISTANCE = 4;
	private static boolean DISPLAY_DEBUG = false;

	// =========================================================

	public static void actOffensively(Unit unit) {
		int mode = defineFrontModeForUnit(unit);

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

	private static void proceedButKeepTheFrontline(Unit unit, int mode, double maxDistBonus) {
		MapPoint offensivePoint = ArmyRendezvousManager.getOffensivePoint();

		// If target is invalid or we're very close to target, spread out.
		if (trySpreadingOutIfNeeded(unit, offensivePoint)) {
			if (DISPLAY_DEBUG) {
				unit.setAiOrder("Spread out");
			}
			return;
		}

		// =========================================================
		// Check if didn't go to far

		if (shouldWaitWithMovingForward(unit)) {
			return;
		}

		// =========================================================

		if (unit.isTank()) {
			handleTankMoveForward(unit, offensivePoint);
		} else {
			handleMoveForward(unit, offensivePoint);
		}

		// =========================================================

		// Make units stick together
		handleDontSeparateTooMuch(unit);

		// =========================================================

		// Keep the front line, advance step by step, but if we're forcing crazy
		// attack,
		// it means just attack without any unit coordination.
		if (!StrategyManager.FORCE_CRAZY_ATTACK) {
			handleKeepTheFrontLine(unit, offensivePoint, maxDistBonus, mode);
		}
	}

	private static boolean shouldWaitWithMovingForward(Unit unit) {
		double allowedMaxDistance = StrategyManager.getAllowedDistanceFromSafePoint();
		MapPoint defensivePoint = ArmyRendezvousManager.getDefensivePointForTanks();
		double distanceToDefensivePoint = defensivePoint.distanceTo(unit);
		if (distanceToDefensivePoint > allowedMaxDistance && distanceToDefensivePoint > 8
				&& isFarFromSafePoint(unit)) {
			// int tanksNear = xvr.countUnitsOfGivenTypeInRadius(
			// UnitTypes.Terran_Siege_Tank_Siege_Mode, maxDistToTanks, unit,
			// true)
			// +
			// xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Tank_Mode,
			// maxDistToTanks, unit, true);

			// (StrengthComparison.getEnemySupply() >= 35 ||
			// xvr.getTimeSeconds() < 1000)
			// &&
			if (xvr.getSuppliesUsed() < 190) {
				return true;
			}
		}

		return false;
	}

	// =========================================================

	private static void handleDontSeparateTooMuch(Unit unit) {
		boolean enoughTanksNearby = xvr.countTanksOurInRadius(unit, 5) >= 2
				|| xvr.countTanksOurInRadius(unit, 6) >= 3;
		if (!enoughTanksNearby) {
			// Unit rendezvousTank = xvr.getNearestTankTo(unit);
			MapPoint rendezvousTank = ArmyRendezvousManager.getRendezvousTankForGroundUnits();
			if (rendezvousTank != null && rendezvousTank.distanceTo(unit) >= 4) {
				UnitActions.attackTo(unit, rendezvousTank.translate(64, 40));
				if (DISPLAY_DEBUG) {
					unit.setAiOrder("Wait for tank");
				}
			}
		}
	}

	private static void handleTankMoveForward(Unit unit, MapPoint offensivePoint) {
		if (unit.isSieged() && unit.getGroundWeaponCooldown() < 1
				&& xvr.getEnemyNearestTo(unit, true, false) == null) {
			unit.unsiege();
		}
	}

	private static void handleMoveForward(Unit unit, MapPoint offensivePoint) {
		if (DISPLAY_DEBUG) {
			unit.setAiOrder("Proceed");
		}
		UnitActions.attackTo(unit, offensivePoint);
	}

	private static void handleKeepTheFrontLine(Unit unit, MapPoint offensivePoint,
			double maxDistBonus, int mode) {

		// =========================================================
		// Unit has advanced, but is too far behind the front line.
		if (isUnitOutOfLine(unit)) {
			actionUnitTooFarBehindTheFrontLine(unit, mode);
		}
	}

	private static boolean isUnitOutOfLine(Unit unit) {
		double manyUnitsBonus = Math.max(3, xvr.countUnitsOursInRadius(unit, 2.8) / 1.7);
		double maxDistToTanks = 3.3 + manyUnitsBonus;

		// =========================================================

		Unit nearestTank = xvr.getNearestTankTo(unit);
		if (nearestTank != null && nearestTank.distanceTo(unit) >= maxDistToTanks) {

			// Ensure that unit isn't foo far only because the units are very
			// stacked
			if (xvr.countUnitsOursInRadius(unit, 4.2) >= 9) {
				return false;
			} else {
				return true;
			}
		}

		return false;
	}

	// =========================================================

	private static boolean isFarFromSafePoint(Unit unit) {
		return unit.distanceTo(xvr.getFirstBase()) > 36
				|| xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Bunker, 6, unit, true) == 0;
	}

	// =========================================================

	private static void actionUnitTooFarBehindTheFrontLine(Unit unit, int mode) {
		if (unit.isSieged()) {
			unit.unsiege();
		}

		if (!UnitManager._forceSpreadOut) {
			actionKeepTheLine(unit);
		}

		if (DISPLAY_DEBUG) {
			unit.setAiOrder("Back off");
		}
	}

	private static void actionKeepTheLine(Unit unit) {
		if (StrengthComparison.getEnemySupply() < 40) {
			return;
		}

		MapPoint rendezvousTankForGroundUnits = ArmyRendezvousManager
				.getRendezvousTankForGroundUnits();

		double minDistToTanks = 2.5;
		double maxDistToTanks = 5.5;
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

	// =========================================================

	private static int defineFrontModeForUnit(Unit unit) {
		if (unit.isTank()) {
			return MODE_VANGUARD;
		} else {
			return MODE_FRONT_GUARD;
		}
	}

	// private static boolean isLuckyLibero(Unit unit) {
	// return unit.getID() % 7 == 0;
	// }

	// =========================================================

	private static boolean trySpreadingOutIfNeeded(Unit unit, MapPoint offensivePoint) {
		if (offensivePoint == null
				|| (offensivePoint.distanceTo(unit) < 4.9 && xvr.countUnitsOursInRadius(
						offensivePoint, 3) >= 9)) {
			UnitActions.spreadOutRandomly(unit);
			return true;
		}

		return false;
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

}

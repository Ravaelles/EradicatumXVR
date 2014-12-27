package ai.managers.units.coordination;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;

public class FrontLineManager {

	public static final int MODE_FRONT_GUARD = 8;
	public static final int MODE_VANGUARD = 15;

	private static final int VANGUARD_SEPARATION_DISTANCE = 4;

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
		if (offensivePoint == null || offensivePoint.distanceTo(unit) < 5) {
			UnitActions.spreadOutRandomly(unit);
		}

		// Target is valid, but it's still far. Proceed forward.
		unit.setAiOrder("Forward!");
		UnitActions.attackTo(unit, offensivePoint);

		if (unit.isTank()) {
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
		MapPoint defensivePoint = ArmyRendezvousManager.getDefensivePointForTanks();
		double distanceToDefensivePoint = defensivePoint.distanceTo(unit);
		return distanceToDefensivePoint > allowedMaxDistance && distanceToDefensivePoint > 10
				&& isFarFromSafePoint(unit);
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

		actionKeepTheLine(unit);
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
		MapPoint rendezvousTankForGroundUnits = ArmyRendezvousManager
				.getRendezvousTankForGroundUnits();

		if (rendezvousTankForGroundUnits != null) {
			UnitActions.attackTo(unit, rendezvousTankForGroundUnits);
		} else {
			UnitActions.moveToSafePlace(unit);
		}
	}

}

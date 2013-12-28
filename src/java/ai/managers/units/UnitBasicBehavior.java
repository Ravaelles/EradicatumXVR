package ai.managers.units;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.managers.BuildingManager;
import ai.managers.StrategyManager;
import ai.terran.TerranMedic;
import ai.terran.TerranSiegeTank;
import ai.terran.TerranVulture;
import ai.terran.TerranWraith;

public class UnitBasicBehavior {

	private static XVR xvr = XVR.getInstance();

	protected static void act(Unit unit) {
		UnitType unitType = unit.getType();
		if (unitType == null) {
			return;
		}

		// ======================================
		// OVERRIDE COMMANDS FOR SPECIFIC UNITS

		// If unit is Building
		if (unitType.isBuilding()) {
			BuildingManager.act(unit);
		}

		// Vulture
		else if (unitType.isVulture()) {
			TerranVulture.act(unit);
			return;
		}

		// Medic
		else if (unitType.isMedic()) {
			TerranMedic.act(unit);
			return;
		}

		// ======================================
		// STANDARD ARMY UNIT COMMANDS
		else {

			// If unit has personalized order
			if (unit.getCallForHelpMission() != null) {
				UnitManager.actWhenOnCallForHelpMission(unit);
			}

			// Standard action for unit
			else {

				// If we're ready to total attack
				if (StrategyManager.isAttackPending()) {
					UnitManager.actWhenMassiveAttackIsPending(unit);
				}

				// Standard situation
				else {
					UnitManager.actWhenNoMassiveAttack(unit);
				}
			}
		}

		// ======================================
		// SPECIFIC ACTIONS for units, but DON'T FULLY OVERRIDE standard
		// behavior

		// Tank
		if (unitType.isTank()) {
			TerranSiegeTank.act(unit);
		}

		// Wraith
		else if (unitType.isWraith()) {
			TerranWraith.act(unit);
		}
	}

	public static void runFromCloseOpponents(Unit unit) {

		// Don't interrupt when just starting an attack
		if (unit.isStartingAttack() || unit.isLoaded() || unit.getType().isFirebat()) {
			return;
		}

		// =============================================
		// Define nearest enemy (threat)
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);

		// If there's dangerous enemy nearby and he's close, try to move away.
		if (nearestEnemy != null && !nearestEnemy.isWorker() && nearestEnemy.distanceTo(unit) < 3.5) {
			boolean movedAss = false;

			// Sieged tanks have to unsiege first
			if (unit.getType().isTank() && unit.isSieged()) {
				unit.unsiege();
				return;
			}

			// Try to move away from unit and if can't (e.g. a wall behind), try
			// to increase tiles away from current location
			for (int i = 3; i <= 7; i++) {
				if (UnitActions.moveAwayFromUnitIfPossible(unit, nearestEnemy, i)) {
					// System.out.println("+++ " + unit.getName() +
					// " Running from unit with i=" + i);
					movedAss = true;
					break;
				}
			}

			// If still didn't move, go back to the safe place
			if (!movedAss) {
				// System.out.println("--- " + unit.getName() +
				// " Running to safe place");
				UnitActions.moveToSafePlace(unit);
			}
		}
	}

}

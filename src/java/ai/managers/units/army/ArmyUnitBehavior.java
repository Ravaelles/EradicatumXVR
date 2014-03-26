package ai.managers.units.army;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.army.ArmyPlacing;
import ai.handling.units.CallForHelp;
import ai.handling.units.UnitActions;
import ai.managers.units.UnitManager;

public class ArmyUnitBehavior {

	private static XVR xvr = XVR.getInstance();

	// =============================================

	public static void actStandardUnit(Unit unit) {
		// if (UnitManager.isHasValidTargetToAttack(unit)) {
		// return;
		// }

		// =====================================
		// Possible override of orders if some unit needs help

		// If some unit called for help
		boolean isOnCallForHelpMission = false;
		if (CallForHelp.isAnyCallForHelp()) {
			boolean accepted = UnitManager.decideWhetherToHelpSomeoneCalledForHelp(unit);
			if (!isOnCallForHelpMission && accepted) {
				isOnCallForHelpMission = true;
			}
		}

		// If unit has personalized order
		if (unit.getCallForHelpMission() != null) {
			UnitManager.actWhenOnCallForHelpMission(unit);
		}

		// Call for help isn't active right now
		if (!isOnCallForHelpMission) {
			ArmyPlacing.goToSafePlaceIfNotAlreadyThere(unit);
		}
	}

	// =============================================

	private static void actNormally(Unit unit) {

		// // If we're ready to total attack
		// if (StrategyManager.isAttackPending()) {
		UnitManager.actWhenMassiveAttackIsPending(unit);
		// }
		//
		// // Standard situation
		// else {
		// actWhenNoMassiveAttack(unit);
		// }
	}

	public static void actWhenNoMassiveAttack(Unit unit) {
		if (UnitManager.shouldUnitBeExplorer(unit) && !unit.isRunningFromEnemy()) {
			UnitActions.spreadOutRandomly(unit);
		} else {
			actNormally(unit);
		}
	}

}

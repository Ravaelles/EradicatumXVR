package ai.managers.units;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.tanks.SiegeTankManager;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.managers.units.coordination.AttackCloseTargets;
import ai.managers.units.coordination.FrontLineManager;
import ai.terran.TerranMedic;
import ai.terran.TerranVulture;

public class UnitOrdinaryActions {

	protected static void tryOrdinaryActions(Unit unit) {
		boolean isFreeFromGenericCommands = isUnitFreeFromGenericCommands(unit);

		// =========================================================
		// Act according to STRATEGY, attack strategic targets,
		// define proper place for a unit.

		UnitType unitType = unit.getType();

		// If unit isn't allowed to act on their own like e.g. Vultures
		// sometimes can, coordinate their actions.
		if (!isFreeFromGenericCommands) {

			// GLOBAL ATTACK is active
			if (StrategyManager.isGlobalAttackActive()) {
				FrontLineManager.actOffensively(unit);
			}

			// DEFENSIVE STANCE is active
			else {
				ArmyRendezvousManager.act(unit);
			}
		}

		// =========================================================
		// SPECIFIC ACTIONS for units, but DON'T FULLY OVERRIDE standard
		// behavior

		// Tank
		if (unitType.isTank()) {
			SiegeTankManager.act(unit);
		}

		// Vulture
		else if (unitType.isVulture()) {
			if (TerranVulture.act(unit)) {
				return;
			}
		}

		// =========================================================
		// OVERRIDE COMMANDS FOR SPECIFIC UNITS

		// Medic
		else if (unitType.isMedic()) {
			TerranMedic.act(unit);
			return;
		}

		// ===============================
		// ATTACK CLOSE targets (Tactics phase)
		if (AttackCloseTargets.tryAttackingCloseTargets(unit)) {
			unit.setAiOrder("Attack close");
		}
	}

	// =========================================================

	private static boolean isUnitFreeFromGenericCommands(Unit unit) {
		return unit.isVulture() && TerranVulture.canActIndividually(unit);
	}

}

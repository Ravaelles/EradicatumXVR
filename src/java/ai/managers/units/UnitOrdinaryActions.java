package ai.managers.units;

import jnibwapi.model.Unit;
import ai.managers.units.coordination.ArmyUnitBasicBehavior;
import ai.managers.units.coordination.AttackCloseTargets;
import ai.terran.TerranSiegeTank;

public class UnitOrdinaryActions {

	protected static void tryOrdinaryActions(Unit unit) {

		// ===============================
		// Act according to STRATEGY, attack strategic targets,
		// define proper place for a unit.
		ArmyUnitBasicBehavior.act(unit);

		// ===============================
		// ATTACK CLOSE targets (Tactics phase)
		if (TerranSiegeTank.getNumberOfUnitsCompleted() >= 2
				&& AttackCloseTargets.tryAttackingCloseTargets(unit)) {
			unit.setAiOrder("Attack close");
		}
	}

}

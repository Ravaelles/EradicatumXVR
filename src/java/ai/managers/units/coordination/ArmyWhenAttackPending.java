package ai.managers.units.coordination;

import jnibwapi.model.Unit;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitManager;

public class ArmyWhenAttackPending {

	public static void actWhenMassiveAttackIsPending(Unit unit) {

		// If unit is surrounded by other units (doesn't attack alone)
		// if (isPartOfClusterOfMinXUnits(unit)) {

		// If there is attack target defined, go for it.
		if (StrategyManager.isSomethingToAttackDefined()) {
			// if (UnitManager.unitIsTooFarFromSafePlaceWhenAttackPending(unit))
			// {
			// return;
			// }

			if (UnitManager.isHasValidTargetToAttack(unit)) {
				return;
			}

			if (StrategyManager.getTargetPoint() != null) {
				if (!UnitManager.isHasValidTargetToAttack(unit)) {
					UnitActions.attackTo(unit, StrategyManager.getTargetPoint());
					unit.setAiOrder("Forward!");
				}
				if (UnitManager.isUnitFullyIdle(unit)) {
					UnitActions.spreadOutRandomly(unit);
				}
			} else {
				UnitActions.spreadOutRandomly(unit);
			}
		}

		// If no attack target is defined it probably means that the fog
		// of war is hiding from us other enemy buildings
		else {
			UnitActions.spreadOutRandomly(unit);
		}
	}

}

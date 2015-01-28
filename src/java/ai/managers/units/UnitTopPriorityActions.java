package ai.managers.units;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.strength.StrengthComparison;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.tanks.EnemyTanksManager;
import ai.managers.units.coordination.ArmyUnitBasicBehavior;

public class UnitTopPriorityActions {

	public static int _lastTimeSpreadOut = -1;

	// =========================================================

	protected static boolean tryTopPriorityActions(Unit unit) {

		// Spread out if enemy is beaten and we're very strong
		if (shouldSpreadOut(unit)) {
			if (unit.isSieged() && unit.getGroundWeaponCooldown() <= 1) {
				unit.unsiege();
			}

			UnitActions.spreadOutRandomly(unit);
			unit.setAiOrder("Spread");
			return true;
		}

		// Avoid enemy tanks in Siege Mode
		if (EnemyTanksManager.tryAvoidingEnemyTanks(unit)) {
			unit.setAiOrder("Enemy tank");
			return true;
		}

		// Disallow units to move close to the defensive buildings
		if (!UnitManager._forceSpreadOut
				&& ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(unit)) {
			return true;
		}

		return false;
	}

	// =========================================================

	private static boolean shouldSpreadOut(Unit unit) {
		UnitManager._forceSpreadOut = false;

		if (unit.isAttacking() || unit.isUnderAttack()) {
			return false;
		}

		if (StrategyManager.getTargetUnit() != null) {
			return false;
		}

		if (xvr.getSuppliesTotal() > 150 && xvr.getSuppliesFree() < 10
				&& StrengthComparison.getEnemySupply() < 40
				&& xvr.countUnitsOursInRadius(unit, 6) >= 15) {

			MapPoint targetPoint = StrategyManager.getTargetPoint();
			if (targetPoint != null && targetPoint.distanceTo(unit) > 5.5
					&& xvr.getTimeSeconds() <= _lastTimeSpreadOut + 10) {
				_lastTimeSpreadOut = xvr.getTimeSeconds();
				UnitManager._forceSpreadOut = true;
				return true;
			} else {
				UnitManager._forceSpreadOut = true;
				return true;
			}
		}

		return false;
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

}

package ai.managers.units;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.tanks.EnemyTanksManager;
import ai.managers.units.coordination.ArmyUnitBasicBehavior;
import ai.terran.TerranFactory;

public class UnitTopPriorityActions {

	public static int _lastTimeSpreadOut = -1;

	// =========================================================

	protected static boolean tryTopPriorityActions(Unit unit) {

		// Disallow units to move close to the defensive buildings
		if (!UnitManager._forceSpreadOut
				&& ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(unit)) {
			return true;
		}

		// Spread out if enemy is beaten and we're very strong
		if (shouldSpreadOut(unit)) {
			if (unit.isSieged() && unit.getGroundWeaponCooldown() <= 1) {
				unit.unsiege();
			}

			spreadOut(unit);

			unit.setAiOrder("Make place");
			return true;
		}

		// Avoid enemy tanks in Siege Mode
		if (EnemyTanksManager.tryAvoidingEnemyTanks(unit)) {
			unit.setAiOrder("Enemy tank");
			return true;
		}

		return false;
	}

	private static void spreadOut(Unit unit) {
		Unit nearestTank = xvr.getNearestTankTo(unit);

		// Tank
		if (unit.isTank() || nearestTank == null) {
			if (unit.getGroundWeaponCooldown() <= 0) {
				UnitActions.spreadOutRandomly(unit);
			}
		}

		// Not-tank
		else {
			if (StrategyManager.getTargetUnit() != null) {
				UnitActions.attackTo(unit, StrategyManager.getTargetUnit());
			} else {
				UnitActions.moveAwayFrom(unit, nearestTank);
			}
		}
	}

	// =========================================================

	private static boolean shouldSpreadOut(Unit unit) {
		UnitManager._forceSpreadOut = false;

		// =========================================================
		// Vs. XIMP

		if (TerranFactory.ONLY_TANKS && xvr.getTimeSeconds() < 490) {
			return false;
		}

		// =========================================================
		// If unit is engaged in combat, don't spread out
		if (unit.getGroundWeaponCooldown() > 0 || unit.isUnderAttack()) {
			return false;
		}

		// =========================================================
		// Handle tanks

		if (unit.isTank()) {
			if (!StrategyManager.isSomethingToAttackDefined() && xvr.getSuppliesFree() <= 2) {
				return true;
			} else {
				return false;
			}
		}

		// =========================================================
		// Anti-stuck code

		// If there's a tank in radius of 2 tiles, spread
		// if (!unit.isTank() && xvr.countTanksOurInRadius(unit, 2) > 0) {
		// return true;
		// }

		if (!unit.isTank() && xvr.countTanksOurInRadius(unit, 1.8) >= 3
				&& xvr.countTanksOurInRadius(unit, 3) >= 7) {
			return true;
		}

		// =========================================================

		if (xvr.getSuppliesTotal() > 150 && xvr.getSuppliesFree() < 10
		// && StrengthComparison.getEnemySupply() < 40
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

		// Unit targetUnit = StrategyManager.getTargetUnit();
		// if (targetUnit != null) {
		// return false;
		// }
		//
		// if (StrategyManager.isSomethingToAttackDefined()) {
		// return false;
		// }

		return false;
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

}

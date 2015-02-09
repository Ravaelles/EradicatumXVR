package ai.managers.units;

import java.util.Collection;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.units.MoveAway;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitAiOrders;
import ai.managers.enemy.HiddenEnemyUnitsManager;
import ai.managers.units.army.BunkerManager;
import ai.managers.units.army.RunManager;
import ai.managers.units.coordination.ArmyUnitBasicBehavior;
import ai.managers.units.workers.RepairAndSons;

public class UnitImportantActions {

	private static final double ENEMY_RANGE_SAFE_DIST_BONUS = 2.5;

	// =========================================================

	protected static boolean tryImportantActions(Unit unit) {

		// Try to load infantry inside bunkers if possible.
		if (BunkerManager.tryLoadingIntoBunkersIfPossible(unit)) {
			unit.setAiOrder("Into bunker");
			return true;
		}

		// If enemy has got very close near to us, move away
		if (RunManager.runFromCloseOpponentsIfNecessary(unit)) {
			unit.setAiOrder("Run");
			return true;
		}

		// Wounded units should avoid being killed (if possible you know...)
		if (ArmyUnitBasicBehavior.tryRunningIfSeriouslyWounded(unit)) {
			unit.setAiOrder("Badly wounded");
			return true;
		}

		// Make sure unit will get repaired
		if (RepairAndSons.tryGoingToRepairIfNeeded(unit)) {
			if (unit.isSieged()) {
				unit.unsiege();
			}
			unit.setAiOrder(UnitAiOrders.ORDER_TO_REPAIR);
			unit.setAiOrder("To repair");
			return true;
		}

		// Use Stimpacks if need.
		if (ArmyUnitBasicBehavior.tryUsingStimpacksIfNeeded(unit)) {
			unit.setAiOrder("Stimpack!");
			return false;
		}

		// Run from hidden Lurkers, Dark Templars etc.
		if (HiddenEnemyUnitsManager.tryAvoidingHiddenUnitsIfNecessary(unit)) {
			return true;
		}

		// Don't interrupt units being repaired
		if (RepairAndSons.isUnitBeingRepaired(unit)) {
			UnitActions.holdPosition(unit);
			return true;
		}

		// Disallow fighting when overwhelmed.
		// if (ArmyUnitBasicBehavior.tryRetreatingIfChancesNotFavorable(unit)) {
		// unit.setAiOrder("Would lose");
		// return;
		// }

		// ===============================
		// INIDIVIDUAL MISSIONS, SPECIAL FORCES
		// if (SpecialForcesManager.tryActingSpecialForceIfNeeded(unit)) {
		// unit.setAiOrder("Special force");
		// return true;
		// }

		// =========================================================

		return false;
	}

	// =========================================================

	public static boolean tryAvoidingEnemyUnitsThatCanShoot(Unit unit) {
		boolean isLandUnit = !unit.getType().isFlyer();

		Collection<Unit> enemyUnitsNear = xvr.getEnemyUnitsInRadius(13, unit);
		for (Unit enemy : enemyUnitsNear) {
			double enemyRange = -1;
			double distToEnemy = unit.distanceTo(enemy);

			// We're ground unit
			if (isLandUnit && enemy.canAttackGroundUnits()
					&& (enemyRange = enemy.getType().getGroundWeapon().getMaxRangeInTiles()) > 1) {
				if (distToEnemy <= enemyRange + ENEMY_RANGE_SAFE_DIST_BONUS) {
					avoidEnemy(unit, enemy);
					return true;
				}
			}

			// We're air unit
			else if (!isLandUnit && enemy.canAttackAirUnits()
					&& (enemyRange = enemy.getType().getAirWeapon().getMaxRangeInTiles()) > 1) {
				if (distToEnemy <= enemyRange + ENEMY_RANGE_SAFE_DIST_BONUS) {
					avoidEnemy(unit, enemy);
					return true;
				}
			}
		}

		return false;
	}

	// =========================================================

	private static void avoidEnemy(Unit unit, Unit enemy) {
		MoveAway.moveAwayFromEnemyOrEnemies(unit);
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

}

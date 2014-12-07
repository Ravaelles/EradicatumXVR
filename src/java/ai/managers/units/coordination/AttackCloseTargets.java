package ai.managers.units.coordination;

import java.util.ArrayList;
import java.util.Collection;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.strength.StrengthRatio;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitManager;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;

public class AttackCloseTargets {

	private static XVR xvr = XVR.getInstance();

	private static final int MAX_DIST_TO_BASE_WHEN_AT_PEACE = 27;
	private static final int MIN_TANKS_TO_FORWARD = 5;
	private static final int MAX_DIST_FROM_BASE_TOBE_ENEMY_WORKER_AROUND_OUR_BASE = 21;
	private static final double FORCE_MOVE_TO_ENEMY_TANK_IF_DISTANCE_GREATER_THAN = 0.8;

	// =========================================================

	public static boolean tryAttackingCloseTargets(Unit unit) {

		// Only relatively healthy units can attack
		if (xvr.getTimeSeconds() < 350 && unit.getType().isTerranInfantry() && !unit.isHPAtLeastNPercent(70)) {
			return false;
		}

		// For tanks, allow to attack only if there're other tanks nearby
		if (unit.getType().isTank()) {
			int TANK_DIST = 8;
			int tanksNear = xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode, TANK_DIST, unit,
					true)
					+ xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode, TANK_DIST, unit, true);
			if (tanksNear < MIN_TANKS_TO_FORWARD) {
				return false;
			}
		}

		// If unit is far from any base and there's no attack pending, don't
		// attack
		if (StrategyManager.isAnyAttackFormPending()) {
			Unit nearestBase = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, unit);
			if (nearestBase != null && nearestBase.distanceTo(unit) > MAX_DIST_TO_BASE_WHEN_AT_PEACE) {
				return false;
			}
		}

		// =========================================================

		UnitType type = unit.getType();

		// !type.isTank() &&
		// boolean canTryAttackingCloseTargets = !type.isWorker() &&
		// !type.isMedic();
		if (unit.isRunningFromEnemy()) {
			return false;
		}

		// Some units can never attack close targets.
		if (type.isMedic() || unit.isBeingHealed() || type.isTankSieged()) {
			return false;
		}

		// =================================
		// If unit is infantry it should try to go inside bunkers, instead of
		// attacking any targets.
		// if (unit.getType().isTerranInfantry()
		// && UnitBasicBehavior.tryLoadingIntoBunkersIfPossible(unit)) {
		// return;
		// }

		// ============================================

		boolean groundAttackCapable = unit.canAttackGroundUnits();
		boolean airAttackCapable = unit.canAttackAirUnits();
		// Unit importantEnemyUnitNearby = null;
		Unit enemyToAttack = null;

		// ============================================

		// Enemy worker is a great target, it will slow down the economy
		if (tryFindingEnemyWorker(unit)) {
			return false;
		}

		// Try selecting top priority units like lurkers, siege tanks.
		// importantEnemyUnitNearby =
		// TargetHandling.getImportantEnemyUnitTargetIfPossibleFor(unit,
		// groundAttackCapable, airAttackCapable);

		ArrayList<Unit> enemyUnits = xvr.getEnemyUnitsVisible(groundAttackCapable, airAttackCapable);

		// if (importantEnemyUnitNearby != null &&
		// importantEnemyUnitNearby.isDetected()) {
		// if (!importantEnemyUnitNearby.getType().isSpiderMine()
		// || (unit.getType().getGroundWeapon().getMaxRangeInTiles()) >= 2)
		// enemyToAttack = importantEnemyUnitNearby;
		// }

		// If no such unit is nearby then attack the closest one.
		// else {
		enemyToAttack = xvr.getUnitNearestFromList(unit, enemyUnits);
		// }

		if (enemyToAttack != null && !enemyToAttack.isDetected()) {
			enemyToAttack = null;
		}

		if (enemyToAttack != null) {
			// if (enemyToAttack != null
			// && (enemyToAttack.getType().isWorker() && xvr.getTimeSeconds() <
			// 600 && xvr
			// .getDistanceBetween(enemyToAttack, xvr.getFirstBase()) < 30)) {
			enemyToAttack = null;

			// for (Iterator<Unit> iterator = enemyUnits.iterator();
			// iterator.hasNext();) {
			// Unit enemyUnit = (Unit) iterator.next();
			// if (enemyUnit.getType().isWorker()
			// && xvr.getDistanceBetween(enemyToAttack, xvr.getFirstBase())
			// < 25) {
			// iterator.remove();
			// }
			// }

			enemyToAttack = xvr.getUnitNearestFromList(unit, enemyUnits);
		}

		enemyToAttack = makeSureItsNotEnemyWorkerAroundTheBase(enemyToAttack);

		// Attack selected target if it's not too far away.
		if (enemyToAttack != null && enemyToAttack.isDetected()) {
			// if (isUnitInPositionToAlwaysAttack(unit)) {
			int maxDistance = unit.getType().isFlyer() ? 300 : 10;
			if (unit.distanceTo(enemyToAttack) > maxDistance) {
				UnitActions.attackEnemyUnit(unit, enemyToAttack);
			}
			// return true;
			// }

			Unit nearestEnemy = xvr.getUnitNearestFromList(unit,
					xvr.getEnemyUnitsVisible(groundAttackCapable, airAttackCapable));

			boolean isStrengthRatioFavorable = StrengthRatio.isStrengthRatioFavorableFor(unit);

			// If there's an enemy near to this unit, don't change the target.
			if (nearestEnemy != null && xvr.getDistanceBetween(unit, nearestEnemy) <= 1) {
			}

			// There's no valid target, attack this enemy.
			else {
				if (!StrengthRatio.isStrengthRatioFavorableFor(unit)
						&& unit.distanceTo(xvr.getFirstBase()) > maxDistance) {
					return false;
				}
			}

			enemyToAttack = makeSureItsNotEnemyWorkerAroundTheBase(enemyToAttack);

			if (nearestEnemy != null && isStrengthRatioFavorable && nearestEnemy.isDetected()) {
				attackCloseTarget(unit, nearestEnemy);
				return true;
			}
		}
		return false;
	}

	private static void attackCloseTarget(Unit unit, Unit nearestEnemy) {
		if (unit == null || nearestEnemy == null) {
			return;
		}

		// Normal units
		if (!nearestEnemy.getType().isTank()) {
			UnitActions.attackEnemyUnit(unit, nearestEnemy);
		}

		// Attacking tanks is handled differently: first, go to the unit (force
		// it to unsiege)
		else {

			// Tank is far, move as close to it as possible
			if (unit.distanceTo(nearestEnemy) > FORCE_MOVE_TO_ENEMY_TANK_IF_DISTANCE_GREATER_THAN) {
				UnitActions.moveTo(unit, nearestEnemy);
			}

			// We're already just in front of the tank, attack it
			else {
				UnitActions.attackEnemyUnit(unit, nearestEnemy);
			}
		}
	}

	// =========================================================

	private static Unit makeSureItsNotEnemyWorkerAroundTheBase(Unit enemy) {
		if (enemy == null) {
			return null;
		}

		if (enemy.getType().isWorker()
				&& enemy.distanceTo(xvr.getFirstBase()) < MAX_DIST_FROM_BASE_TOBE_ENEMY_WORKER_AROUND_OUR_BASE) {
			return null;
		}

		return enemy;
	}

	private static boolean tryFindingEnemyWorker(Unit unit) {
		Unit enemyWorker = null;

		// ============================
		// Workers Repairing are crucial to attack
		Collection<Unit> enemyWorkers = xvr.getEnemyWorkersInRadius(7, unit);
		if (enemyWorkers != null) {
			for (Unit worker : enemyWorkers) {
				if (worker.isRepairing()) {
					enemyWorker = worker;
					break;
				}
			}
		}

		// ==============================
		// Normal workers can be attacked with priority, but only early in the
		// game follow them
		// int maxDistToWorker = xvr.getTimeSeconds() < 1000 ? 2 : 1;
		// Unit someEnemyWorker = xvr.getEnemyWorkerInRadius(maxDistToWorker,
		// unit);
		// if (enemyWorker != null) {
		// if (xvr.getDistanceBetween(xvr.getFirstBase(), enemyWorker) > 30
		// || xvr.getTimeSeconds() > 600) {
		// enemyWorker = someEnemyWorker;
		// }
		// }

		if (enemyWorker == null) {
			return false;
		} else {
			UnitActions.attackEnemyUnit(unit, enemyWorker);
			return true;
		}
	}

	protected static boolean isUnitInPositionToAlwaysAttack(Unit unit) {
		boolean ourPhotonCannonIsNear = xvr.getUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(), 4, unit, true)
				.size() > 0;
		boolean baseInDanger = (xvr.getDistanceBetween(
				xvr.getUnitNearestFromList(unit, TerranCommandCenter.getBases()), unit) <= 7);

		return ourPhotonCannonIsNear || baseInDanger;
	}

}

package ai.managers.units;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.army.StrengthRatio;
import ai.handling.army.TargetHandling;
import ai.handling.units.UnitActions;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;

public class AttackCloseTargets {

	private static XVR xvr = XVR.getInstance();

	public static boolean tryAttackingCloseTargets(Unit unit) {
		UnitType type = unit.getType();

		// !type.isTank() &&
		boolean canTryAttackingCloseTargets = !type.isWorker() && !type.isMedic();
		if (!canTryAttackingCloseTargets || unit.isRunningFromEnemy()) {
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
		Unit importantEnemyUnitNearby = null;
		Unit enemyToAttack = null;

		// ============================================

		// Enemy worker is a great target, it will slow down the economy
		if (tryFindingEnemyWorker(unit)) {
			return false;
		}

		// Try selecting top priority units like lurkers, siege tanks.
		importantEnemyUnitNearby = TargetHandling.getImportantEnemyUnitTargetIfPossibleFor(unit,
				groundAttackCapable, airAttackCapable);

		ArrayList<Unit> enemyUnits = xvr
				.getEnemyUnitsVisible(groundAttackCapable, airAttackCapable);

		if (importantEnemyUnitNearby != null && importantEnemyUnitNearby.isDetected()) {
			if (!importantEnemyUnitNearby.getType().isSpiderMine()
					|| (unit.getType().getGroundWeapon().getMaxRangeInTiles()) >= 2)
				enemyToAttack = importantEnemyUnitNearby;
		}

		// If no such unit is nearby then attack the closest one.
		else {
			enemyToAttack = xvr.getUnitNearestFromList(unit, enemyUnits);
		}

		if (enemyToAttack != null && !enemyToAttack.isDetected()) {
			enemyToAttack = null;
		}

		if (enemyToAttack != null
				&& (enemyToAttack.getType().isWorker() && xvr.getTimeSeconds() < 600 && xvr
						.getDistanceBetween(enemyToAttack, xvr.getFirstBase()) < 30)) {
			enemyToAttack = null;

			for (Iterator<Unit> iterator = enemyUnits.iterator(); iterator.hasNext();) {
				Unit enemyUnit = (Unit) iterator.next();
				if (enemyUnit.getType().isWorker()
						&& xvr.getDistanceBetween(enemyToAttack, xvr.getFirstBase()) < 25) {
					iterator.remove();
				}
			}

			enemyToAttack = xvr.getUnitNearestFromList(unit, enemyUnits);
		}

		// Attack selected target if it's not too far away.
		if (enemyToAttack != null && enemyToAttack.isDetected()) {
			if (isUnitInPositionToAlwaysAttack(unit)) {
				UnitActions.attackEnemyUnit(unit, enemyToAttack);
				return true;
			}

			Unit nearestEnemy = xvr.getUnitNearestFromList(unit,
					xvr.getEnemyUnitsVisible(groundAttackCapable, airAttackCapable));

			boolean isStrengthRatioFavorable = StrengthRatio.isStrengthRatioFavorableFor(unit);

			// If there's an enemy near to this unit, don't change the target.
			if (nearestEnemy != null && xvr.getDistanceBetween(unit, nearestEnemy) <= 1) {
			}

			// There's no valid target, attack this enemy.
			else {
				int maxDistance = unit.getType().isFlyer() ? 300 : 10;

				if (!StrengthRatio.isStrengthRatioFavorableFor(unit)
						&& unit.distanceTo(xvr.getFirstBase()) > maxDistance) {
					return false;
				}
			}

			if (nearestEnemy != null && isStrengthRatioFavorable) {
				UnitActions.attackEnemyUnit(unit, nearestEnemy);
				return true;
			}
		}
		return false;
	}

	private static boolean tryFindingEnemyWorker(Unit unit) {
		Unit enemyWorker = null;

		// ============================
		// Workers Repairing are crucial to attack
		Collection<Unit> enemyWorkers = xvr.getEnemyWorkersInRadius(5, unit);
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
		int maxDistToWorker = xvr.getTimeSeconds() < 1000 ? 2 : 1;
		Unit someEnemyWorker = xvr.getEnemyWorkerInRadius(maxDistToWorker, unit);
		if (enemyWorker != null) {
			if (xvr.getDistanceBetween(xvr.getFirstBase(), enemyWorker) > 30
					|| xvr.getTimeSeconds() > 600) {
				enemyWorker = someEnemyWorker;
			}
		}

		if (enemyWorker == null) {
			return false;
		} else {
			UnitActions.attackEnemyUnit(unit, enemyWorker);
			return true;
		}
	}

	protected static boolean isUnitInPositionToAlwaysAttack(Unit unit) {
		boolean ourPhotonCannonIsNear = xvr.getUnitsOfGivenTypeInRadius(
				TerranBunker.getBuildingType(), 4, unit, true).size() > 0;
		boolean baseInDanger = (xvr.getDistanceBetween(
				xvr.getUnitNearestFromList(unit, TerranCommandCenter.getBases()), unit) <= 7);

		return ourPhotonCannonIsNear || baseInDanger;
	}

}

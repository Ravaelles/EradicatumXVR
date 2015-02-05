package ai.managers.units.coordination;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.terran.TerranBunker;

public class AttackCloseTargets {

	private static int MAX_DIST_TO_CLOSE_ATTACK = 12;

	// =========================================================

	public static boolean tryAttackingCloseTargets(Unit unit) {

		// First of all, protect the base from harm
		if (tryAttackingBaseThreats(unit)) {
			return true;
		}

		// If there's an enemy near and you're allowed to fight, engage in
		// combat
		else {

			// Check if there's a close enemy to attack. If not, exit.
			Unit nearestEnemy = xvr.getNearestEnemy(unit);
			if (nearestEnemy == null || nearestEnemy.distanceTo(unit) > MAX_DIST_TO_CLOSE_ATTACK) {
				return false;
			}

			// There is some enemy nearby
			else {

				// Define if this unit is allowed to attack the enemy
				if (isUnitAllowedToAttackCloseTargets(unit)) {

					// Define best enemy to attack and do it
					if (tryAttackingCloseEnemy(unit)) {
						return true;
					}
				}
			}

		}

		return false;
	}

	// =========================================================

	private static boolean tryAttackingCloseEnemy(Unit unit) {
		Unit enemy = defineCloseEnemyToAttackForUnit(unit);

		if (enemy != null && !unit.isAttacking() && unit.getGroundWeaponCooldown() == 0) {
			UnitActions.attackEnemyUnit(unit, enemy);
			return true;
		} else {
			return false;
		}
	}

	// =========================================================

	private static boolean tryAttackingBaseThreats(Unit unit) {
		Unit baseThreatUnit = getEnemyThatIsThreatToMainBase();

		if (baseThreatUnit != null && !unit.isAttacking()) {
			UnitActions.attackTo(unit, baseThreatUnit);
			return true;
		} else {
			return false;
		}
	}

	// =========================================================

	private static Unit defineCloseEnemyToAttackForUnit(Unit unit) {
		boolean canUnitAttackGround = unit.canAttackGroundUnits();
		boolean canUnitAttackAir = unit.canAttackAirUnits();

		// Define list of visible enemies that are possible to be attacked by
		// this unit
		// E.g. a firebat can't attack flyers.
		ArrayList<Unit> enemyUnitsVisible = xvr.getEnemyUnitsVisible(canUnitAttackGround,
				canUnitAttackAir);
		Unit nearestEnemy = xvr.getUnitNearestFromList(unit, enemyUnitsVisible);

		// If we found the enemy that is close enough, attack him
		if (nearestEnemy != null && nearestEnemy.distanceTo(unit) <= MAX_DIST_TO_CLOSE_ATTACK) {
			return nearestEnemy;
		} else {
			return null;
		}
	}

	// =========================================================

	private static boolean isUnitAllowedToAttackCloseTargets(Unit unit) {
		if (unit.getHPPercent() < 40 || unit.getHP() < 30) {
			return false;
		}

		if (unit.getType().isFlyer()) {
			return true;
		}

		if (StrategyManager.isGlobalAttackInProgress()) {
			return isUnitAllowedToAttackInPeace(unit);
		} else {
			return isUnitAllowedToAttackInGlobalAttack(unit);
		}
	}

	private static boolean isUnitAllowedToAttackInGlobalAttack(Unit unit) {
		if (unit.getType().isFlyer()) {
			return true;
		}

		// Unit has to have tank protection
		Unit nearestTank = xvr.getNearestTankTo(unit);
		return nearestTank != null && nearestTank.distanceTo(unit) <= 6;
	}

	private static boolean isUnitAllowedToAttackInPeace(Unit unit) {

		// Unit has to have tank protection
		Unit nearestTank = xvr.getNearestTankTo(unit);
		if (nearestTank != null && nearestTank.distanceTo(unit) <= 7) {
			return true;
		}

		// ...or be close to a bunker
		Unit nearestBunker = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), unit);
		if (nearestBunker != null && nearestBunker.distanceTo(unit) <= 7) {
			return true;
		}

		return false;
	}

	// =========================================================

	private static Unit getEnemyThatIsThreatToMainBase() {
		Unit firstBase = xvr.getFirstBase();
		Unit nearestEnemyToMainBase = null;
		if (firstBase != null) {
			nearestEnemyToMainBase = xvr.getNearestEnemy(firstBase);
		}

		// =========================================================
		// Units that are far from the base aren't actually a threat
		if (nearestEnemyToMainBase != null && nearestEnemyToMainBase.distanceTo(firstBase) > 17) {
			nearestEnemyToMainBase = null;
		}

		return nearestEnemyToMainBase;
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	// public static boolean tryAttackingCloseTargets(Unit unit) {
	//
	// // Only relatively healthy units can attack
	// if (xvr.getTimeSeconds() < 350 && unit.getType().isTerranInfantry()
	// && !unit.isHPAtLeastNPercent(70)) {
	// return false;
	// }
	//
	// // For tanks, allow to attack only if there're other tanks nearby
	// if (unit.getType().isTank()) {
	// int TANK_DIST = 8;
	// int tanksNear = xvr.countUnitsOfGivenTypeInRadius(
	// UnitTypes.Terran_Siege_Tank_Siege_Mode, TANK_DIST, unit, true)
	// +
	// xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode,
	// TANK_DIST, unit, true);
	// if (tanksNear < MIN_TANKS_TO_FORWARD) {
	// return false;
	// }
	// }
	//
	// // If unit is far from any base and there's no attack pending, don't
	// // attack
	// if (StrategyManager.isGlobalAttackInProgress()) {
	// Unit nearestBase = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, unit);
	// if (nearestBase != null
	// && nearestBase.distanceTo(unit) > MAX_DIST_TO_BASE_WHEN_AT_PEACE) {
	// return false;
	// }
	// }
	//
	// // =========================================================
	//
	// UnitType type = unit.getType();
	//
	// // !type.isTank() &&
	// // boolean canTryAttackingCloseTargets = !type.isWorker() &&
	// // !type.isMedic();
	// if (unit.isRunningFromEnemy()) {
	// return false;
	// }
	//
	// // Some units can never attack close targets.
	// if (type.isMedic() || unit.isBeingHealed() || type.isTankSieged()) {
	// return false;
	// }
	//
	// // =================================
	// // If unit is infantry it should try to go inside bunkers, instead of
	// // attacking any targets.
	// // if (unit.getType().isTerranInfantry()
	// // && UnitBasicBehavior.tryLoadingIntoBunkersIfPossible(unit)) {
	// // return;
	// // }
	//
	// // ============================================
	//
	// boolean groundAttackCapable = unit.canAttackGroundUnits();
	// boolean airAttackCapable = unit.canAttackAirUnits();
	// Unit importantEnemyUnitNearby = null;
	// Unit enemyToAttack = null;
	//
	// // ============================================
	//
	// // Enemy worker is a great target, it will slow down the economy
	// if (tryFindingEnemyWorker(unit)) {
	// return false;
	// }
	//
	// // Try selecting top priority units like lurkers, siege tanks.
	// importantEnemyUnitNearby =
	// TargetHandling.getImportantEnemyUnitTargetIfPossibleFor(unit,
	// groundAttackCapable, airAttackCapable);
	//
	// ArrayList<Unit> enemyUnits = xvr
	// .getEnemyUnitsVisible(groundAttackCapable, airAttackCapable);
	//
	// if (importantEnemyUnitNearby != null &&
	// importantEnemyUnitNearby.isDetected()) {
	// if (!importantEnemyUnitNearby.getType().isSpiderMine()
	// || (unit.getType().getGroundWeapon().getMaxRangeInTiles()) >= 2)
	// enemyToAttack = importantEnemyUnitNearby;
	// }
	//
	// // If no such unit is nearby then attack the closest one.
	// else {
	// enemyToAttack = xvr.getUnitNearestFromList(unit, enemyUnits);
	// }
	//
	// if (enemyToAttack != null && !enemyToAttack.isDetected()) {
	// enemyToAttack = null;
	// }
	//
	// if (enemyToAttack != null) {
	// // if (enemyToAttack != null
	// // && (enemyToAttack.getType().isWorker() && xvr.getTimeSeconds() <
	// // 600 && xvr
	// // .getDistanceBetween(enemyToAttack, xvr.getFirstBase()) < 30)) {
	// enemyToAttack = null;
	//
	// // for (Iterator<Unit> iterator = enemyUnits.iterator();
	// // iterator.hasNext();) {
	// // Unit enemyUnit = (Unit) iterator.next();
	// // if (enemyUnit.getType().isWorker()
	// // && xvr.getDistanceBetween(enemyToAttack, xvr.getFirstBase())
	// // < 25) {
	// // iterator.remove();
	// // }
	// // }
	//
	// enemyToAttack = xvr.getUnitNearestFromList(unit, enemyUnits);
	// }
	//
	// enemyToAttack = makeSureItsNotEnemyWorkerAroundTheBase(enemyToAttack);
	// if (enemyToAttack == null) {
	// return false;
	// }
	//
	// // =========================================================
	//
	// if (disallowAttack(unit, enemyToAttack)) {
	// return false;
	// }
	//
	// // =========================================================
	//
	// // Attack selected target if it's not too far away.
	// if (enemyToAttack != null && enemyToAttack.isDetected()) {
	// // if (isUnitInPositionToAlwaysAttack(unit)) {
	// int maxDistance = unit.getType().isFlyer() ? 300 : 10;
	// if (unit.distanceTo(enemyToAttack) > maxDistance) {
	// UnitActions.attackEnemyUnit(unit, enemyToAttack);
	// }
	// // return true;
	// // }
	//
	// Unit nearestEnemy = xvr.getUnitNearestFromList(unit,
	// xvr.getEnemyUnitsVisible(groundAttackCapable, airAttackCapable));
	//
	// boolean isStrengthRatioFavorable =
	// StrengthRatio.isStrengthRatioFavorableFor(unit);
	//
	// // If there's an enemy near to this unit, don't change the target.
	// if (nearestEnemy != null && xvr.getDistanceBetween(unit, nearestEnemy) <=
	// 1) {
	// }
	//
	// // There's no valid target, attack this enemy.
	// else {
	// if (!StrengthRatio.isStrengthRatioFavorableFor(unit)
	// && unit.distanceTo(xvr.getFirstBase()) > maxDistance) {
	// return false;
	// }
	// }
	//
	// enemyToAttack = makeSureItsNotEnemyWorkerAroundTheBase(enemyToAttack);
	//
	// if (nearestEnemy != null && isStrengthRatioFavorable &&
	// nearestEnemy.isDetected()
	// && nearestEnemy.distanceTo(unit) < 14) {
	// attackCloseTarget(unit, nearestEnemy);
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// private static boolean disallowAttack(Unit unit, Unit enemyToAttack) {
	// // =========================================================
	// // Ignore flying buildings
	// if (enemyToAttack.isBuilding() && enemyToAttack.isLifted()) {
	// return false;
	// }
	//
	// // =========================================================
	// // Only allow attacking enemies that are near our main base
	//
	// if (firstBase != null) {
	// boolean isVeryCloseToMainBase = firstBase.distanceTo(enemyToAttack) < 15
	// && xvr.getTimeSeconds() < 500;
	//
	// if (isVeryCloseToMainBase) {
	// return true;
	// }
	//
	// // =========================================================
	// // Make units stick to tanks or bunkers
	//
	// if (!unit.isTank()) {
	// Unit nearestTank = xvr.getNearestTankTo(unit);
	// if (nearestTank != null && nearestTank.distanceTo(unit) >= 4.7) {
	// return true;
	// }
	// }
	//
	// // =========================================================
	//
	// boolean engageZealots = enemyToAttack.isZealot() && unit.isVulture()
	// && xvr.getTimeSeconds() <= 430;
	// boolean isTooFarFromBase = firstBase.distanceTo(enemyToAttack) > 24;
	// boolean enoughTanksSupport = xvr.countTanksOurInRadius(unit, 7) > 0
	// || enemyToAttack.isZealot();
	//
	// if (isTooFarFromBase || !enoughTanksSupport) {
	// if (engageZealots) {
	// return true;
	// } else {
	// return false;
	// }
	// }
	// }
	//
	// return false;
	// }
	//
	// private static void attackCloseTarget(Unit unit, Unit nearestEnemy) {
	// if (unit == null || nearestEnemy == null) {
	// return;
	// }
	//
	// // Normal units
	// if (!nearestEnemy.getType().isTank()) {
	// UnitActions.attackEnemyUnit(unit, nearestEnemy);
	// }
	//
	// // Attacking tanks is handled differently: first, go to the unit (force
	// // it to unsiege)
	// else {
	//
	// // Tank is far, move as close to it as possible
	// if (unit.distanceTo(nearestEnemy) >
	// FORCE_MOVE_TO_ENEMY_TANK_IF_DISTANCE_GREATER_THAN) {
	// UnitActions.moveTo(unit, nearestEnemy);
	// }
	//
	// // We're already just in front of the tank, attack it
	// else {
	// UnitActions.attackEnemyUnit(unit, nearestEnemy);
	// }
	// }
	// }
	//
	// // =========================================================
	//
	// private static Unit makeSureItsNotEnemyWorkerAroundTheBase(Unit enemy) {
	// if (enemy == null) {
	// return null;
	// }
	//
	// if (enemy.getType().isWorker() && enemy.distanceTo(xvr.getFirstBase()) <
	// 16) {
	// return null;
	// }
	//
	// return enemy;
	// }
	//
	// private static boolean tryFindingEnemyWorker(Unit unit) {
	// Unit enemyWorker = null;
	//
	// // ============================
	// // Workers Repairing are crucial to attack
	// Collection<Unit> enemyWorkers = xvr.getEnemyWorkersInRadius(7, unit);
	// if (enemyWorkers != null) {
	// for (Unit worker : enemyWorkers) {
	// if (worker.isRepairing()) {
	// enemyWorker = worker;
	// break;
	// }
	// }
	// }
	//
	// // ==============================
	// // Normal workers can be attacked with priority, but only early in the
	// // game follow them
	// // int maxDistToWorker = xvr.getTimeSeconds() < 1000 ? 2 : 1;
	// // Unit someEnemyWorker = xvr.getEnemyWorkerInRadius(maxDistToWorker,
	// // unit);
	// // if (enemyWorker != null) {
	// // if (xvr.getDistanceBetween(xvr.getFirstBase(), enemyWorker) > 30
	// // || xvr.getTimeSeconds() > 600) {
	// // enemyWorker = someEnemyWorker;
	// // }
	// // }
	//
	// if (enemyWorker == null) {
	// return false;
	// } else {
	// UnitActions.attackEnemyUnit(unit, enemyWorker);
	// return true;
	// }
	// }
	//
	// protected static boolean isUnitInPositionToAlwaysAttack(Unit unit) {
	// boolean ourPhotonCannonIsNear = xvr.getUnitsOfGivenTypeInRadius(
	// TerranBunker.getBuildingType(), 4, unit, true).size() > 0;
	// boolean baseInDanger = (xvr.getDistanceBetween(
	// xvr.getUnitNearestFromList(unit, TerranCommandCenter.getBases()), unit)
	// <= 7);
	//
	// return ourPhotonCannonIsNear || baseInDanger;
	// }

}

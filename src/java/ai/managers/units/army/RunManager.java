package ai.managers.units.army;

import java.util.Collection;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitManager;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.managers.units.coordination.AttackCloseTargets;

public class RunManager {

	private static XVR xvr = XVR.getInstance();

	private static final double SAFE_DIST_FROM_ENEMY = 2.7;

	// =========================================================

	public static boolean runFromCloseOpponentsIfNecessary(Unit unit) {

		// If attack is pending, only tanks can run away from enemies.
		if (StrategyManager.isGlobalAttackInProgress()) {
			if (!unit.isTank()) {
				return false;
			}
		}

		return runFromCloseOpponentsIfNecessary(unit, SAFE_DIST_FROM_ENEMY);
	}

	public static boolean runFromCloseOpponentsIfNecessary(Unit unit, double safeDistance) {
		if (shouldFightInsteadOfRunning(unit)) {
			return false;
		}

		UnitType type = unit.getType();

		// take into calculation all nearby enemies
		Collection<Unit> enemyUnitsInRadius = xvr.getEnemyUnitsInRadius(12, unit);
		for (Unit enemy : enemyUnitsInRadius) {

			// If enemy is capable of attacking us (e.g. some air units are
			// unable to do so)
			if (enemy.canAttack(unit) && !enemy.getType().isWorker()) {
				double distToEnemy = unit.distanceTo(enemy);

				// If close unit is strong and there're many of them, include
				// enemy range.
				if (enemy.getHP() >= 50 && enemyUnitsInRadius.size() >= 2) {
					int enemyRange = enemy.getType().getGroundWeapon().getMaxRangeInTiles();
					distToEnemy -= enemyRange;
				}

				boolean extraCondition = false;
				if (type.isVulture() && enemy.getType().isFlyer() && !enemy.getType().isObserver()
						&& enemy.distanceTo(unit) <= 8) {
					extraCondition = true;
				}

				// Define if enemy is real danger or maybe he's too far etc
				boolean enemyCriticallyClose = distToEnemy > 0.1 && distToEnemy < safeDistance;
				boolean enemyCanShootAtUs = !enemyCriticallyClose
						&& !UnitManager.isUnitSafeFromEnemyShootRange(unit, enemy);
				boolean canConsiderRunningFromEnemy = extraCondition || enemyCriticallyClose
						|| enemyCanShootAtUs;

				// If it makes sense to run from the enemy, inform unit about
				// this fact.
				// NOTE: unit may decide to attack the enemy if it's ready to
				// shoot.
				if (canConsiderRunningFromEnemy && !type.isFirebat()) {
					return handleIsRunning(unit, enemy, true);
				}
			}
		}

		// =========================================================

		return handleIsNotRunning(unit);

		// =========================================================
		//
		// double distanceBonusIfWounded = (unit.isWounded() ? 0.95 : 0);
		// double criticallyCloseDistance = 3 + distanceBonusIfWounded +
		// (type.isTank() ? 1.5 : 0)
		// - (type.isMedic() ? 1 : 0);
		// boolean safeFromEnemyShootRange =
		// UnitManager.isUnitSafeFromEnemiesShootRange(unit,
		// xvr.getEnemyUnitsInRadius(11, unit));
		//
	}

	private static boolean shouldFightInsteadOfRunning(Unit unit) {
		boolean isRelativelyHealthy = (unit.getHPPercent() > 65 && unit.getHP() >= 30);
		if (!isRelativelyHealthy && unit.isVulture()) {
			isRelativelyHealthy = unit.getHP() >= 21;
		}

		if (unit.getGroundWeaponCooldown() == 0 && isRelativelyHealthy) {
			return true;
		}

		return false;
	}

	private static boolean handleIsNotRunning(Unit unit) {
		return false;
	}

	private static boolean handleIsRunning(Unit unit, Unit enemy, boolean isRunning) {
		if (isRunning) {
			unit.setIsRunningFromEnemyNow();
			handleRunBehavior(unit, enemy);
		}

		return isRunning;
	}

	private static void handleRunBehavior(Unit unit, Unit enemy) {

		// Define distance to the enemy
		double distToEnemy = unit.distanceTo(enemy);

		// Define where is the safe place
		MapPoint safePlace = ArmyRendezvousManager.goToSafePlaceIfNotAlreadyThere(unit);

		// Define if we should run to the safe place or run from the enemy unit
		boolean safePlaceIsDefinedAndSafe = safePlace != null
				&& xvr.countUnitsEnemyInRadius(safePlace, 12) == 0;
		boolean runToTheSafePlace = safePlaceIsDefinedAndSafe && safePlace.distanceTo(unit) < 7;

		// Counter-attack makes sense
		if (tryCounterAttackingEnemyIfMakesSense(unit, enemy, distToEnemy)) {
		}

		// Counter-attack doesn't make sense, just run
		else {

			// Run to the safe place
			if (runToTheSafePlace) {
				unit.setAiOrder("RUN: to safe place");
				UnitActions.moveTo(unit, safePlace);
			}

			// Run from the enemy unit, in random direction
			else {
				unit.setAiOrder("RUN: from enemy");
				UnitActions.moveAwayFrom(unit, enemy);
			}
		}
	}

	private static boolean tryCounterAttackingEnemyIfMakesSense(Unit unit, Unit enemy,
			double distToEnemy) {
		boolean shouldCounterAttack = false;

		// If enemy is faster than we are, always attack
		if (enemy.getType().getTopSpeed() > unit.getType().getTopSpeed()) {
			shouldCounterAttack = true;
			unit.setAiOrder("COUNTER-attack: enemy is fast!");
		}

		// // If we have good chances of winning, fight
		// if (StrengthRatio.isStrengthRatioFavorableFor(unit)) {
		// shouldCounterAttack = true;
		// unit.setAiOrder("COUNTER-attack: good chances");
		// }

		// If we have ready cooldown, attack
		if ((enemy.getType().isFlyer() && unit.getAirWeaponCooldown() == 0)
				|| (!enemy.getType().isFlyer() && unit.getGroundWeaponCooldown() == 0)) {
			shouldCounterAttack = true;
			unit.setAiOrder("COUNTER-attack: shoot-ready");
		}

		// =========================================================
		// If should counter-attack, do it. Attack the closest enemy.
		if (shouldCounterAttack) {
			shouldCounterAttack = AttackCloseTargets.tryAttackingCloseTargets(unit);
		}

		return shouldCounterAttack;
	}

}

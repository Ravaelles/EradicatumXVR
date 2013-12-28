package ai.managers.units;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.army.ArmyPlacing;
import ai.handling.army.StrengthEvaluator;
import ai.handling.army.TargetHandling;
import ai.handling.map.MapExploration;
import ai.handling.units.CallForHelp;
import ai.handling.units.UnitActions;
import ai.managers.StrategyManager;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.utils.RUtilities;

public class UnitManager {

	public static final UnitTypes WORKER = UnitTypes.Terran_SCV;
	public static final UnitTypes BASE = UnitTypes.Terran_Command_Center;
	public static final UnitTypes BARRACKS = UnitTypes.Terran_Barracks;

	protected static XVR xvr = XVR.getInstance();

	protected static int _unitCounter = 0;

	// ===========================================================

	public static void act() {
		Unit base = xvr.getFirstBase();
		if (base == null) {
			return;
		}

		// ===============================
		// Act with live units
		// ChokePoint beHere = TerranMapExploration.getNearestChokePointFor(xvr,
		// base);

		// Act with non workers
		for (Unit unit : xvr.getUnitsNonWorker()) {
			UnitType type = unit.getType();
			if (type.equals(UnitManager.WORKER)) {
				continue;
			}

			// ===============
			// Act according to strategy, attack strategic targets, go healing,
			// place properly (Strategy phase)
			UnitBasicBehavior.act(unit);

			if (type.isBuilding()) {
				continue;
			}

			// Don't interrupt shooting units
			if (unit.isStartingAttack()) {
				continue;
			}

			if (unit.isBeingHealed()) {
				continue;
			}

			// ===============
			// Attack close targets (Tactics phase)
			if (!type.isTank() && !type.isMedic()) {
				actTryAttackingCloseEnemyUnits(unit);
			}
			if (type.isVulture()) {
				actVulture(unit);
			}

			// Don't interrupt dark templars in killing spree
			// if (type.isDarkTemplar()) {
			// continue;
			// }

			// If enemy has got very close near to us, move away
			UnitBasicBehavior.runFromCloseOpponents(unit);

			// Run from lurkers etc
			avoidHiddenUnitsIfNecessary(unit);

			// ======================================

			// Wounded units should avoid being killed if possible
			handleWoundedUnitBehaviourIfNecessary(unit);

			// If units is jammed and is attacked, attack back
			// handleAntiStuckCode(unit);

			// ======================================
			// Try to load infantry inside bunkers if possible
			if (type.isTerranInfantry()) {
				handleInteractionWithBunkers(unit);
			}

			// ======================================
			// Increase unit counter, so we can know which unit in order it was.
			_unitCounter++;
		}

		// ===============================
		// Reset variables
		_unitCounter = 0;
		CallForHelp.clearOldOnes();
	}

	private static void actVulture(Unit unit) {
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		if (nearestEnemy != null && nearestEnemy.distanceTo(unit) <= 5) {
			UnitActions.moveToSafePlace(unit);
		}
	}

	protected static void handleAntiStuckCode(Unit unit) {
		boolean shouldFightBack = false;

		if (unit.getType().isWorker()) {
			return;
		}

		// If unit is stuck, attack.
		if (unit.isStuck() || unit.isUnderAttack() || unit.isMoving()) {
			Unit nearestEnemy = xvr.getNearestEnemyInRadius(unit, 1, true, true);
			shouldFightBack = nearestEnemy != null && nearestEnemy.isDetected();

			// && xvr.getUnitsInRadius(unit, 2, xvr.getUnitsNonWorker())
			// .size() >= 2
			if (shouldFightBack
					|| unit.isStuck()
					|| (unit.isMoving() && xvr.getUnitsInRadius(unit, 1, xvr.getUnitsNonWorker())
							.size() >= 2) || unit.getGroundWeaponCooldown() == 0) {
				actTryAttackingCloseEnemyUnits(unit);
			}
		}

		else if (unit.getGroundWeaponCooldown() == 0 && unit.isUnderAttack()) {
			if (shouldFightBack) {
				actTryAttackingCloseEnemyUnits(unit);
			}

			// && xvr.getNearestEnemyInRadius(unit, 1) != null
			// if (!StrengthEvaluator.isStrengthRatioCriticalFor(unit)) {
			// actTryAttackingCloseEnemyUnits(unit);
			// }
		}

		// // If unit is stuck, attack.
		// if (unit.isStuck() || unit.isUnderAttack()) {
		// actTryAttackingCloseEnemyUnits(unit);
		// }
		//
		// else if (unit.getGroundWeaponCooldown() == 0 && unit.isUnderAttack())
		// {
		//
		// // && xvr.getNearestEnemyInRadius(unit, 1) != null
		// // if (!StrengthEvaluator.isStrengthRatioCriticalFor(unit)) {
		// actTryAttackingCloseEnemyUnits(unit);
		// // }
		// }
	}

	public static void applyStrengthEvaluatorToAllUnits() {
		for (Unit unit : xvr.getUnitsNonBuilding()) {
			UnitType type = unit.getType();
			if (type.equals(UnitManager.WORKER)) {
				continue;
			}

			// // Don't interrupt dark templars in killing spree
			// if (type.isDarkTemplar()) {
			// continue;
			// }

			// // Some units have special orders
			// if (type.isHighTemplar() || type.isObserver()) {
			// continue;
			// }

			// ============================
			decideSkirmishIfToFightOrRetreat(unit);
			// handleAntiStuckCode(unit);
		}
	}

	protected static void handleWoundedUnitBehaviourIfNecessary(Unit unit) {
		if (unit.getHP() <= unit.getMaxHP() * 0.4) {

			// Now, it doesn't make sense to run away if we're close to some
			// bunker or cannon and we're lonely. In this case it's better to
			// attack, rather than retreat without causing any damage.
			if (isInSuicideShouldFightPosition(unit)) {
				return;
			}

			// If there are tanks nearby, DON'T RUN. Rather die first!
			if (xvr.countUnitsEnemyOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode, 15,
					unit) > 0) {
				return;
			}

			// If there are tanks nearby, DON'T RUN. Rather die first!
			if (unit.distanceTo(xvr.getFirstBase()) < 17) {
				return;
			}

			if (StrategyManager.isAttackPending()) {
				return;
			}

			UnitActions.actWhenLowHitPointsOrShields(unit, false);

			if (unit.isRepairable()) {
				// UnitActions.repairThisUnit(unit);
				RepairAndSons.issueTicketToRepair(unit);

				Unit repairer = RepairAndSons.getRepairerForUnit(unit);
				if (repairer != null && repairer.distanceTo(unit) >= 2) {
					UnitActions.moveTo(unit, repairer);
				} else {
					UnitActions.holdPosition(unit);
				}
			}
		}
	}

	protected static boolean isInSuicideShouldFightPosition(Unit unit) {
		if (!unit.getType().isVulture()) {
			return false;
		}

		// Only if we're only unit in this region it makes sense to die.
		int alliesNearby = -1 + xvr.countUnitsInRadius(unit, 5, true);
		if (alliesNearby <= 0) {
			Unit enemy = xvr.getEnemyDefensiveGroundBuildingNear(unit.getX(), unit.getY());
			if (xvr.getDistanceBetween(enemy, unit) <= 3) {
				return true;
			}
		}

		return false;
	}

	protected static void avoidHiddenUnitsIfNecessary(Unit unit) {
		Unit hiddenEnemyUnitNearby = MapExploration.getHiddenEnemyUnitNearbyTo(unit);
		if (hiddenEnemyUnitNearby != null && unit.isDetected()
				&& !hiddenEnemyUnitNearby.isDetected()) {
			UnitActions.moveAwayFromUnitIfPossible(unit, hiddenEnemyUnitNearby, 9);
		}
	}

	protected static void avoidSeriousSpellEffectsIfNecessary(Unit unit) {
		if (unit.isUnderStorm() || unit.isUnderDisruptionWeb()) {
			if (unit.isMoving()) {
				return;
			}
			UnitActions.moveTo(unit, unit.getX() + 5 * 32 * (-1 * RUtilities.rand(0, 1)),
					unit.getY() + 5 * 32 * (-1 * RUtilities.rand(0, 1)));
		}
	}

	protected static void decideSkirmishIfToFightOrRetreat(Unit unit) {
		Unit firstBase = xvr.getFirstBase();
		// UnitType type = unit.getType();
		if (firstBase == null) {
			return;
		}

		if (!unit.isAttacking() || xvr.getDistanceSimple(unit, firstBase) <= 15) {
			return;
		}
		// if (!unit.isAttacking() || !unit.isUnderAttack()
		// || xvr.getDistanceBetween(unit, firstBase) <= 12) {
		// return;
		// }

		// if (unit.getType().isDarkTemplar() || unit.getType().isObserver()) {
		// if (!unit.isDetected()) {
		// return;
		// }
		// }

		// Don't interrupt shooting units
		// if (type.isDragoon() || type.isReaver()) {
		// Unit enemyNear = xvr.getUnitNearestFromList(unit,
		// xvr.getEnemyUnitsVisible());
		// if (unit != null && enemyNear != null && xvr.getDistanceBetween(unit,
		// enemyNear) <= 3) {
		// return;
		// }
		// }
		if (unit.isStartingAttack()) {
			return;
		}

		// // If there's tank nearby, DON't retreat
		// if
		// (xvr.countUnitsEnemyOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode,
		// 17, unit) > 0) {
		// return;
		// }

		// // If there's ENEMY BUNKER
		// if (xvr.countUnitsEnemyOfGivenTypeInRadius(UnitTypes.Terran_Bunker,
		// 3, unit) > 0) {
		// return;
		// }
		//
		// // If there's ENEMY CANNON
		// if
		// (xvr.countUnitsEnemyOfGivenTypeInRadius(TerranBunker.getBuildingType(),
		// 3, unit) > 0) {
		// return;
		// }

		// If there's our first base nearby
		if (unit.distanceTo(xvr.getFirstBase()) <= 19) {
			return;
		}

		// If there's our first base nearby
		if (xvr.getDistanceBetween(
				xvr.getUnitNearestFromList(unit, TerranCommandCenter.getBases()), unit) <= 10) {
			return;
		}

		// // If there's enemy CANNON
		// if (xvr.getUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(),
		// 4, unit, false).size() > 0) {
		// return;
		// }

		// If there's OUR BUNKER
		if (xvr.countUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(), 6, unit, true) > 0) {
			return;
		}

		// // If there's sunken
		// if (xvr.getUnitsOfGivenTypeInRadius(UnitTypes.Zerg_Sunken_Colony, 3,
		// unit, false).size() > 0) {
		// return;
		// }

		// // Attack Probes if possible
		// if (xvr.getUnitsOfGivenTypeInRadius(UnitTypes.Protoss_Probe, 10,
		// unit, false).size() > 2) {
		// return;
		// }

		if (!StrengthEvaluator.isStrengthRatioFavorableFor(unit)) {
			UnitActions.moveToSafePlace(unit);
		}
	}

	protected static void actTryAttackingCloseEnemyUnits(Unit unit) {
		UnitType type = unit.getType();
		if (type.isMedic() || unit.isBeingHealed() || type.isTankSieged()) {
			return;
		}

		boolean groundAttackCapable = unit.canAttackGroundUnits();
		boolean airAttackCapable = unit.canAttackAirUnits();
		Unit importantEnemyUnitNearby = null;
		Unit enemyToAttack = null;

		// Workers Repairing
		Collection<Unit> enemyWorkers = xvr.getEnemyWorkersInRadius(5, unit);
		if (enemyWorkers != null) {
			for (Unit worker : enemyWorkers) {
				if (worker.isRepairing()) {
					importantEnemyUnitNearby = worker;
					break;
				}
			}

			if (importantEnemyUnitNearby != null) {
				UnitActions.attackEnemyUnit(unit, enemyToAttack);
				return;
			}
		}

		// Normal workers
		Unit enemyWorker = xvr.getEnemyWorkerInRadius(2, unit);
		if (enemyWorker != null) {
			importantEnemyUnitNearby = enemyWorker;
			if (xvr.getDistanceBetween(xvr.getFirstBase(), importantEnemyUnitNearby) > 30
					|| xvr.getTimeSeconds() > 600) {
				UnitActions.attackEnemyUnit(unit, enemyToAttack);
				return;
			}
		}

		// Disallow wounded units to attack distant targets.
		if (XVR.isEnemyTerran() && xvr.getTimeSeconds() > 600) {
			if ((unit.getShields() < 15 || (unit.getShields() < 40 && unit.getHP() < 40))) {
				return;
			}
		}

		// Try selecting top priority units like lurkers, siege tanks.
		importantEnemyUnitNearby = TargetHandling.getImportantEnemyUnitTargetIfPossibleFor(unit,
				groundAttackCapable, airAttackCapable);

		ArrayList<Unit> enemyUnits = xvr
				.getEnemyUnitsVisible(groundAttackCapable, airAttackCapable);

		if (importantEnemyUnitNearby != null && importantEnemyUnitNearby.isDetected()) {
			if (!importantEnemyUnitNearby.getType().isSpiderMine()
					|| (unit.getType().getGroundWeapon().getMaxRange() / 32) >= 2)
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
				return;
			}

			Unit nearestEnemy = xvr.getUnitNearestFromList(unit,
					xvr.getEnemyUnitsVisible(groundAttackCapable, airAttackCapable));

			// If there's an enemy near to this unit, don't change the target.
			if (nearestEnemy != null && xvr.getDistanceBetween(unit, nearestEnemy) <= 1) {
				// return;
			}

			// There's no valid target, attack this enemy.
			else {
				int maxDistance = unit.getType().isFlyer() ? 300 : 10;

				if (!StrengthEvaluator.isStrengthRatioFavorableFor(unit)
						&& unit.distanceTo(xvr.getFirstBase()) > maxDistance) {
					return;
				}

				int distance = (int) xvr.getDistanceSimple(unit, enemyToAttack);
				if (distance < TargetHandling.MAX_DIST) {
					if (XVR.isEnemyTerran() && xvr.getTimeSeconds() < 500) {
						if (enemyToAttack.getType().isBunker()) {
							enemyWorker = xvr.getEnemyWorkerInRadius(12, unit);
							if (enemyWorker != null) {
								importantEnemyUnitNearby = enemyWorker;
								UnitActions.attackEnemyUnit(unit, enemyToAttack);
								return;
							}
							return;
						}
					}
					UnitActions.attackTo(unit, enemyToAttack);
				}
			}
		}

		if (unit.getType().isTerranInfantry()) {
			handleInteractionWithBunkers(unit);
		}
	}

	protected static boolean isUnitInPositionToAlwaysAttack(Unit unit) {
		boolean ourPhotonCannonIsNear = xvr.getUnitsOfGivenTypeInRadius(
				TerranBunker.getBuildingType(), 4, unit, true).size() > 0;
		boolean baseInDanger = (xvr.getDistanceBetween(
				xvr.getUnitNearestFromList(unit, TerranCommandCenter.getBases()), unit) <= 7);

		return ourPhotonCannonIsNear || baseInDanger;
	}

	protected static void actWhenOnCallForHelpMission(Unit unit) {
		Unit caller = unit.getCallForHelpMission().getCaller();

		// If already close to the point to be, cancel order.
		if (xvr.getDistanceBetween(unit, caller) <= 3) {
			unit.getCallForHelpMission().unitArrivedToHelp(unit);
		}

		// Still way to go!
		else {
			if (StrengthEvaluator.isStrengthRatioFavorableFor(unit)) {
				UnitActions.attackTo(unit, caller.getX(), caller.getY());
			}
		}
	}

	protected static boolean actMakeDecisionSomeoneCalledForHelp(Unit unit) {
		for (CallForHelp call : CallForHelp.getThoseInNeedOfHelp()) {
			boolean willAcceptCallForHelp = false;

			// Critical call for help must be accepted
			if (call.isCritical()) {
				willAcceptCallForHelp = true;
			}

			// No critical call, react only if we're not too far
			else if (xvr.getDistanceBetween(call.getCaller(), unit) < 30) {
				willAcceptCallForHelp = true;
			}

			if (willAcceptCallForHelp) {
				call.unitHasAcceptedIt(unit);
				return true;
			}
		}
		return false;
	}

	protected static void actWhenNoMassiveAttack(Unit unit) {
		if (shouldUnitBeExplorer(unit)) {
			UnitActions.spreadOutRandomly(unit);
		} else {
			if (isUnitAttackingSomeone(unit)) {
				return;
			}

			// =====================================
			// Possible override of orders if some unit needs help

			// If some unit called for help
			boolean isOnCallForHelpMission = false;
			if (CallForHelp.isAnyCallForHelp()) {
				boolean accepted = actMakeDecisionSomeoneCalledForHelp(unit);
				if (!isOnCallForHelpMission && accepted) {
					isOnCallForHelpMission = true;
				}
			}

			// Call for help isn't active right now
			if (!isOnCallForHelpMission) {
				ArmyPlacing.goToSafePlaceIfNotAlreadyThere(unit);
			}
		}

		// // Now try to hide in nearby bunker if possible
		// if (unit.isNotMedic()) {
		// ProtossCannon.tryToLoadIntoBunker(unit);
		// }
	}

	protected static boolean shouldUnitBeExplorer(Unit unit) {
		return (_unitCounter == 0 || _unitCounter == 1 || _unitCounter == 7)
				|| unit.getTypeID() == UnitTypes.Protoss_Dark_Templar.ordinal();
	}

	protected static void actWhenMassiveAttackIsPending(Unit unit) {

		// If unit is surrounded by other units (doesn't attack alone)
		// if (isPartOfClusterOfMinXUnits(unit)) {

		// If there is attack target defined, go for it.
		if (StrategyManager.isSomethingToAttackDefined()) {
			if (isUnitAttackingSomeone(unit)) {
				return;
			}

			if (StrategyManager.getTargetPoint() != null) {
				if (!isUnitAttackingSomeone(unit)) {
					UnitActions.attackTo(unit, StrategyManager.getTargetPoint());
				}
				if (isUnitFullyIdle(unit)) {
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

		if (!StrengthEvaluator.isStrengthRatioFavorableFor(unit)) {
			UnitActions.moveToSafePlace(unit);
		}
	}

	protected static boolean isUnitFullyIdle(Unit unit) {
		return !unit.isAttacking() && !unit.isMoving() && !unit.isUnderAttack() && unit.isIdle();
		// && unit.getGroundWeaponCooldown() == 0
	}

	protected static boolean isUnitAttackingSomeone(Unit unit) {
		return unit.getOrderTargetID() != -1 || unit.getTargetUnitID() != -1;
	}

	public static void avoidSeriousSpellEffectsIfNecessary() {
		for (Unit unit : xvr.getBwapi().getMyUnits()) {
			avoidSeriousSpellEffectsIfNecessary(unit);
		}
	}

	protected static void handleInteractionWithBunkers(Unit unit) {
		if (TerranBunker.getNumberOfUnitsCompleted() == 0) {
			return;
		}

		boolean isUnitInsideBunker = unit.isLoaded();
		boolean enemyIsNearby = xvr.getNearestEnemyInRadius(unit, 11, true, true) != null;

		// If unit should be inside bunker, try to load it inside.
		// if (shouldRunToBunker) {
		if (!isUnitInsideBunker) {
			if (!enemyIsNearby && StrategyManager.isAnyAttackFormPending()) {
				return;
			}
			loadIntoBunkerNearbyIfPossible(unit);
		}
		// }

		// Unit shouldn't be inside a bunker, unload it if it's inside.
		else {
			if (!enemyIsNearby) {
				Unit bunker = unit.getBunkerThatsIsLoadedInto();
				if (bunker != null && bunker.getNumLoadedUnits() > 1
						&& StrategyManager.isAnyAttackFormPending()) {
					unit.unload();
				}
			}
		}
	}

	protected static void loadIntoBunkerNearbyIfPossible(Unit unit) {
		final int MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT = 18;

		if (unit.getType().isMedic()) {
			return;
		}

		// Define what is the nearest bunker to this unit
		Unit nearestBunker = xvr.getUnitOfTypeNearestTo(UnitTypes.Terran_Bunker, unit);
		if (nearestBunker == null) {
			return;
		}
		double distToBunker = nearestBunker.distanceTo(unit);

		// If the distance to bunker is small, try to load into it if possible
		if (distToBunker <= MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT) {

			// If this bunekr is free, load into it.
			if (nearestBunker.getNumLoadedUnits() < 4) {
				UnitActions.loadUnitInto(unit, nearestBunker);
				return;
			}

			// Bunker is full, try to find other bunkers in radius that may have
			// space inside
			else {

				// Get the list of bunkers that are near unit.
				ArrayList<Unit> bunkersNearby = xvr.getUnitsOfGivenTypeInRadius(
						UnitTypes.Terran_Bunker, MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT, unit, true);
				for (Unit bunker : bunkersNearby) {
					if (bunker.getNumLoadedUnits() < 4) {
						UnitActions.loadUnitInto(unit, bunker);
						return;
					}
				}
			}
		}

	}

}

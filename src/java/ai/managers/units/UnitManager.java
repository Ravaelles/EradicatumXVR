package ai.managers.units;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.army.ArmyPlacing;
import ai.handling.army.StrengthEvaluator;
import ai.handling.map.MapExploration;
import ai.handling.units.CallForHelp;
import ai.handling.units.UnitActions;
import ai.managers.BuildingManager;
import ai.managers.StrategyManager;
import ai.terran.TerranBunker;
import ai.utils.RUtilities;

public class UnitManager {

	public static final UnitTypes WORKER = UnitTypes.Terran_SCV;
	public static final UnitTypes BASE = UnitTypes.Terran_Command_Center;
	public static final UnitTypes BARRACKS = UnitTypes.Terran_Barracks;

	private static XVR xvr = XVR.getInstance();

	@SuppressWarnings("unused")
	private static int _unitCounter = 0;

	// ===========================================================

	public static void act() {

		// If there's no any base left, we're so fucked up there's no point to
		// continue.
		Unit base = xvr.getFirstBase();
		if (base == null) {
			return;
		}

		// ===============================
		// Act with all UNITS or outsource action to different manager.

		// Act with non workers units
		for (Unit unit : xvr.getUnitsNonWorker()) {
			UnitType type = unit.getType();
			updateBeingRepairedStatus(unit);

			// ===============================
			// BUILDINGS have their own manager.
			if (type.isBuilding()) {
				BuildingManager.act(unit);
				continue;
			}

			// FLYERS (air units) have their own manager.
			if (type.isFlyer()) {
				FlyerManager.act(unit);
				continue;
			}

			// ==============================
			// Should not listen to any actions that we announce here because it
			// would mess the things up.

			// Don't interrupt when shooting or don't move when being repaired.
			if (unit.isStartingAttack() || unit.isBeingRepaired()) {
				continue;
			}

			// ======================================
			// TOP PRIORITY ACTIONS, order is important!

			// Try to load infantry inside bunkers if possible.
			if (type.isTerranInfantry() && UnitBasicBehavior.tryLoadingIntoBunkersIfPossible(unit)) {
				continue;
			}

			// If enemy has got very close near to us, move away
			if (unit.isWounded() && UnitBasicBehavior.runFromCloseOpponentsIfNecessary(unit)) {
				continue;
			}

			// Disallow units to move close to the defensive building like
			// Photon Cannon
			if (UnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(unit)) {
				continue;
			}

			// ===============================
			// Act according to STRATEGY, attack strategic targets,
			// define proper place for a unit.
			UnitBasicBehavior.act(unit);

			// ===============================
			// ATTACK CLOSE targets (Tactics phase)
			boolean canTryAttackingCloseTargets = !type.isVulture() && !type.isTank()
					&& !type.isMedic();
			if (canTryAttackingCloseTargets) {
				AttackCloseTargets.attackCloseTargets(unit);
			}

			// ===============================
			// Run from hidden Lurkers, Dark Templars etc.
			avoidHiddenUnitsIfNecessary(unit);

			// Wounded units should avoid being killed if possible
			handleWoundedUnitBehaviourIfNecessary(unit);

			// If units is jammed and is attacked, attack back
			// handleAntiStuckCode(unit);

			// ======================================
			// Increase unit counter, so we can know which unit in order it was.
			_unitCounter++;
		}

		// ===============================
		// Reset variables
		_unitCounter = 0;
		CallForHelp.clearOldOnes();
	}

	// protected static void handleAntiStuckCode(Unit unit) {
	// boolean shouldFightBack = false;
	//
	// if (unit.getType().isWorker()) {
	// return;
	// }
	//
	// // If unit is stuck, attack.
	// if (unit.isStuck() || unit.isUnderAttack() || unit.isMoving()) {
	// Unit nearestEnemy = xvr.getNearestEnemyInRadius(unit, 1, true, true);
	// shouldFightBack = nearestEnemy != null && nearestEnemy.isDetected();
	//
	// // && xvr.getUnitsInRadius(unit, 2, xvr.getUnitsNonWorker())
	// // .size() >= 2
	// if (shouldFightBack
	// || unit.isStuck()
	// || (unit.isMoving() && xvr.getUnitsInRadius(unit, 1,
	// xvr.getUnitsNonWorker())
	// .size() >= 2) || unit.getGroundWeaponCooldown() == 0) {
	// actTryAttackingCloseEnemyUnits(unit);
	// }
	// }
	//
	// else if (unit.getGroundWeaponCooldown() == 0 && unit.isUnderAttack()) {
	// if (shouldFightBack) {
	// actTryAttackingCloseEnemyUnits(unit);
	// }
	//
	// // && xvr.getNearestEnemyInRadius(unit, 1) != null
	// // if (!StrengthEvaluator.isStrengthRatioCriticalFor(unit)) {
	// // actTryAttackingCloseEnemyUnits(unit);
	// // }
	// }
	//
	// // // If unit is stuck, attack.
	// // if (unit.isStuck() || unit.isUnderAttack()) {
	// // actTryAttackingCloseEnemyUnits(unit);
	// // }
	// //
	// // else if (unit.getGroundWeaponCooldown() == 0 && unit.isUnderAttack())
	// // {
	// //
	// // // && xvr.getNearestEnemyInRadius(unit, 1) != null
	// // // if (!StrengthEvaluator.isStrengthRatioCriticalFor(unit)) {
	// // actTryAttackingCloseEnemyUnits(unit);
	// // // }
	// // }
	// }

	public static void applyStrengthEvaluatorToAllUnits() {
		for (Unit unit : xvr.getUnitsNonBuilding()) {
			UnitType type = unit.getType();
			if (type.equals(UnitManager.WORKER)) {
				continue;
			}

			// ============================
			decideSkirmishIfToFightOrRetreat(unit);
			handleWoundedUnitBehaviourIfNecessary(unit);
			UnitBasicBehavior.runFromCloseOpponentsIfNecessary(unit);
			// handleAntiStuckCode(unit);
		}
	}

	protected static void handleWoundedUnitBehaviourIfNecessary(Unit unit) {
		if (unit.getHP() <= unit.getMaxHP() * 0.4) {
			// // If there are tanks nearby, DON'T RUN. Rather die first!
			// if
			// (xvr.countUnitsEnemyOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode,
			// 15,
			// unit) > 0) {
			// return;
			// }

			// // If there are tanks nearby, DON'T RUN. Rather die first!
			// if (unit.distanceTo(xvr.getFirstBase()) < 17) {
			// return;
			// }

			// if (StrategyManager.isAttackPending()) {
			// return;
			// }

			UnitActions.actWhenLowHitPointsOrShields(unit, false);

			if (unit.isRepairable()) {
				RepairAndSons.issueTicketToRepairIfHasnt(unit);

				Unit repairer = RepairAndSons.getRepairerForUnit(unit);
				if (repairer != null && repairer.distanceTo(unit) >= 1.3) {
					UnitActions.moveTo(unit, repairer);
				} else {
					UnitActions.moveAwayFromNearestEnemy(unit);
				}
			}
		}
	}

	protected static void avoidHiddenUnitsIfNecessary(Unit unit) {
		Unit hiddenEnemyUnitNearby = MapExploration.getHiddenEnemyUnitNearbyTo(unit);
		if (hiddenEnemyUnitNearby != null && unit.isDetected()
				&& !hiddenEnemyUnitNearby.isDetected()) {
			UnitActions.moveAwayFromUnit(unit, hiddenEnemyUnitNearby);
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

		// If no base isn't existing, screw this.
		Unit firstBase = xvr.getFirstBase();
		if (firstBase == null) {
			return;
		}

		// ============================================
		// Some top level situations when don't try retreating

		// Don't interrupt unit that has just started shooting.
		if (unit.isStartingAttack()) {
			return;
		}

		// If unit isn't attacking or is very close to the critical first base,
		// don't retreat.
		// if ((!unit.isAttacking() && !unit.isWorker())
		// || xvr.getDistanceSimple(unit, firstBase) <= 15) {
		// return;
		// }

		// ============================================
		// Now is a huge block of situations where we shouldn't allow a retreat.

		// If there's our first base nearby
		// if (xvr.getDistanceBetween(
		// xvr.getUnitNearestFromList(unit, TerranCommandCenter.getBases()),
		// unit) <= 10) {
		// return;
		// }

		// If there's OUR BUNKER nearby, we should be here at all costs, because
		// if we lose this position, then every other battle will be far tougher
		// than fighting here, near the bunker.
		if (unit.getGroundWeaponCooldown() > 0
				&& xvr.countUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(), 3, unit, true) > 0) {
			return;
		}

		// ===============================================
		// If all is fine, we can CALCULATE CHANCES TO WIN
		// and if we wouldn't win, then go where it's safe
		// and by doing this we may encounter some help.
		if (!StrengthEvaluator.isStrengthRatioFavorableFor(unit)) {
			// UnitActions.moveToSafePlace(unit);
			UnitActions.moveAwayFromNearestEnemy(unit);
		}
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
		return // (_unitCounter == 0 || _unitCounter == 1 || _unitCounter == 7)
				// ||
		unit.getTypeID() == UnitTypes.Terran_Vulture.ordinal();
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

	private static void updateBeingRepairedStatus(Unit unit) {
		if (!unit.isWounded()) {
			unit.setBeingRepaired(false);
		}
	}

}

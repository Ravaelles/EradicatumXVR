package ai.managers.units;

import java.util.ArrayList;
import java.util.Collection;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.army.ArmyPlacing;
import ai.handling.army.StrengthRatio;
import ai.handling.units.CallForHelp;
import ai.handling.units.UnitActions;
import ai.managers.enemy.HiddenEnemyUnitsManager;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.ArmyUnitBasicBehavior;
import ai.managers.units.army.AttackCloseTargets;
import ai.managers.units.army.FlyerManager;
import ai.managers.units.buildings.BuildingManager;

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
		for (Unit unit : xvr.getUnitsNonWorkerAllowIncompleted()) {
			UnitType type = unit.getType();
			updateBeingRepairedStatus(unit);

			// ===============================
			// IF UNIT SHOULD BE HANDLED BY DIFFERENT MANAGE

			// BUILDINGS have their own manager.
			if (type.isBuilding()) {
				BuildingManager.act(unit);
				continue;
			}

			if (!unit.isCompleted()) {
				continue;
			}

			// FLYERS (air units) have their own manager.
			if (type.isFlyer()) {
				FlyerManager.act(unit);
				continue;
			}

			// ===============================
			act(unit);

			// ======================================
			// Increase unit counter, so we can know which unit in order it was.
			_unitCounter++;
		}

		// ===============================
		// Reset variables
		_unitCounter = 0;
		CallForHelp.clearOldOnes();
	}

	private static void act(Unit unit) {
		UnitType type = unit.getType();

		// ==============================
		// Should not listen to any actions that we announce here because it
		// would mess the things up.

		// Don't interrupt when shooting or don't move when being repaired.
		if (unit.isStartingAttack() || unit.isBeingRepaired() || unit.isRunningFromEnemy()
				|| type.isSpiderMine()) {
			return;
		}

		// *UPDATE* value of strength ratio.
		StrengthRatio.recalculateFor(unit);

		// ======================================
		// TOP PRIORITY ACTIONS, order is important!

		// Try to load infantry inside bunkers if possible.
		if (ArmyUnitBasicBehavior.tryLoadingIntoBunkersIfPossible(unit)) {
			unit.setAiOrder("Into bunker");
			return;
		}

		// Wounded units should avoid being killed (if possible you know...)
		if (ArmyUnitBasicBehavior.tryRunningIfSeriouslyWounded(unit)) {
			unit.setAiOrder("Is badly wounded");
			return;
		}

		// If enemy has got very close near to us, move away
		if (ArmyUnitBasicBehavior.runFromCloseOpponentsIfNecessary(unit)) {
			unit.setAiOrder("Run from enemy");
			return;
		}

		// Disallow units to move close to the defensive building like
		// Photon Cannon
		if (ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(unit)) {
			return;
		}

		// Disallow fighting when overwhelmed.
		if (ArmyUnitBasicBehavior.tryRetreatingIfChancesNotFavorable(unit)) {
			return;
		}

		// ===============================
		// Act according to STRATEGY, attack strategic targets,
		// define proper place for a unit.
		ArmyUnitBasicBehavior.act(unit);

		// ===============================
		// ATTACK CLOSE targets (Tactics phase)
		if (AttackCloseTargets.tryAttackingCloseTargets(unit)) {
			unit.setAiOrder("Attack close targets");
		}
		ArmyUnitBasicBehavior.tryUsingStimpacksIfNeeded(unit);

		// ===============================
		// Run from hidden Lurkers, Dark Templars etc.
		HiddenEnemyUnitsManager.avoidHiddenUnitsIfNecessary(unit);

		// If enemy has got very close near to us, move away
		if (ArmyUnitBasicBehavior.runFromCloseOpponentsIfNecessary(unit)) {
			unit.setAiOrder("Run from enemy 2");
			return;
		}
	}

	// =========================================================

	public static void actWhenOnCallForHelpMission(Unit unit) {
		Unit caller = unit.getCallForHelpMission().getCaller();

		// If already close to the point to be, cancel order.
		if (xvr.getDistanceBetween(unit, caller) <= 3) {
			unit.getCallForHelpMission().unitArrivedToHelp(unit);
		}

		// Still way to go!
		else {
			if (StrengthRatio.isStrengthRatioFavorableFor(unit)) {
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

	public static void actWhenNoMassiveAttack(Unit unit) {
		if (shouldUnitBeExplorer(unit) && !unit.isRunningFromEnemy()) {
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
		return (_unitCounter == 5 || _unitCounter == 14);
		// unit.getTypeID() == UnitTypes.Terran_Vulture.ordinal();
	}

	public static void actWhenMassiveAttackIsPending(Unit unit) {

		// If unit is surrounded by other units (doesn't attack alone)
		// if (isPartOfClusterOfMinXUnits(unit)) {

		// If there is attack target defined, go for it.
		if (StrategyManager.isSomethingToAttackDefined()) {
			if (unitIsTooFarFromSafePlaceWhenAttackPending(unit)) {
				return;
			}

			if (isUnitAttackingSomeone(unit)) {
				return;
			}

			if (StrategyManager.getTargetPoint() != null) {
				if (!isUnitAttackingSomeone(unit)) {
					UnitActions.attackTo(unit, StrategyManager.getTargetPoint());
					unit.setAiOrder("Forward!");
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

		if (!StrengthRatio.isStrengthRatioFavorableFor(unit)) {
			UnitActions.moveToSafePlace(unit);
		}
	}

	private static boolean unitIsTooFarFromSafePlaceWhenAttackPending(Unit unit) {
		if (StrategyManager.getMinBattleUnits() <= 5) {
			return false;
		}

		if (unit.distanceTo(ArmyPlacing.getSafePointFor(unit)) > StrategyManager
				.getAllowedDistanceFromSafePoint()) {
			// ArmyPlacing.goToSafePlaceIfNotAlreadyThere(unit);
			UnitActions.attackTo(unit, unit.getX(), unit.getY());
			return true;
		}
		return false;
	}

	protected static boolean isUnitFullyIdle(Unit unit) {
		return !unit.isAttacking() && !unit.isMoving() && !unit.isUnderAttack() && unit.isIdle();
		// && unit.getGroundWeaponCooldown() == 0
	}

	protected static boolean isUnitAttackingSomeone(Unit unit) {
		return unit.getOrderTargetID() != -1 || unit.getTargetUnitID() != -1;
	}

	public static void avoidSpellEffectsAndMinesIfNecessary() {
		for (Unit unit : xvr.getBwapi().getMyUnits()) {
			if (ArmyUnitBasicBehavior.tryAvoidingSeriousSpellEffectsIfNecessary(unit)) {
				continue;
			}
			if (ArmyUnitBasicBehavior.tryAvoidingActivatedSpiderMines(unit)) {
				continue;
			}
		}
	}

	private static void updateBeingRepairedStatus(Unit unit) {
		if (!unit.isWounded()) {
			unit.setBeingRepaired(false);
		}
	}

	public static boolean isUnitSafeFromEnemiesShootRange(Unit unit, Collection<Unit> enemies) {
		// int ourRange = unit.getType().getGroundWeapon().getMinRangeInTiles();

		for (Unit enemy : enemies) {
			int enemyRange = enemy.getType().getGroundWeapon().getMinRangeInTiles();
			if (unit.distanceTo(enemy) < 1.5 + enemyRange) {
				return false;
			}
		}

		return true;
	}

	public static boolean areVeryCloseUnitsReatreting(Unit unit, Collection<Unit> units) {
		ArrayList<Unit> veryCloseTeammates = xvr.getUnitsInRadius(unit, 2.8, units);
		veryCloseTeammates.remove(unit);
		for (Unit teammate : veryCloseTeammates) {
			if (teammate.isRunningFromEnemy()) {
				return true;
			}
		}
		return false;
	}

}

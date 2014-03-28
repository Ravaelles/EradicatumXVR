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
import ai.managers.units.army.RunManager;
import ai.managers.units.buildings.BuildingManager;
import ai.managers.units.workers.RepairAndSons;

public class UnitManager {

	public static final UnitTypes WORKER = UnitTypes.Terran_SCV;
	public static final UnitTypes BASE = UnitTypes.Terran_Command_Center;
	public static final UnitTypes BARRACKS = UnitTypes.Terran_Barracks;

	private static XVR xvr = XVR.getInstance();

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

			// Reject non controllable unit types
			if (type.isSpiderMine()) {
				continue;
			}

			// =========================================================

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
		// UnitType type = unit.getType();

		// ==============================
		// Should not listen to any actions that we announce here because it
		// would mess the things up.

		// Don't interrupt when shooting or don't move when being repaired.
		if (unit.isStartingAttack()) {
			return;
		}

		// *UPDATE* value of strength ratio.
		StrengthRatio.recalculateFor(unit);

		// ======================================
		// TOP PRIORITY ACTIONS, order is important!

		// Avoid activated mines
		if (ArmyUnitBasicBehavior.tryAvoidingActivatedSpiderMines(unit)) {
			return;
		}

		// Make sure unit will get repaired
		if (RepairAndSons.tryGoingToRepairIfNeeded(unit)) {
			return;
		}

		// Use Stimpacks if need.
		ArmyUnitBasicBehavior.tryUsingStimpacksIfNeeded(unit);

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
		if (RunManager.runFromCloseOpponentsIfNecessary(unit)) {
			return;
		}

		// Disallow units to move close to the defensive building like
		// Photon Cannon
		if (ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(unit)) {
			return;
		}

		// Disallow fighting when overwhelmed.
		// if (ArmyUnitBasicBehavior.tryRetreatingIfChancesNotFavorable(unit)) {
		// unit.setAiOrder("Would lose");
		// return;
		// }

		// ===============================
		// Act according to STRATEGY, attack strategic targets,
		// define proper place for a unit.
		ArmyUnitBasicBehavior.act(unit);

		// ===============================
		// Run from hidden Lurkers, Dark Templars etc.
		HiddenEnemyUnitsManager.avoidHiddenUnitsIfNecessary(unit);

		// ===============================
		// Don't interrupt units being repaired
		if (unit.isBeingRepaired()) {
			return;
		}

		// ===============================
		// ATTACK CLOSE targets (Tactics phase)
		if (AttackCloseTargets.tryAttackingCloseTargets(unit)) {
			unit.setAiOrder("Attack close targets");
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

	public static boolean decideWhetherToHelpSomeoneCalledForHelp(Unit unit) {
		for (CallForHelp call : CallForHelp.getThoseInNeedOfHelp()) {
			boolean willAcceptCallForHelp = false;

			// Critical call for help must be accepted
			if (call.isCritical()) {
				willAcceptCallForHelp = true;
			}

			// No critical call, react only if we're not too far
			else if (xvr.getDistanceBetween(call.getCaller(), unit) < 20) {
				willAcceptCallForHelp = true;
			}

			if (willAcceptCallForHelp) {
				call.unitHasAcceptedIt(unit);
				return true;
			}
		}
		return false;
	}

	public static boolean shouldUnitBeExplorer(Unit unit) {
		return (_unitCounter == 5 || _unitCounter == 14);
		// unit.getTypeID() == UnitTypes.Terran_Vulture.ordinal();
	}

	public static boolean unitIsTooFarFromSafePlaceWhenAttackPending(Unit unit) {
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

	public static boolean isUnitFullyIdle(Unit unit) {
		return !unit.isAttacking() && !unit.isMoving() && !unit.isUnderAttack() && unit.isIdle();
		// && unit.getGroundWeaponCooldown() == 0
	}

	public static boolean isHasValidTargetToAttack(Unit unit) {
		return unit.getOrderTargetID() != -1 || unit.getTargetUnitID() != -1
				|| unit.isStartingAttack();
	}

	public static void avoidSpellEffectsAndMinesIfNecessary() {
		for (Unit unit : xvr.getBwapi().getMyUnits()) {
			if (ArmyUnitBasicBehavior.tryAvoidingSeriousSpellEffectsIfNecessary(unit)) {
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

	public static boolean isUnitSafeFromEnemyShootRange(Unit unit, Unit enemy) {
		// int ourRange = unit.getType().getGroundWeapon().getMinRangeInTiles();

		int enemyRange = enemy.getType().getGroundWeapon().getMinRangeInTiles();
		if (unit.distanceTo(enemy) < 1.7 + enemyRange) {
			return false;
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

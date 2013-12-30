package ai.managers.units;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.army.ArmyPlacing;
import ai.handling.army.StrengthEvaluator;
import ai.handling.units.CallForHelp;
import ai.handling.units.UnitActions;
import ai.managers.BuildingManager;
import ai.managers.StrategyManager;
import ai.terran.TerranBunker;

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
			// IF UNIT SHOULD BE HANDLED BY DIFFERENT MANAGE

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

		// ======================================
		// TOP PRIORITY ACTIONS, order is important!

		// Try to load infantry inside bunkers if possible.
		if (UnitBasicBehavior.tryLoadingIntoBunkersIfPossible(unit)) {
			unit.setAiOrder("Into bunker");
			return;
		}

		// If enemy has got very close near to us, move away
		if (UnitBasicBehavior.runFromCloseOpponentsIfNecessary(unit)) {
			unit.setAiOrder("Run");
			return;
		}

		// Disallow units to move close to the defensive building like
		// Photon Cannon
		if (UnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(unit)) {
			unit.setAiOrder("Avoid building");
			return;
		}

		// Disallow fighting when overwhelmed.
		if (tryRetreatingIfChancesNotFavorable(unit)) {
			unit.setAiOrder("Would lose");
			return;
		}

		// ===============================
		// Act according to STRATEGY, attack strategic targets,
		// define proper place for a unit.
		UnitBasicBehavior.act(unit);

		// ===============================
		// ATTACK CLOSE targets (Tactics phase)
		if (AttackCloseTargets.tryAttackingCloseTargets(unit)) {
			UnitBasicBehavior.tryUsingStimpacksIfNeeded(unit);
			unit.setAiOrder("Attack");
		}

		// ===============================
		// Run from hidden Lurkers, Dark Templars etc.
		UnitBasicBehavior.avoidHiddenUnitsIfNecessary(unit);

		// Wounded units should avoid being killed (if possible you know...)
		UnitBasicBehavior.handleWoundedUnitBehaviourIfNecessary(unit);
	}

	protected static boolean tryRetreatingIfChancesNotFavorable(Unit unit) {

		// If no base isn't existing, screw this.
		Unit firstBase = xvr.getFirstBase();
		if (firstBase == null) {
			return false;
		}

		// ============================================
		// Some top level situations when don't try retreating

		// If no enemy is critically close, don't retreat
		if (xvr.getNearestEnemyDistance(unit, true, false) <= 2) {
			return false;
		}

		// Don't interrupt unit that has just started shooting.
		if (unit.isStartingAttack()) {
			return false;
		}

		// MEDICS can run only if INJURED
		if (unit.getType().isMedic() && !unit.isWounded()) {
			return false;
		}

		// If unit isn't attacking or is very close to the critical first base,
		// don't retreat.
		// if ((!unit.isAttacking() && !unit.isWorker())
		// || xvr.getDistanceSimple(unit, firstBase) <= 15) {
		// return;
		// }

		// ============================================
		// Now is a block of situations where we shouldn't allow a retreat.

		// If there's our first base nearby
		// if (xvr.getDistanceBetween(
		// xvr.getUnitNearestFromList(unit, TerranCommandCenter.getBases()),
		// unit) <= 10) {
		// return;
		// }

		// If there's OUR BUNKER nearby, we should be here at all costs, because
		// if we lose this position, then every other battle will be far tougher
		// than fighting here, near the bunker.
		if (!unit.isWounded()
				&& unit.getGroundWeaponCooldown() > 0
				&& xvr.countUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(), 3.5, unit,
						true) > 0) {
			// if () {
			return false;
			// }
		}

		// ===============================================
		// If all is fine, we can CALCULATE CHANCES TO WIN
		// and if we wouldn't win, then go where it's safe
		// and by doing this we may encounter some help.
		if (!StrengthEvaluator.isStrengthRatioFavorableFor(unit)) {
			// UnitActions.moveToSafePlace(unit);
			UnitActions.moveAwayFromNearestEnemy(unit);
			return true;
		}

		return false;
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
		return // (_unitCounter == 0 || _unitCounter == 1 || _unitCounter == 7)
				// ||
		unit.getTypeID() == UnitTypes.Terran_Vulture.ordinal();
	}

	protected static void actWhenMassiveAttackIsPending(Unit unit) {

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

	private static boolean unitIsTooFarFromSafePlaceWhenAttackPending(Unit unit) {
		if (StrategyManager.getMinBattleUnits() <= 5) {
			return false;
		}

		if (unit.distanceTo(ArmyPlacing.getSafePointFor(unit)) > StrategyManager
				.getAllowedDistanceFromSafePoint()) {
			ArmyPlacing.goToSafePlaceIfNotAlreadyThere(unit);
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
			if (UnitBasicBehavior.tryAvoidingSeriousSpellEffectsIfNecessary(unit)) {
				continue;
			}
			if (UnitBasicBehavior.tryAvoidingActivatedSpiderMines(unit)) {
				continue;
			}
		}
	}

	private static void updateBeingRepairedStatus(Unit unit) {
		if (!unit.isWounded()) {
			unit.setBeingRepaired(false);
		}
	}

}

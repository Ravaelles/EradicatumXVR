package ai.managers.strategy;

import java.util.Collection;

import jnibwapi.model.Unit;
import ai.core.Painter;
import ai.core.XVR;
import ai.handling.army.TargetHandling;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitCounter;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.terran.TerranMedic;
import ai.terran.TerranSiegeTank;

public class StrategyManager {

	public static boolean FORCE_CRAZY_ATTACK = false;

	// =========================================================

	private static XVR xvr = XVR.getInstance();

	// private static final int MINIMUM_INITIAL_ARMY_TO_PUSH_ONE_TIME = 5;
	// private static final int MINIMUM_NON_INITIAL_ARMY_TO_PUSH = 25;
	private static final int MINIMUM_THRESHOLD_ARMY_TO_PUSH = 20;
	public static final int INITIAL_MIN_UNITS = 1;
	// private static final int MINIMUM_ARMY_PSI_USED_THRESHOLD = 75;

	/**
	 * It means we are NOT ready to attack the enemy, because we suck pretty
	 * badly.
	 */
	private static final int STATE_PEACE = 5;

	/** We have approved attack plan, now we should define targets. */
	private static final int STATE_NEW_ATTACK = 7;

	/** Attack is currently pending. */
	private static final int STATE_ATTACK_PENDING = 9;

	/** Attack has failed, we're taking too much losses, retreat somewhere. */
	private static final int STATE_RETREAT = 11;

	// =====================================================

	/**
	 * Current state of attack. Only allowed values are constants of this class
	 * that are prefixed with STATE_XXX.
	 */
	private static int currentState = STATE_PEACE;

	/**
	 * If we are ready to attack it represents pixel coordinates of place, where
	 * our units will move with "Attack" order. It represents place, not the
	 * specific Unit. Units are supposed to attack the neighborhood of this
	 * point.
	 */
	private static MapPoint _attackPoint;

	/**
	 * If we are ready to attack it represents the unit that is the focus of our
	 * armies. Based upon this variable _attackPoint will be defined. If this
	 * value is null it means that we have destroyed this unit/building and
	 * should find next target, so basically it should be almost always
	 * non-null.
	 */
	private static Unit _attackTargetUnit;

	@SuppressWarnings("unused")
	private static int retreatsCounter = 0;
	@SuppressWarnings("unused")
	private static final int EXTRA_UNITS_PER_RETREAT = 1;
	private static final int MIN_MEDICS = 3;
	private static final int MIN_TANKS = 3;

	private static int _minBattleUnits = INITIAL_MIN_UNITS;
	private static int _lastTimeWaitCalled = 0;

	private static double allowedDistanceFromSafePoint = 0;
	private static int _lastTimeDistancePenalty = 0;

	private static final double STEP_DISTANCE_WHEN_ATTACK_PENDING = 0.73;
	private static final int MINIMAL_DISTANCE_FROM_SAFE_POINT = 3;

	// private static boolean pushedInitially = false;

	// ====================================================

	/**
	 * Decide if full attack makes sense or if we're already attacking decide
	 * whether to retreat, continue attack or to change target.
	 */
	public static void evaluateMassiveAttackOptions() {

		// Always attack?
		if (FORCE_CRAZY_ATTACK) {
			currentState = STATE_ATTACK_PENDING;
			allowedDistanceFromSafePoint = 999;
			return;
		}

		// Currently we are nor attacking, nor retreating.
		if (!isAnyAttackFormPending()) {
			decisionWhenNotAttacking();
		}

		// We are either attacking or retreating.
		if (isAnyAttackFormPending()) {
			allowedDistanceFromSafePoint += STEP_DISTANCE_WHEN_ATTACK_PENDING;
			decisionWhenAttacking();
		}
	}

	private static boolean decideIfWeAreReadyToAttack() {
		int battleUnits = UnitCounter.getNumberOfBattleUnitsCompleted();
		int minUnits = calculateMinimumUnitsToAttack();

		if (battleUnits > minUnits) {
			return true;
		}

		boolean haveEnoughMedics = TerranMedic.getNumberOfUnitsCompleted() >= MIN_MEDICS;
		boolean haveEnoughTanks = TerranSiegeTank.getNumberOfUnitsCompleted() >= MIN_TANKS;

		if (haveEnoughMedics && haveEnoughTanks && battleUnits >= minUnits || _minBattleUnits <= 5) {
			return true;
		} else {
			boolean weAreReady = (battleUnits >= minUnits * 0.35) && isAnyAttackFormPending();

			if (battleUnits > MINIMUM_THRESHOLD_ARMY_TO_PUSH) {
				weAreReady = true;
			}

			return weAreReady;
		}
	}

	// =========================================================

	public static int calculateMinimumUnitsToAttack() {
		return getMinBattleUnits();
		// return getMinBattleUnits() + retreatsCounter *
		// EXTRA_UNITS_PER_RETREAT
		// + (retreatsCounter >= 2 ? retreatsCounter * 2 : 0);
	}

	private static void decisionWhenNotAttacking() {

		// According to many different factors decide if we should attack
		// enemy.
		boolean shouldAttack = decideIfWeAreReadyToAttack();

		// If we should attack, change the status correspondingly.
		if (shouldAttack) {
			changeStateTo(STATE_NEW_ATTACK);
		} else {
			armyIsNotReadyToAttack();
		}
	}

	private static void decisionWhenAttacking() {

		// If our army is ready to attack the enemy...
		if (isNewAttackState()) {
			changeStateTo(STATE_ATTACK_PENDING);

			// We will try to define place for our army where to attack. It
			// probably will be center around a crucial building like Command
			// Center. But what we really need is the point where to go, not the
			// unit. As long as the point is defined we can attack the enemy.

			// If we don't have defined point where to attack it means we
			// haven't yet decided where to go. So it's the war's very start.
			// Define this assault point now. It would be reasonable to relate
			// it to a particular unit.
			// if (!isPointWhereToAttackDefined()) {
			StrategyManager.defineInitialAttackTarget();
			// }
		}

		// Attack is pending, it's quite "regular" situation.
		if (isAttackPending()) {

			// Now we surely have defined our point where to attack, but it can
			// be so, that the unit which was the target has been destroyed
			// (e.g. just a second ago), so we're standing in the middle of
			// wasteland.
			// In this case define next target.
			// if (!isSomethingToAttackDefined()) {
			defineNextTarget();
			// }

			// Check again if continue attack or to retreat.
			boolean shouldAttack = decideIfWeAreReadyToAttack();
			if (!shouldAttack) {
				retreatsCounter++;
				changeStateTo(STATE_RETREAT);
			}
		}

		// If we should retreat... fly you fools!
		if (isRetreatNecessary()) {
			retreat();
		}
	}

	public static void forceRedefinitionOfNextTarget() {
		_attackPoint = null;
		_attackTargetUnit = null;
		defineNextTarget();
	}

	private static void defineNextTarget() {
		Unit target = TargetHandling.getImportantEnemyUnitTargetIfPossibleFor(
				ArmyRendezvousManager.getArmyCenterPoint(), true, true);
		Collection<Unit> enemyBuildings = xvr.getEnemyBuildings();

		// Remove refineries, geysers etc
		// for (Iterator<Unit> iterator = enemyBuildings.iterator();
		// iterator.hasNext();) {
		// Unit unit = (Unit) iterator.next();
		// if (unit.getType().isOnGeyser()) {
		// iterator.remove();
		// }
		// }

		// Try to target some crucial building
		if (!TargetHandling.isProperTarget(target)) {
			target = TargetHandling.findTopPriorityTargetIfPossible(enemyBuildings, true, false);
		}
		if (!TargetHandling.isProperTarget(target)) {
			target = TargetHandling.findHighPriorityTargetIfPossible(enemyBuildings);
		}
		if (!TargetHandling.isProperTarget(target)) {
			target = TargetHandling.findNormalPriorityTargetIfPossible(enemyBuildings);
		}

		// If not target found attack the nearest building
		if (!TargetHandling.isProperTarget(target)) {
			Unit base = xvr.getFirstBase();
			if (base == null) {
				return;
			}
			target = xvr.getUnitNearestFromList(base.getX(), base.getY(), enemyBuildings, true,
					false);
		}

		// Update the target.
		if (target != null) {
			if (_attackTargetUnit != target) {
				changeNextTargetTo(target);
			} else {
				updateTargetPosition();
			}
		} else {
			_attackPoint = null;
			if (_attackTargetUnit != target && _attackTargetUnit == null) {
				Painter.message(xvr, "Next target is null... =/");
			}
		}

		// System.out.println("_attackTargetUnit = " +
		// _attackTargetUnit.toStringShort());
		// System.out.println("_attackPoint = " + _attackPoint);
		// System.out.println("isProperTarget(target) = " +
		// TargetHandling.isProperTarget(target));
		// System.out.println();
	}

	private static void updateTargetPosition() {
		_attackPoint = _attackTargetUnit;
	}

	private static void retreat() {
		changeStateTo(STATE_PEACE);
	}

	private static void changeStateTo(int newState) {
		currentState = newState;
		if (currentState == STATE_PEACE || currentState == STATE_NEW_ATTACK) {
			armyIsNotReadyToAttack();
		}
	}

	public static boolean isAnyAttackFormPending() {
		return currentState != STATE_PEACE;
	}

	private static boolean isNewAttackState() {
		return currentState == STATE_NEW_ATTACK;
	}

	public static boolean isAttackPending() {
		return currentState == STATE_ATTACK_PENDING;
	}

	public static boolean isRetreatNecessary() {
		return currentState == STATE_RETREAT;
	}

	public static boolean isSomethingToAttackDefined() {
		// return _attackUnitNeighbourhood != null
		// && _attackUnitNeighbourhood.isExists();
		return _attackTargetUnit != null && !_attackTargetUnit.getType().isOnGeyser();
	}

	private static void armyIsNotReadyToAttack() {
		_attackPoint = null;
		_attackTargetUnit = null;
		allowedDistanceFromSafePoint = MINIMAL_DISTANCE_FROM_SAFE_POINT;
	}

	private static void defineInitialAttackTarget() {
		// Unit buildingToAttack = MapExploration.getNearestEnemyBase();
		Unit buildingToAttack = MapExploration.getNearestEnemyBuilding();

		// We know some building of CPU that we can attack.
		if (buildingToAttack != null) {
			changeNextTargetTo(buildingToAttack);
		}

		// No building to attack found, safely decide not to attack.
		else {
			changeStateTo(STATE_PEACE);
		}
	}

	private static void changeNextTargetTo(Unit attackTarget) {
		if (attackTarget == null) {
			Painter.message(xvr, "ERROR! ATTACK TARGET UNKNOWN!");
			return;
		}
		// Debug.message(xvr, "Next to attack: "
		// + attackTarget.getType().getName());

		_attackTargetUnit = attackTarget;
		updateTargetPosition();
	}

	public static Unit getTargetUnit() {
		return _attackTargetUnit;
	}

	public static MapPoint getTargetPoint() {
		return _attackPoint;
	}

	private static void waitUntilMinBattleUnits() {
		if (_minBattleUnits <= MINIMAL_DISTANCE_FROM_SAFE_POINT) {
			_minBattleUnits = 26;
		}

		int now = xvr.getTimeSeconds();
		if (now - _lastTimeWaitCalled > 100) {
			_lastTimeWaitCalled = now;

			// if (minBattleUnits < minUnits) {
			// minBattleUnits += minUnits;
			// }

			retreatsCounter++;
			forcePeace();
		}

	}

	public static int getMinBattleUnits() {
		return _minBattleUnits;
	}

	public static void forcePeace() {
		allowedDistanceFromSafePoint = MINIMAL_DISTANCE_FROM_SAFE_POINT;
		changeStateTo(STATE_PEACE);
	}

	public static void waitForMoreUnits() {
		waitUntilMinBattleUnits();
	}

	public static double getAllowedDistanceFromSafePoint() {
		return allowedDistanceFromSafePoint;
	}

	public static void reduceAllowedDistanceFromSafePoint() {
		int now = xvr.getTimeSeconds();
		_lastTimeDistancePenalty = now;

		// if (now - _lastTimeDistancePenalty >= 4) {
		// allowedDistanceFromSafePoint *= 0.82;
		// }
	}

	public static void reduceSlightlyAllowedDistanceFromSafePoint() {
		int now = xvr.getTimeSeconds();
		if (now - _lastTimeDistancePenalty >= 2) {
			_lastTimeDistancePenalty = now;

			allowedDistanceFromSafePoint -= STEP_DISTANCE_WHEN_ATTACK_PENDING;
			if (allowedDistanceFromSafePoint < MINIMAL_DISTANCE_FROM_SAFE_POINT) {
				allowedDistanceFromSafePoint = MINIMAL_DISTANCE_FROM_SAFE_POINT;
			}
		}
	}
}

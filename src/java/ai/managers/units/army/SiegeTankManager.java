package ai.managers.units.army;

import java.util.HashMap;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.TankTargeting;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitManager;

public class SiegeTankManager {

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Siege_Tank_Tank_Mode;

	static class DecisionHelper {
		static boolean _isUnitWhereItShouldBe;
		static MapPoint _properPlace;
		static Unit _nearestEnemy;
		static Unit _nearestEnemyBuilding;
		static double _nearestEnemyDist;

		/**
		 * If a tank is in this map it means it is considering unsieging, but we
		 * should wait some time before this happens.
		 */
		static HashMap<Unit, Integer> unsiegeIdeasMap = new HashMap<>();
	}

	// =========================================================

	public static void act(Unit unit) {
		DecisionHelper._properPlace = unit.getProperPlaceToBe();
		updateProperPlaceToBeForTank(unit);
		DecisionHelper._isUnitWhereItShouldBe = DecisionHelper._properPlace == null
				|| DecisionHelper._properPlace.distanceTo(unit) <= 3;
		DecisionHelper._nearestEnemy = xvr.getNearestGroundEnemy(unit);
		DecisionHelper._nearestEnemyBuilding = MapExploration.getNearestEnemyBuilding(unit);
		DecisionHelper._nearestEnemyDist = DecisionHelper._nearestEnemy != null ? DecisionHelper._nearestEnemy
				.distanceTo(unit) : -1;

		if (unit.isSieged()) {
			actWhenSieged(unit);
		} else {
			actWhenInNormalMode(unit);
		}
	}

	private static void updateProperPlaceToBeForTank(Unit unit) {
		Unit nearestArmyUnit = xvr.getUnitNearestFromList(unit, xvr.getUnitsArmyNonTanks(), true,
				false);
		if (nearestArmyUnit != null && !nearestArmyUnit.isLoaded()) {
			DecisionHelper._properPlace = nearestArmyUnit;
		}

		if (StrategyManager.isAnyAttackFormPending()) {
			DecisionHelper._properPlace = StrategyManager.getTargetPoint();
		}
	}

	private static void actWhenInNormalMode(Unit unit) {
		if (shouldSiege(unit) && notTooManySiegedUnitHere(unit) && didntJustUnsiege(unit)) {
			unit.siege();
		}

		if (mustSiege(unit)) {
			unit.siege();
		}
	}

	private static boolean mustSiege(Unit unit) {

		// If there's enemy building in range, siege.
		if (DecisionHelper._nearestEnemyBuilding != null
				&& DecisionHelper._nearestEnemyBuilding.distanceTo(unit) <= 10.4) {
			return true;
		}

		return false;
	}

	private static boolean didntJustUnsiege(Unit unit) {
		return unit.getLastTimeSieged() + 5 <= xvr.getTimeSeconds();
	}

	private static void actWhenSieged(Unit unit) {
		unit.setLastTimeSieged(xvr.getTimeSeconds());

		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		double nearestEnemyDist = nearestEnemy != null ? nearestEnemy.distanceTo(unit) : -1;

		boolean neighborhoodDangerous = nearestEnemyDist >= 0 && nearestEnemyDist <= 2.5
				&& unit.getStrengthRatio() < 1.8;
		// boolean chancesRatherBad = unit.getStrengthRatio() < 1.1;
		if (neighborhoodDangerous) {
			unit.unsiege();
			return;
		}

		// If tank from various reasons shouldn't be here, unsiege.
		if (!shouldSiege(unit) && !unit.isStartingAttack()) {
			infoTankIsConsideringUnsieging(unit);
		}

		// If tank should be here, try to attack proper target.
		else {
			MapPoint targetForTank = TankTargeting.defineTargetForSiegeTank(unit);
			if (targetForTank != null) {
				if (targetForTank instanceof Unit && ((Unit) targetForTank).isDetected()) {
					UnitActions.attackEnemyUnit(unit, (Unit) targetForTank);
				} else {
					UnitActions.attackTo(unit, targetForTank);
				}
			} else {
				if (nearestEnemy != null) {
					if (nearestEnemyDist >= 0 && (nearestEnemyDist < 2 || nearestEnemyDist <= 7)) {
						infoTankIsConsideringUnsieging(unit);
					}
				}
			}
		}

		if (isUnsiegingIdeaTimerExpired(unit)) {
			if (!mustSiege(unit)) {
				unit.unsiege();
			}
		}
	}

	private static boolean isUnsiegingIdeaTimerExpired(Unit unit) {
		if (DecisionHelper.unsiegeIdeasMap.containsKey(unit)) {
			if (xvr.getTimeSeconds() - DecisionHelper.unsiegeIdeasMap.get(unit) >= 14) {
				return true;
			}
		}
		return false;
	}

	private static void infoTankIsConsideringUnsieging(Unit unit) {
		if (!DecisionHelper.unsiegeIdeasMap.containsKey(unit)) {
			DecisionHelper.unsiegeIdeasMap.put(unit, xvr.getTimeSeconds());
		}
	}

	private static boolean shouldSiege(Unit unit) {
		boolean isEnemyNearShootRange = (DecisionHelper._nearestEnemyDist > 0 && DecisionHelper._nearestEnemyDist <= (DecisionHelper._nearestEnemy
				.getType().isBuilding() ? 10.6 : 13));

		// Check if should siege, based on unit proper place to be (e.g. near
		// the bunker), but consider the neighborhood, if it's safe etc.
		if ((DecisionHelper._isUnitWhereItShouldBe && notTooManySiegedInArea(unit))
				|| isEnemyNearShootRange) {
			if (canSiegeInThisPlace(unit) && isNeighborhoodSafeToSiege(unit)) {
				return true;
			}
		}

		// If there's an enemy in the range of shoot and there are some other
		// units around this tank, then siege.
		int oursNearby = xvr.countUnitsOursInRadius(unit, 7);
		if (isEnemyNearShootRange && oursNearby >= 5) {
			return true;
		}

		// If there's enemy building in range, siege.
		if (DecisionHelper._nearestEnemyBuilding != null
				&& DecisionHelper._nearestEnemyBuilding.distanceTo(unit) <= 10.5 && oursNearby >= 2) {
			return true;
		}

		return false;
	}

	private static boolean notTooManySiegedInArea(Unit unit) {
		// return xvr.countUnitsOfGivenTypeInRadius(type, tileRadius, point,
		// onlyMyUnits);
		return true;
	}

	private static boolean notTooManySiegedUnitHere(Unit unit) {
		Unit nearBuilding = xvr.getUnitNearestFromList(unit, xvr.getUnitsBuildings());
		boolean isNearBuilding = nearBuilding != null && nearBuilding.distanceTo(unit) <= 8;

		ChokePoint nearChoke = MapExploration.getNearestChokePointFor(unit);
		boolean isNearChoke = nearChoke != null && unit.distanceToChokePoint(nearChoke) <= 3;

		if (isNearBuilding || isNearChoke) {
			return xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode, 2.7,
					unit, true) <= 2;
		} else {
			return false;
		}
	}

	private static boolean isNeighborhoodSafeToSiege(Unit unit) {
		if (xvr.countUnitsEnemyInRadius(unit, 4) <= 0) {
			return true;
		}
		return false;
	}

	private static boolean canSiegeInThisPlace(Unit unit) {
		ChokePoint nearestChoke = MapExploration.getNearestChokePointFor(unit);

		// Don't siege in the choke point near base, or you'll... lose.
		if (nearestChoke.getRadiusInTiles() <= 3 && unit.distanceToChokePoint(nearestChoke) <= 3) {
			Unit nearestBase = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, unit);
			if (nearestBase != null && nearestBase.distanceTo(unit) <= 20) {
				return false;
			}
		}

		return true;
	}

}

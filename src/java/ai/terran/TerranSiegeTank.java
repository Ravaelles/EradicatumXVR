package ai.terran;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.TankTargeting;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.StrategyManager;
import ai.managers.TechnologyManager;
import ai.managers.units.UnitManager;

public class TerranSiegeTank {

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Siege_Tank_Tank_Mode;

	private static boolean _isUnitWhereItShouldBe;
	private static MapPoint _properPlace;
	private static Unit _nearestEnemy;
	private static Unit _nearestEnemyBuilding;
	private static double _nearestEnemyDist;

	/**
	 * If a tank is in this map it means it is considering unsieging, but we
	 * should wait some time before this happens.
	 */
	private static HashMap<Unit, Integer> unsiegeIdeasMap = new HashMap<>();

	// =====================================================

	public static void act(Unit unit) {
		_properPlace = unit.getProperPlaceToBe();
		updateProperPlaceToBeForTank(unit);
		_isUnitWhereItShouldBe = _properPlace == null || _properPlace.distanceTo(unit) <= 3;
		_nearestEnemy = xvr.getNearestGroundEnemy(unit);
		_nearestEnemyBuilding = MapExploration.getNearestEnemyBuilding(unit);
		_nearestEnemyDist = _nearestEnemy != null ? _nearestEnemy.distanceTo(unit) : -1;

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
			_properPlace = nearestArmyUnit;
		}

		if (StrategyManager.isAnyAttackFormPending()) {
			_properPlace = StrategyManager.getTargetPoint();
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
		if (_nearestEnemyBuilding != null && _nearestEnemyBuilding.distanceTo(unit) <= 10.4) {
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
		if (unsiegeIdeasMap.containsKey(unit)) {
			if (xvr.getTimeSeconds() - unsiegeIdeasMap.get(unit) >= 8) {
				return true;
			}
		}
		return false;
	}

	private static void infoTankIsConsideringUnsieging(Unit unit) {
		if (!unsiegeIdeasMap.containsKey(unit)) {
			unsiegeIdeasMap.put(unit, xvr.getTimeSeconds());
		}
	}

	private static boolean shouldSiege(Unit unit) {
		boolean isEnemyNearShootRange = (_nearestEnemyDist > 0 && _nearestEnemyDist <= (_nearestEnemy
				.getType().isBuilding() ? 10.6 : 13));

		// Check if should siege, based on unit proper place to be (e.g. near
		// the bunker), but consider the neighborhood, if it's safe etc.
		if ((_isUnitWhereItShouldBe && notTooManySiegedInArea(unit)) || isEnemyNearShootRange) {
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
		if (_nearestEnemyBuilding != null && _nearestEnemyBuilding.distanceTo(unit) <= 10.5
				&& oursNearby >= 2) {
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

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(UnitTypes.Terran_Siege_Tank_Siege_Mode)
				+ UnitCounter.getNumberOfUnits(UnitTypes.Terran_Siege_Tank_Tank_Mode);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(UnitTypes.Terran_Siege_Tank_Siege_Mode)
				+ UnitCounter.getNumberOfUnitsCompleted(UnitTypes.Terran_Siege_Tank_Tank_Mode);
	}

	public static boolean isSiegeModeResearched() {
		return TechnologyManager.isSiegeModeResearched();
	}

	public static UnitTypes getUnitType() {
		return unitType;
	}

	public static Collection<Unit> getAllCompletedTanks() {
		ArrayList<Unit> all = new ArrayList<>();
		for (Unit unit : xvr.getBwapi().getMyUnits()) {
			if (unit.getType().isTank()) {
				all.add(unit);
			}
		}
		return all;
	}

}

package ai.terran;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitCounter;
import ai.managers.TechnologyManager;

public class TerranSiegeTank {

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Siege_Tank_Tank_Mode;

	private static boolean _isUnitWhereItShouldBe;
	private static MapPoint _properPlace;
	private static Unit _nearestEnemy;
	private static double _nearestEnemyDist;

	public static void act(Unit unit) {
		_properPlace = unit.getProperPlaceToBe();
		updateProperPlaceToBeForTank(unit);
		_isUnitWhereItShouldBe = _properPlace == null || _properPlace.distanceTo(unit) <= 3.1;
		_nearestEnemy = xvr.getNearestGroundEnemy(unit);
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
	}

	private static void actWhenInNormalMode(Unit unit) {
		if (shouldSiege(unit) && notTooManySiegedUnitHere(unit)) {
			unit.siege();
		}
	}

	private static boolean shouldSiege(Unit unit) {
		if (_isUnitWhereItShouldBe || (_nearestEnemyDist > 0 && _nearestEnemyDist <= 11)) {
			if (canSiegeInThisPlace(unit) && isNeighborhoodSafeToSiege(unit)) {
				return true;
			}
		}

		return false;
	}

	private static boolean notTooManySiegedUnitHere(Unit unit) {
		return xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode, 2.5, unit,
				true) <= 2;
	}

	private static boolean isNeighborhoodSafeToSiege(Unit unit) {
		if (xvr.countUnitsEnemyInRadius(unit, 5) <= 0) {
			return true;
		}
		return false;
	}

	private static boolean canSiegeInThisPlace(Unit unit) {
		ChokePoint nearestChoke = MapExploration.getNearestChokePointFor(unit);
		if (nearestChoke.getRadiusInTiles() <= 3 && unit.distanceToChokePoint(nearestChoke) <= 3) {
			return false;
		}

		if (!unit.isMoving()) {
			return true;
		}

		return true;
	}

	private static void actWhenSieged(Unit unit) {
		// if (!_isUnitWhereItShouldBe
		// || (StrategyManager.isAnyAttackFormPending() && _nearestEnemyDist >
		// 11)
		// || !isNeighborhoodSafeToSiege(unit)) {
		if (!shouldSiege(unit)) {
			unit.unsiege();
		}
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

}

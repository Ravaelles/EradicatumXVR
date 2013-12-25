package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitCounter;
import ai.managers.StrategyManager;
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
		_isUnitWhereItShouldBe = _properPlace == null || _properPlace.distanceTo(unit) <= 2;
		_nearestEnemy = xvr.getNearestEnemy(unit);
		_nearestEnemyDist = _nearestEnemy != null ? _nearestEnemy.distanceTo(unit) : -1;

		if (unit.isSieged()) {
			actWhenSieged(unit);
		} else {
			actWhenInNormalMode(unit);
		}
	}

	private static void actWhenInNormalMode(Unit unit) {
		if (_isUnitWhereItShouldBe || (_nearestEnemyDist > 0 && _nearestEnemyDist <= 11)) {
			unit.siege();
		}
	}

	private static void actWhenSieged(Unit unit) {
		if (!_isUnitWhereItShouldBe
				|| (StrategyManager.isAnyAttackFormPending() && _nearestEnemyDist > 11)) {
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

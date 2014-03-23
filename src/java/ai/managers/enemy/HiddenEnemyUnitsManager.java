package ai.managers.enemy;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.units.UnitActions;
import ai.terran.TerranComsatStation;

public class HiddenEnemyUnitsManager {

	private static XVR xvr = XVR.getInstance();

	public static void act() {
		for (Unit enemy : xvr.getBwapi().getEnemyUnits()) {
			if (!enemy.isDetected()) {
				undetectedEnemyUnitKnown(enemy);
			}
		}
	}

	private static void undetectedEnemyUnitKnown(Unit unit) {
		Unit ourNearestBuildingToThisUnit = xvr.getUnitNearestFromList(unit,
				xvr.getUnitsBuildings());
		if (ourNearestBuildingToThisUnit == null) {
			return;
		}
		boolean isNearBuilding = unit.distanceTo(ourNearestBuildingToThisUnit) <= 5;
		boolean isNearGroupOfUnits = xvr.countUnitsOursInRadius(unit, 7) >= 2;

		UnitType type = unit.getType();
		boolean isSpecialUnit = type.isLurker() || type.isGhost() || type.isFlyer();
		if (ourNearestBuildingToThisUnit == null || isNearBuilding || isNearGroupOfUnits
				|| isSpecialUnit) {
			TerranComsatStation.hiddenUnitDetected(unit);
		}

	}

	public static void avoidHiddenUnitsIfNecessary(Unit unit) {
		Unit hiddenEnemyUnitNearby = MapExploration.getHiddenEnemyUnitNearbyTo(unit);
		if (hiddenEnemyUnitNearby != null && unit.isDetected()
				&& !hiddenEnemyUnitNearby.isDetected()
				&& shouldAvoidThisHiddenEnemyUnit(unit, hiddenEnemyUnitNearby)) {
			UnitActions.moveAwayFromUnit(unit, hiddenEnemyUnitNearby);
			unit.setAiOrder("Avoid hidden unit");
		}
	}

	private static boolean shouldAvoidThisHiddenEnemyUnit(Unit ourUnit, Unit enemyUnit) {
		return enemyUnit.canAttack(ourUnit);
	}

}

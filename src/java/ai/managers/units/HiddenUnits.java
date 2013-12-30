package ai.managers.units;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.terran.TerranComsatStation;

public class HiddenUnits {

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
		boolean isNearBuilding = ourNearestBuildingToThisUnit.distanceTo(unit) <= 5;
		boolean isNearGroupOfUnits = xvr.countUnitsOursInRadius(unit, 7) >= 2;
		if (ourNearestBuildingToThisUnit == null || isNearBuilding || isNearGroupOfUnits) {
			TerranComsatStation.hiddenUnitDetected(unit);
		}

	}
}

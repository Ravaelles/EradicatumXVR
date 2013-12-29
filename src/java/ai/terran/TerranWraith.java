package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.TechnologyManager;

public class TerranWraith {

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Siege_Tank_Tank_Mode;

	public static void act(Unit unit) {

		// NOT cloaked.
		if (!unit.isCloaked()) {
			if (unit.getEnergy() > 10 && TechnologyManager.isWraithCloakingFieldResearched()) {
				unit.cloak();
				UnitActions.useTech(unit, TechnologyManager.CLOAKING_FIELD);
			}
		}

		// CLOAKED.
		else {
			double nearestEnemyDist = xvr.getNearestEnemyDistance(unit, true, true);
			boolean isEnemyNear = nearestEnemyDist < -0.1 && nearestEnemyDist > 11;
			boolean isDetectedByEnemy = unit.isDetected();
			if (isEnemyNear || isDetectedByEnemy) {
				unit.decloak();
			}
		}
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(unitType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(unitType);
	}

	public static UnitTypes getUnitType() {
		return unitType;
	}

	public static void setUnitType(UnitTypes unitType) {
		TerranWraith.unitType = unitType;
	}

}

package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.economy.TechnologyManager;

public class TerranScienceVessel {

	// private static final int MIN_DIST_BETWEEN_VESSELS = 6;
	//
	private static final UnitTypes unitType = UnitTypes.Terran_Science_Vessel;

	private static XVR xvr = XVR.getInstance();

	// ====================================

	public static void act(Unit unit) {
		if (tryUsingDefensiveMatrix(unit)) {
			return;
		}
	}

	private static boolean tryUsingDefensiveMatrix(Unit unit) {
		if (unit.getEnergy() >= 70) {
			Unit useOn = TerranSiegeTank.getFrontTank();
			if (useOn != null) {
				UnitActions.useTech(unit, TechnologyManager.DEFENSIVE_MATRIX, useOn);
			}
		}
		return false;
	}

	// =========================================================

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(unitType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(unitType);
	}

}

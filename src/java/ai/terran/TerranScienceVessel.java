package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.handling.units.UnitCounter;

public class TerranScienceVessel {

	// private static final int MIN_DIST_BETWEEN_VESSELS = 6;
	//
	private static final UnitTypes unitType = UnitTypes.Terran_Science_Vessel;

	// private static XVR xvr = XVR.getInstance();

	// ====================================

	public static void act(Unit unit) {

	}

	// =========================================================

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(unitType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(unitType);
	}

}

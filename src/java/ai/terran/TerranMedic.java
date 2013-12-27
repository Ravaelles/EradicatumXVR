package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;

public class TerranMedic {

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Medic;

	public static UnitTypes getUnitType() {
		return unitType;
	}

	public static void act(Unit unit) {
		Unit goTo = null;
		Unit marine = xvr.getUnitOfTypeNearestTo(UnitTypes.Terran_Marine, unit);
		if (marine != null) {
			goTo = marine;
		}

		// If there's someone to protect, go there
		if (goTo != null) {
			double distance = goTo.distanceTo(unit);

			// If distance is big, just go
			if (distance > 3) {
				UnitActions.moveTo(unit, goTo);
			} else {
				UnitActions.moveTo(unit, goTo);
			}
		}

		// ==============================
		// Manually check for units to heal
		ArrayList<Unit> possibleToHeal = xvr
				.getUnitsInRadius(unit, 6, xvr.getUnitsPossibleToHeal());
		for (Unit otherUnit : possibleToHeal) {
			if (otherUnit.isWounded()) {
				xvr.getBwapi().rightClick(unit, otherUnit);
				return;
			}
		}
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(unitType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(unitType);
	}

}

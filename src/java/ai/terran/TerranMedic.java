package ai.terran;

import java.util.ArrayList;
import java.util.Collection;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;

public class TerranMedic {

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Medic;

	public static UnitTypes getUnitType() {
		return unitType;
	}

	// =========================================================

	public static void act(Unit unit) {
		MapPoint goTo = null;
		MapPoint unitPlace = getNearestInfantryPreferablyOutsideBunker(unit);
		if (unitPlace != null) {
			goTo = unitPlace;
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
		ArrayList<Unit> possibleToHeal = xvr.getUnitsInRadius(unit, 50,
				xvr.getUnitsPossibleToHeal());
		for (Unit otherUnit : possibleToHeal) {
			if (otherUnit.isWounded()) {
				xvr.getBwapi().rightClick(unit, otherUnit);
				return;
			}
		}
	}

	// =========================================================

	private static MapPoint getNearestInfantryPreferablyOutsideBunker(Unit unit) {

		// Define list of all infantry units that we could possibly follow
		Collection<Unit> allInfantry = xvr.getUnitsOurOfTypes(UnitTypes.Terran_Marine,
				UnitTypes.Terran_Firebat);
		ArrayList<Unit> nearestInfantry = xvr.getUnitsInRadius(unit, 300, allInfantry);

		// Try to go there, where's a marine/firebat not in a bunker
		for (Unit infantry : nearestInfantry) {
			if (infantry.isCompleted() && !infantry.isLoaded()) {
				return infantry;
			}
		}

		// Units in bunkers will do fine...
		for (Unit infantry : nearestInfantry) {
			return infantry;
		}

		return TerranCommandCenter.getSecondBaseLocation();
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(unitType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(unitType);
	}

}

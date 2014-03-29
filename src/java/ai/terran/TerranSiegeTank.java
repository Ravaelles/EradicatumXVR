package ai.terran;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.economy.TechnologyManager;
import ai.utils.RUtilities;

public class TerranSiegeTank {

	private static XVR xvr = XVR.getInstance();
	private static UnitTypes unitType = UnitTypes.Terran_Siege_Tank_Tank_Mode;
	private static Unit medianTank = null;
	private static Unit frontTank = null;

	// =====================================================

	public static void recalculateMedianAndFrontTanks() {
		defineMedianTank();
		defineFrontTank();
	}

	/**
	 * Define tank that is nearest to enemy. Nearest in terms of longest
	 * distance to the first base.
	 */
	private static void defineFrontTank() {
		if (getNumberOfUnitsCompleted() == 0) {
			frontTank = null;
		} else {
			Map<Unit, Double> distancesToBase = new TreeMap<>();
			for (Unit tank : getAllCompletedTanks()) {
				distancesToBase.put(tank, tank.distanceTo(xvr.getFirstBase()));
			}
			distancesToBase = RUtilities.sortByValue(distancesToBase, false);

			frontTank = (Unit) RUtilities.getFirstMapElement(distancesToBase);
		}
	}

	/**
	 * Define tank that subjectively located "in the middle of other tanks".
	 * This way we can determine where the center of our Panzerdivision is.
	 */
	private static void defineMedianTank() {
		if (getNumberOfUnitsCompleted() == 0) {
			medianTank = null;
		} else {
			ArrayList<Integer> xCoordinates = new ArrayList<Integer>();
			ArrayList<Integer> yCoordinates = new ArrayList<Integer>();
			java.util.Collections.sort(xCoordinates);
			java.util.Collections.sort(yCoordinates);
			for (Unit tank : getAllCompletedTanks()) {
				xCoordinates.add(tank.getTx());
				yCoordinates.add(tank.getTy());
			}

			int middleIndex = xCoordinates.size() / 2;
			MapPointInstance medianPoint = new MapPointInstance(xCoordinates.get(middleIndex),
					yCoordinates.get(middleIndex));

			medianTank = xvr.getUnitNearestFromList(medianPoint, getAllCompletedTanks());
		}
	}

	public static Unit getMedianTank() {
		return medianTank;
	}

	public static Unit getFrontTank() {
		return frontTank;
	}

	// =====================================================

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

	public static boolean hasAnyTank() {
		return getNumberOfUnitsCompleted() > 0;
	}

}

package ai.handling.units;

import java.util.HashMap;
import java.util.Set;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.terran.TerranSupplyDepot;

public class UnitCounter {

	private static HashMap<Integer, Integer> numberOfUnits = new HashMap<Integer, Integer>();
	private static XVR xvr = XVR.getInstance();

	// public static HashMap<Integer, Integer> getNumberOfUnits() {
	// return numberOfUnits;
	// }

	public static void recalculateUnits() {
		resetUnits();
		countUnits();
	}

	private static void resetUnits() {
		numberOfUnits.clear();
	}

	private static void countUnits() {
		for (Unit unit : xvr.getBwapi().getMyUnits()) {
			if (!unit.isExists()) {
				continue;
			}
			numberOfUnits.put(
					unit.getTypeID(),
					(numberOfUnits.containsKey(unit.getTypeID()) ? numberOfUnits.get(unit
							.getTypeID()) + 1 : 1));
		}
	}

	public static int getNumberOfUnits(UnitTypes type) {
		return numberOfUnits.containsKey(type.ordinal()) ? numberOfUnits.get(type.ordinal()) : 0;
	}

	public static Set<Integer> getExistingUnitTypes() {
		return numberOfUnits.keySet();
	}

	public static int getNumberOfUnitsCompleted(UnitTypes type) {
		int result = 0;
		for (Unit unit : xvr.getUnitsOfType(type)) {
			if (unit.isCompleted()) {
				result++;
			}
		}
		return result;
	}

	public static int getNumberOfBattleUnits() {
		return getNumberOfInfantryUnits() + +getNumberOfVehicleUnits() + getNumberOfShipUnits();
	}

	public static int getNumberOfVehicleUnits() {
		return getNumberOfUnits(UnitTypes.Terran_Vulture)
				+ getNumberOfUnits(UnitTypes.Terran_Siege_Tank_Siege_Mode)
				+ getNumberOfUnits(UnitTypes.Terran_Siege_Tank_Tank_Mode)
				+ getNumberOfUnits(UnitTypes.Terran_Goliath);
	}

	public static int getNumberOfVehicleUnitsCompleted() {
		return getNumberOfUnitsCompleted(UnitTypes.Terran_Vulture)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Siege_Tank_Siege_Mode)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Siege_Tank_Tank_Mode)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Goliath);
	}

	public static int getNumberOfShipUnits() {
		return getNumberOfUnits(UnitTypes.Terran_Dropship)
				+ getNumberOfUnits(UnitTypes.Terran_Wraith)
				+ getNumberOfUnits(UnitTypes.Terran_Valkyrie)
				+ getNumberOfUnits(UnitTypes.Terran_Science_Vessel)
				+ getNumberOfUnits(UnitTypes.Terran_Battlecruiser);
	}

	public static int getNumberOfShipUnitsCompleted() {
		return getNumberOfUnitsCompleted(UnitTypes.Terran_Dropship)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Wraith)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Valkyrie)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Science_Vessel)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Battlecruiser);
	}

	public static int getNumberOfBattleUnitsCompleted() {
		return getNumberOfInfantryUnitsCompleted() + +getNumberOfVehicleUnitsCompleted()
				+ getNumberOfShipUnitsCompleted();
	}

	public static boolean weHaveBuilding(UnitTypes unitType) {
		return getNumberOfUnits(unitType) > 0;
	}

	public static boolean weHaveBuildingFinished(UnitTypes unitType) {
		return getNumberOfUnitsCompleted(unitType) > 0;
	}

	public static int getNumberOfInfantryUnits() {
		return getNumberOfUnits(UnitTypes.Terran_Marine)
				+ getNumberOfUnits(UnitTypes.Terran_Firebat)
				+ getNumberOfUnits(UnitTypes.Terran_Ghost)
				+ getNumberOfUnits(UnitTypes.Terran_Medic);
	}

	public static int getNumberOfInfantryUnitsCompleted() {
		return getNumberOfUnitsCompleted(UnitTypes.Terran_Marine)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Firebat)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Ghost)
				+ getNumberOfUnitsCompleted(UnitTypes.Terran_Medic);
	}

	public static boolean weHaveSupplyDepotFinished() {
		return getNumberOfUnitsCompleted(TerranSupplyDepot.getBuildingType()) > 0;
	}

	public static boolean weHaveSupplyDepot() {
		return getNumberOfUnits(TerranSupplyDepot.getBuildingType()) > 0;
	}

	public static int countAirUnitsNonValkyrie() {
		return getNumberOfShipUnits() - getNumberOfUnits(UnitTypes.Terran_Valkyrie);
	}

	public static int getNumberOfSupplyDepots() {
		return getNumberOfUnits(TerranSupplyDepot.getBuildingType());
	}

	public static int getNumberOfSupplyDepotsCompleted() {
		return getNumberOfUnitsCompleted(TerranSupplyDepot.getBuildingType());
	}

}

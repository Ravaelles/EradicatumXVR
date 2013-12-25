package ai.terran;

import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.AddOn;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;

public class TerranComsatStation {

	private static final UnitTypes buildingType = UnitTypes.Terran_Comsat_Station;
	private static XVR xvr = XVR.getInstance();

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			Constructing.constructAddOn(
					AddOn.getBuildingWithNoAddOn(UnitTypes.Terran_Command_Center),
					buildingType);
			return;
		}
		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	public static boolean shouldBuild() {
		if (UnitCounter.weHaveBuilding(TerranAcademy.getBuildingType())) {
			int bases = TerranCommandCenter.getNumberOfUnitsCompleted();
			int comsats = getNumberOfUnits();

			boolean shouldBuild = bases > comsats;
			if (shouldBuild) {
				return true;
			}
		}
		return false;
	}

	public static Unit getOneNotBusy() {
		for (Unit unit : xvr.getUnitsOfType(buildingType)) {
			if (unit.isBuildingNotBusy()) {
				return unit;
			}
		}
		return null;
	}

	public static UnitTypes getBuildingType() {
		return buildingType;
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(buildingType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(buildingType);
	}

}

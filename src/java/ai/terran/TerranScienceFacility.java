package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;

public class TerranScienceFacility {

	private static final UnitTypes buildingType = UnitTypes.Terran_Science_Facility;
	private static XVR xvr = XVR.getInstance();

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			Constructing.construct(buildingType);
		}
	}

	// =========================================================

	public static boolean shouldBuild() {
		// int starports =
		// UnitCounter.getNumberOfUnits(TerranStarport.getBuildingType());
		// int scienceFacilities = getNumberOfUnits();
		// boolean weAreBuilding = Constructing.weAreBuilding(buildingType);
		//
		// if (scienceFacilities == 0 && starports >= 2 && !weAreBuilding) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		//
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		// return false;
		return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	// =========================================================

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

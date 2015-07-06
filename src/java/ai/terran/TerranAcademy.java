package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;

public class TerranAcademy {

	private static final UnitTypes buildingType = UnitTypes.Terran_Academy;
	private static XVR xvr = XVR.getInstance();

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			Constructing.construct(xvr, buildingType);
		}
		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	public static boolean shouldBuild() {
		boolean weAreBuilding = Constructing.weAreBuilding(buildingType);
		if (weAreBuilding) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		int academies = getNumberOfUnits();
		if (academies == 0 && xvr.getTimeSeconds() >= 275) {
			int barracks = TerranBarracks.getNumberOfUnitsCompleted();

			if (barracks >= TerranBarracks.MAX_BARRACKS && !weAreBuilding
					&& UnitCounter.getNumberOfBattleUnits() >= 5) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			}

			if (TerranRefinery.getNumberOfUnitsCompleted() == 1
					|| TerranFactory.getNumberOfUnits() == 1) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			}
		}

		return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
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

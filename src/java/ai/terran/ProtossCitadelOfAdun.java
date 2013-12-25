package ai.terran;

import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.BotStrategyManager;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;

public class ProtossCitadelOfAdun {

	private static final UnitTypes buildingType = UnitTypes.Protoss_Citadel_of_Adun;
	private static XVR xvr = XVR.getInstance();

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			Constructing.construct(xvr, buildingType);
		}
		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	public static boolean shouldBuild() {
		if (!UnitCounter.weHaveBuilding(buildingType)
				&& UnitCounter
						.weHaveBuilding(UnitTypes.Protoss_Cybernetics_Core)
				&& UnitCounter.getNumberOfUnits(TerranBarracks
						.getBuildingType()) >= 3
				&& !Constructing.weAreBuilding(buildingType)) {
			if (BotStrategyManager.isExpandWithBunkers()) {
				if (UnitCounter.getNumberOfBattleUnits() >= 2) {
					ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
					return true;
				}
			} else {
				if (UnitCounter.getNumberOfBattleUnits() >= TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING
						|| xvr.getTimeSeconds() > 800) {
					ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
					return true;
				}
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

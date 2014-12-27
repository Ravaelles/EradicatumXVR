package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.economy.TechnologyManager;

public class TerranArmory {

	private static final UnitTypes buildingType = UnitTypes.Terran_Armory;
	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static boolean shouldBuild() {
		int factories = UnitCounter.getNumberOfUnits(TerranFactory.getBuildingType());
		int armories = getNumberOfUnits();
		boolean weAreBuilding = Constructing.weAreBuilding(buildingType);

		if (armories == 0
				&& (factories >= 2 && !weAreBuilding
						&& !TechnologyManager.isSiegeModeResearchPossible() || TerranFactory.FORCE_GOLIATHS_INSTEAD_VULTURES)) {
			if (UnitCounter.getNumberOfBattleUnits() >= 12
					|| TerranFactory.FORCE_GOLIATHS_INSTEAD_VULTURES) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		return false;
	}

	// =========================================================

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			Constructing.construct(xvr, buildingType);
		}
		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
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

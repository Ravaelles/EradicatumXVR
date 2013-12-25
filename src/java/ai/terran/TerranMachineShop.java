package ai.terran;

import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.AddOn;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;

public class TerranMachineShop {

	private static final UnitTypes buildingType = UnitTypes.Terran_Machine_Shop;
	private static XVR xvr = XVR.getInstance();

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			Constructing.constructAddOn(
					AddOn.getBuildingWithNoAddOn(TerranFactory.getBuildingType()),
					buildingType);
			return;
		}
		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	public static boolean shouldBuild() {
		if (UnitCounter.weHaveBuilding(TerranFactory.getBuildingType())) {
			int factories = TerranFactory.getNumberOfUnitsCompleted();
			int addOns = getNumberOfUnits();

			boolean shouldBuild = factories > addOns;
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

package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.AddOn;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;

public class TerranMachineShop {

	private static final UnitTypes buildingType = UnitTypes.Terran_Machine_Shop;
	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static boolean shouldBuild() {
		// =========================================================
		// Begin EASY-WAY

		int factories = TerranFactory.getNumberOfUnitsCompleted();
		if (factories < 2) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		int vultures = TerranVulture.getNumberOfUnits();
		if (vultures <= TerranVulture.CRITICALLY_FEW_VULTURES
				&& TerranFactory.getOneNotBusy() == null) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		// End EASY-WAY
		// =========================================================

		if (factories > 0) {
			int mashineShops = getNumberOfUnits();

			if (xvr.canAfford(50, 50) && factories > mashineShops
					&& TerranFactory.getOneNotBusy() != null) {
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
			Constructing.constructAddOn(
					AddOn.getBuildingWithNoAddOn(TerranFactory.getBuildingType()), buildingType);
			return;
		}
		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	// =========================================================

	public static Unit getOneNotBusy() {
		for (Unit unit : xvr.getUnitsOfType(buildingType)) {
			if (unit.isCompleted() && unit.isBuildingNotBusy()) {
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

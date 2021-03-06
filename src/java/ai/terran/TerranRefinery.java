package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.units.UnitManager;

public class TerranRefinery {

	private static XVR xvr = XVR.getInstance();

	private static final UnitTypes buildingType = UnitTypes.Terran_Refinery;

	// =========================================================

	public static boolean shouldBuild() {
		int supplyUsed = xvr.getSuppliesUsed();
		// int minGateways = BotStrategyManager.isExpandWithBunkers() ? 3 : 4;
		int barracks = UnitCounter.getNumberOfUnits(UnitManager.BARRACKS);
		int refineries = UnitCounter.getNumberOfUnits(buildingType);
		int battleUnits = UnitCounter.getNumberOfBattleUnits();
		boolean weHaveAcademy = UnitCounter.weHaveBuilding(TerranAcademy.getBuildingType());

		// =========================================================
		// Begin EASY-WAY

		// If no bases are left, just quit.
		if (TerranCommandCenter.getNumberOfUnits() == 0) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		int minSupply = 14;
		if (xvr.isEnemyProtoss()) {
			minSupply += 1;
		}

		if (refineries == 0 && (supplyUsed >= minSupply || xvr.canAfford(270))) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (refineries == 0 && TerranFactory.ONLY_TANKS && supplyUsed > 9) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		// End EASY-WAY
		// =========================================================

		// if (refineries == 0) {
		// boolean isEnoughInfantry = (battleUnits >= 6 || battleUnits >=
		// ArmyCreationManager.MINIMUM_MARINES);
		// boolean isAnotherBaseAndFreeMinerals =
		// TerranCommandCenter.getNumberOfUnits() > 1
		// || xvr.canAfford(468)
		// || (TerranBarracks.getNumberOfUnitsCompleted() == 0
		// && TerranBarracks.getNumberOfUnits() > 0 && xvr.canAfford(134));
		// if (isEnoughInfantry || isAnotherBaseAndFreeMinerals) {
		// return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// }
		// }

		if (UnitCounter.getNumberOfUnitsCompleted(TerranEngineeringBay.getBuildingType()) == 0
				&& UnitCounter.getNumberOfUnits(TerranBunker.getBuildingType()) == 0) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		if (!Constructing.weAreBuilding(buildingType)
				&& UnitCounter.getNumberOfUnits(buildingType) < UnitCounter
						.getNumberOfUnitsCompleted(UnitManager.BASE) && weHaveAcademy) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (!Constructing.weAreBuilding(buildingType)
				&& (weHaveAcademy || xvr.canAfford(200))
				&& UnitCounter.getNumberOfUnits(buildingType) < UnitCounter
						.getNumberOfUnitsCompleted(UnitManager.BASE)) {
			if (battleUnits >= TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			}
		}

		if (UnitCounter.getNumberOfUnits(buildingType) < UnitCounter
				.getNumberOfUnitsCompleted(UnitManager.BASE) && xvr.canAfford(750)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	// =========================================================

	public static MapPoint findTileForRefinery() {
		Unit nearestGeyser = xvr.getUnitNearestFromList(xvr.getFirstBase(), xvr.getGeysersUnits());
		if (nearestGeyser != null
				&& xvr.getUnitsOfGivenTypeInRadius(UnitManager.BASE, 15, nearestGeyser, true)
						.isEmpty()) {
			return null;
		}

		if (nearestGeyser != null) {
			// return new MapPointInstance(nearestGeyser.getX(),
			// nearestGeyser.getY());
			return new MapPointInstance(nearestGeyser.getX() - 64, nearestGeyser.getY() - 32);
		} else {
			return null;
		}
	}

	// =========================================================

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			Constructing.construct(buildingType);
		}
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

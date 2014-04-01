package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.strategy.BotStrategyManager;
import ai.managers.units.UnitManager;
import ai.managers.units.army.ArmyCreationManager;

public class TerranRefinery {

	private static XVR xvr = XVR.getInstance();

	private static final UnitTypes buildingType = UnitTypes.Terran_Refinery;

	// =========================================================

	public static boolean shouldBuild() {
		int minGateways = BotStrategyManager.isExpandWithBunkers() ? 3 : 4;
		int barracks = UnitCounter.getNumberOfUnits(UnitManager.BARRACKS);
		int refineries = UnitCounter.getNumberOfUnits(buildingType);
		int battleUnits = UnitCounter.getNumberOfBattleUnits();
		boolean weHaveAcademy = UnitCounter.weHaveBuilding(TerranAcademy.getBuildingType());

		if (refineries == 0) {
			boolean isEnoughInfantry = (battleUnits >= 6 || battleUnits >= ArmyCreationManager.MINIMUM_MARINES);
			boolean isAnotherBaseAndFreeMinerals = TerranCommandCenter.getNumberOfUnits() > 1
					|| xvr.canAfford(468)
					|| (TerranBarracks.getNumberOfUnitsCompleted() == 0
							&& TerranBarracks.getNumberOfUnits() > 0 && xvr.canAfford(134));
			if (isEnoughInfantry || isAnotherBaseAndFreeMinerals) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			}
		}

		if (UnitCounter.getNumberOfUnitsCompleted(TerranEngineeringBay.getBuildingType()) == 0
				&& UnitCounter.getNumberOfUnits(TerranBunker.getBuildingType()) == 0) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			return false;
		}

		if (!Constructing.weAreBuilding(buildingType)
				&& UnitCounter.getNumberOfUnits(buildingType) < UnitCounter
						.getNumberOfUnitsCompleted(UnitManager.BASE) && weHaveAcademy) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			return true;
		}

		if (!Constructing.weAreBuilding(buildingType)
				&& (weHaveAcademy || barracks >= minGateways || xvr.canAfford(700))
				&& UnitCounter.getNumberOfUnits(buildingType) < UnitCounter
						.getNumberOfUnitsCompleted(UnitManager.BASE)) {
			if (battleUnits >= TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		if (UnitCounter.getNumberOfUnits(buildingType) < UnitCounter
				.getNumberOfUnitsCompleted(UnitManager.BASE) && xvr.canAfford(750)) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			return true;
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

	public static UnitTypes getBuildingType() {
		return buildingType;
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(buildingType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(buildingType);
	}

	public static MapPoint findTileForRefinery() {
		Unit nearestGeyser = xvr.getUnitNearestFromList(xvr.getFirstBase(), xvr.getGeysersUnits());
		if (nearestGeyser != null
				&& xvr.getUnitsOfGivenTypeInRadius(UnitManager.BASE, 15, nearestGeyser, true)
						.isEmpty()) {
			return null;
		}

		if (nearestGeyser != null) {
			return new MapPointInstance(nearestGeyser.getX() - 64, nearestGeyser.getY() - 32);
		} else {
			return null;
		}
	}

}

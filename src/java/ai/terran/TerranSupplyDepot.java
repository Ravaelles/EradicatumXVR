package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.strategy.BotStrategyManager;
import ai.managers.units.UnitManager;

public class TerranSupplyDepot {

	// private static int INITIAL_DEPOT_MIN_DIST_FROM_BASE = 6;
	// private static int INITIAL_DEPOT_MAX_DIST_FROM_BASE = 18;
	// private static int DEPOT_FROM_DEPOT_MIN_DISTANCE = 0;
	// private static int DEPOT_FROM_DEPOT_MAX_DISTANCE = 15;

	private static final UnitTypes buildingType = UnitTypes.Terran_Supply_Depot;
	private static XVR xvr = XVR.getInstance();

	// =========================================================
	// Should build?

	public static boolean shouldBuild() {
		boolean weAreBuilding = Constructing.weAreBuilding(buildingType);

		int free = xvr.getSuppliesFree();
		int total = xvr.getSuppliesTotal();
		int depots = UnitCounter.getNumberOfUnits(buildingType);
		int barracks = TerranBarracks.getNumberOfUnits();
		int workers = UnitCounter.getNumberOfUnits(UnitManager.WORKER);
		int engineeringBays = TerranEngineeringBay.getNumberOfUnits();

		if (xvr.getSuppliesFree() >= 18 && getNumberOfUnits() != getNumberOfUnitsCompleted()) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		// =========================================================
		// ANTI ZERG RUSH
		if (xvr.isEnemyZerg()) {

			// First build barracks
			if (barracks == 0) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			}

			// Then bunker
			if (TerranBunker.getNumberOfUnits() == 0
					&& !Constructing.weAreBuilding(TerranBunker.getBuildingType())
					&& !xvr.canAfford(200)) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			}

			if (barracks >= 1 && depots == 0 && xvr.canAfford(200)) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			}

			if ((depots >= 1 || weAreBuilding) && TerranBunker.getNumberOfUnits() == 0) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			}
		}

		// =========================================================
		// Begin EASY-WAY

		if (depots == 0) {
			if (xvr.isEnemyProtoss()) {
				if (TerranBarracks.getNumberOfUnits() <= 0 && !xvr.canAfford(250)) {
					return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
				}
			}

			if (xvr.canAfford(92)) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			} else {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			}
		}

		// End EASY-WAY
		// =========================================================

		if (total > 8 && total < 200) {
			if (free <= 4 || (total >= 39 && free <= 18) || (total >= 65 && free <= 26)) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			}
		}

		if (TerranBunker.getNumberOfUnits() < TerranBunker.GLOBAL_MAX_BUNKERS
				&& TerranBunker.shouldBuild() && !xvr.canAfford(200)) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		if (depots == 0 && xvr.canAfford(90)) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			return true;
		}

		if (barracks == 0 && depots == 1) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			return false;
		}
		if (barracks == 1 && depots == 1) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			return false;
		}

		// ### VERSION ### Expansion with cannons
		if (BotStrategyManager.isExpandWithBunkers()) {
			if (depots == 0 && (workers >= 9 || xvr.canAfford(92))) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		if (BotStrategyManager.isExpandWithBunkers()) {
			if (depots == 1
					&& ((engineeringBays == 1 && xvr.canAfford(54)) || (engineeringBays == 0 && xvr
							.canAfford(194)))) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		} else {
			if (depots == 1
					&& ((engineeringBays == 1 && xvr.canAfford(92)) || (engineeringBays == 0 && xvr
							.canAfford(216)))) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		if (total == 200) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			return false;
		}

		if (total < 80 && Constructing.weAreBuilding(buildingType)) {
			if (!(total >= 10 && total <= 20 && free == 0)) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
				return false;
			}
		}

		boolean shouldBuild = ((depots == 0 && total <= 9 && free <= 3)
				|| (total >= 10 && total <= 17 && free <= 4 && depots <= 1)
				|| (total >= 18 && total <= 25 && free <= 5)
				|| (total > 25 && total <= 45 && free <= 8) || (total > 45 && free <= 10) || (total > 90
				&& total < 200 && free <= 20));

		ShouldBuildCache.cacheShouldBuildInfo(buildingType, shouldBuild);
		return shouldBuild;
	}

	// =========================================================

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			Constructing.construct(buildingType);
		}
	}

	public static double calculateExistingDepotsStrength() {
		double result = 0;

		for (Unit pylon : xvr.getUnitsOfType(buildingType)) {
			result += (double) pylon.getHP() / 500;
		}

		return result;
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

package ai.terran;

import java.util.ArrayList;
import java.util.HashMap;

import jnibwapi.model.BaseLocation;
import jnibwapi.model.Map;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.strategy.BotStrategyManager;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitManager;
import ai.managers.units.buildings.BuildingManager;
import ai.managers.units.workers.WorkerManager;
import ai.utils.CodeProfiler;
import ai.utils.RUtilities;

public class TerranCommandCenter {

	private static XVR xvr = XVR.getInstance();

	public static final int MAX_WORKERS = 65;

	private static int MAX_DIST_OF_MINERAL_FROM_BASE = 12;
	private static final int ARMY_UNITS_PER_NEW_BASE = 10;
	private static final int MIN_WORKERS = 19;
	public static final int WORKERS_PER_GEYSER = 4;

	private static MapPoint _secondBase = null;
	private static MapPoint _cachedNextBaseTile = null;
	private static int _lastTimeCalculatedTileForBase = -1;
	public static int _lastFail = -1;

	public static int EXPAND_ONLY_IF_TANKS_MORE_THAN = -1;

	private static final UnitTypes buildingType = UnitTypes.Terran_Command_Center;

	// =========================================================

	public static boolean shouldBuild() {
		int x = 69;
		if (x == 69) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}
		if ((EXPAND_ONLY_IF_TANKS_MORE_THAN > -1 || xvr.canAfford(600))
				&& TerranSiegeTank.getNumberOfUnits() < EXPAND_ONLY_IF_TANKS_MORE_THAN) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		int bases = UnitCounter.getNumberOfUnits(buildingType);

		if (bases >= 2) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		int factories = UnitCounter.getNumberOfUnits(buildingType);
		int battleUnits = UnitCounter.getNumberOfBattleUnits();

		boolean factoryFirstConditionOkay = xvr.canAfford(384)
				|| (!TerranFactory.FORCE_FACTORY_BEFORE_SECOND_BASE || TerranFactory.getNumberOfUnits() > 0);

		// =========================================================
		// Begin EASY-WAY

		if (bases <= 1 && (xvr.getTimeSeconds() >= 290 || xvr.canAfford(350))) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (bases <= 1 && xvr.canAfford(370)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (bases <= 1
				&& (TerranVulture.getNumberOfUnits() >= TerranVulture.CRITICALLY_FEW_VULTURES || xvr.getTimeSeconds() > 380)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (factories < 2 && !xvr.canAfford(450)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		if (factories >= 2 && xvr.canAfford(92)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (bases >= 4) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		// End EASY-WAY
		// =========================================================

		if (bases <= 1 && (battleUnits >= TerranBarracks.CRITICALLY_FEW_INFANTRY || xvr.canAfford(358))
				&& (TerranBunker.getNumberOfUnitsCompleted() >= TerranBunker.GLOBAL_MAX_BUNKERS || xvr.canAfford(384))
				&& factoryFirstConditionOkay) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (xvr.canAfford(550) && battleUnits >= 18) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (xvr.getTimeSeconds() >= 390 && bases <= 1 && !Constructing.weAreBuilding(UnitManager.BASE)
				&& battleUnits >= TerranBarracks.CRITICALLY_FEW_INFANTRY) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (bases >= 2 && battleUnits <= bases * 7 && !xvr.canAfford(420)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		if (battleUnits < StrategyManager.getMinBattleUnits() + 2 && !xvr.canAfford(500)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		if (xvr.canAfford(600)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		// int quickUnitsThreshold = BotStrategyManager.isExpandWithBunkers() ?
		// 4 : 13;
		// if (battleUnits >= quickUnitsThreshold && xvr.canAfford(350) ||
		// xvr.canAfford(500)) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }

		// FORCE quick expansion if we're rich
		// if (xvr.canAfford(380)) {
		// if (barracks >= 2 && battleUnits >= 13 && !XVR.isEnemyTerran()) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		//
		// int thresholdBattleUnits = TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING
		// - 4;
		// if (battleUnits < thresholdBattleUnits || !xvr.canAfford(350)) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		// return false;
		// } else {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		// }
		// if (xvr.canAfford(410)) {
		// if (battleUnits < 11) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		// return false;
		// } else {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		// }

		// // Initially, we must wait to have at least 3 barracks to build first
		// // base.
		if (bases == 1) {
			// if (barracksCompleted <= 2 && !xvr.canAfford(500)) {
			// return ShouldBuildCache.cacheShouldBuildInfo(buildingType,
			// false);
			// }
		}

		// More than one base
		else {

			// But if we already have another base...
			if ((bases * ARMY_UNITS_PER_NEW_BASE > battleUnits && !xvr.canAfford(500))) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			}
		}

		return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			Constructing.construct(buildingType);
		}

		// boolean shouldBuild = shouldBuild();
		//
		// if (shouldBuild) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// Constructing.construct(xvr, buildingType);
		// } else {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		// }

		// =========================================================
		// Ensure base is being built
		// if (xvr.getFrames() % 30 == 0
		// && BuildingManager.getNextBaseBuilder() == null) {
		// Constructing.construct(xvr, buildingType);
		// // System.out.println("Base build fix #69 (builder: "
		// // + BuildingManager.getNextBaseBuilder() + ")");
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// }
	}

	// =========================================================

	public static void act() {
		for (Unit base : xvr.getUnitsOfTypeCompleted(buildingType)) {
			TerranCommandCenter.act(base);
		}
	}

	public static void act(Unit base) {

		// Calculate number of workers at nearby geyser, if there's too many
		// of them, send some of them away
		checkNumberOfGasWorkersAt(base);

		// If we don't have enough workers at our base
		checkNumberOfWorkersOverallAtBase(base);

		// =========================================
		// ======== SSCAI FIX: Remove 0 minerals ===
		checkIfRemoveZeroMineralsCrystal(base);
	}

	// =========================================================

	private static boolean shouldBuildWorkers(Unit base) {
		if (base == null) {
			return false;
		}

		int workers = UnitCounter.getNumberOfUnits(UnitManager.WORKER);

		// =========================================================
		// Prioritize Comsat station
		if (TerranComsatStation.MODE_ASAP && TerranComsatStation.getNumberOfUnits() == 0) {
			if (xvr.canAfford(0, 50)) {
				return false;
			}
		}

		// =========================================================
		// Prioritize quick tanks
		if (TerranFactory.ONLY_TANKS && workers >= 10 && !xvr.canAfford(200)) {
			return false;
		}

		// =========================================================
		// Dont produce workers at bases that are under attack, unless it's the
		// main base
		if (base.getID() != xvr.getFirstBase().getID()) {
			if (xvr.countUnitsEnemyInRadius(base, 15) >= 2) {
				return false;
			}
		}

		// =========================================================

		int depots = UnitCounter.getNumberOfUnits(TerranSupplyDepot.getBuildingType());
		boolean weAreBuildingDepot = Constructing.weAreBuilding(TerranSupplyDepot.getBuildingType());

		// =========================================================
		// ANTI ZERG-RUSH
		if (xvr.isEnemyZerg()) {

			// Barracks
			if (workers >= 7 && TerranBarracks.getNumberOfUnits() == 0
					&& !Constructing.weAreBuilding(TerranBarracks.getBuildingType())) {
				return false;
			}

			// Bunker
			if (workers >= 8 && TerranBunker.getNumberOfUnits() == 0
					&& !Constructing.weAreBuilding(TerranBunker.getBuildingType()) && !xvr.canAfford(150)) {
				return false;
			}
		}

		// =========================================================

		if (workers >= MAX_WORKERS) {
			return false;
		}

		// =========================================================
		// PRIORITIZE FIRST DEPOT

		if (depots == 0 && workers > 7 && (xvr.canAfford(84) && !xvr.canAfford(150)) && TerranSupplyDepot.shouldBuild()) {
			return false;
		}

		// =========================================================
		// PRIORITIZE FIRST FACTORY
		if (TerranFactory.getNumberOfUnits() == 0 && TerranFactory.shouldBuild()
				&& (xvr.canAfford(150) && !xvr.canAfford(250))) {
			return false;
		}

		// =========================================================
		// PRIORITIZE BUNKERS

		if (TerranBunker.getNumberOfUnits() == 0 && TerranBunker.shouldBuild() && !xvr.canAfford(150)) {
			// if (TerranBunker.getNumberOfUnits() <
			// TerranBunker.GLOBAL_MAX_BUNKERS && TerranBunker.shouldBuild()
			// && !xvr.canAfford(150)) {
			return false;
		}

		// =========================================================

		// Quick FIRST DEPOT
		// if (BotStrategyManager.isExpandWithbunkers()) {
		// if (workers == 9 && (depots == 0 || !weAreBuildingDepot) &&
		// !xvr.canAfford(150)) {
		// return false;
		//
		// }
		// }

		// Quick FIRST BUNKER
		if (BotStrategyManager.isExpandWithBunkers()) {
			if (depots == 2) {
				if (!Constructing.weAreBuilding(TerranBunker.getBuildingType()) && !xvr.canAfford(200) && workers >= 20) {
					return false;
				} else {
					return xvr.canAfford(184);
				}
			}
		}

		if (workers < MIN_WORKERS && xvr.canAfford(58)) {
			return true;
		}

		int bases = UnitCounter.getNumberOfUnits(UnitManager.BASE);
		int bunkers = UnitCounter.getNumberOfUnits(TerranBunker.getBuildingType());

		if (workers > bases * 25) {
			return false;
		}

		if (BotStrategyManager.isExpandWithBunkers()) {
			if (workers >= MIN_WORKERS && bunkers < TerranBunker.GLOBAL_MAX_BUNKERS) {
				return false;
			}
		}

		// if (UnitCounter.getNumberOfBattleUnits() < 3 * workers) {
		// return false;
		// }

		int workersNearBase = getNumberOfWorkersNearBase(base);
		double existingToOptimalRatio = (double) workersNearBase / getOptimalMineralGatherersAtBase(base);

		// If we have only one base and already some workers, promote more
		// gateways
		int barracks = UnitCounter.getNumberOfUnits(TerranBarracks.getBuildingType());
		if (UnitCounter.getNumberOfUnits(buildingType) == 1 && barracks < TerranBarracks.MAX_BARRACKS) {
			if (existingToOptimalRatio > 0.5 + 0.12 * barracks) {
				return false;
			}
		}

		// Check if need to build some building. If so then check if we can
		// afford to train worker, we don't want to steal resources.
		int[] shouldBuildAnyBuilding = Constructing.shouldBuildAnyBuilding();
		if (shouldBuildAnyBuilding != null) {

			// If we can't afford extra 50 minerals, disallow building of
			// worker. BUT if we don't have even 40% of mineral gatherers then
			// allow.
			if (!xvr.canAfford(shouldBuildAnyBuilding[0] + 100) && existingToOptimalRatio > 0.4) {
				return false;
			}
		}

		return (existingToOptimalRatio < 1 ? true : false);
	}

	// =========================================================
	// Migrate workers between bases

	private static void trySendingWorkersToOtherBaseFrom(Unit base) {
		double mineralWorkersToOptimalRatio = defineNumberOfWorkersToOptimalNumberRatioFor(base);

		for (Unit otherBase : getBases()) {
			boolean baseReady = (otherBase.isCompleted() || BuildingManager.countConstructionProgress(buildingType) >= 86);

			if (!otherBase.equals(base) && baseReady && otherBase.distanceTo(base) <= 35) {
				double otherBaseRatio = defineNumberOfWorkersToOptimalNumberRatioFor(otherBase);
				// System.out.println("THEY/WE: " + mineralWorkersToOptimalRatio
				// + " / "
				// + otherBaseRatio);
				if (mineralWorkersToOptimalRatio - 0.13 >= otherBaseRatio) {
					if (xvr.getFrames() % 20 == 0) {
						sendOneWorkerFromTo(base, otherBase);
						return;
					}
				}
			}
		}
	}

	// =========================================================
	// Define tile for the next base

	/** Find building tile for new base. */
	public static MapPoint findTileForNextBase(boolean forceNewSolution) {
		if (xvr.getFirstBase() == null || xvr.getSuppliesUsed() < 7) {
			return null;
		}

		int now = xvr.getTimeSeconds();
		if (_lastFail + 8 >= now) {
			return null;
		}

		// Try to get cached value
		boolean isVeryOldSolution = _lastTimeCalculatedTileForBase + 5 <= xvr.getTimeSeconds();
		if (!forceNewSolution && !isVeryOldSolution) {
			return _cachedNextBaseTile;
		}
		// if (_cachedNextBaseTile != null && !forceNewSolution &&
		// !isVeryOldSolution) {
		// return _cachedNextBaseTile;
		// }

		// Make sure you're not calculating base location all the time
		if (forceNewSolution || isVeryOldSolution) {

			if (_lastTimeCalculatedTileForBase != -1 && now - _lastTimeCalculatedTileForBase <= 3) {
				return _cachedNextBaseTile;
			}
			_lastTimeCalculatedTileForBase = now;
		}

		// ===============================
		BaseLocation nearestFreeBaseLocation = getNearestFreeBaseLocation();
		if (nearestFreeBaseLocation != null) {
			MapPoint point = nearestFreeBaseLocation;

			CodeProfiler.startMeasuring("New base");
			_cachedNextBaseTile = Constructing.getLegitTileToBuildNear(buildingType, point, 0, 7);
			CodeProfiler.endMeasuring("New base");
		} else {
			System.out.println("Error! No place for next base!");
			_cachedNextBaseTile = null;
		}

		return _cachedNextBaseTile;
	}

	// =========================================================

	private static void checkNumberOfWorkersOverallAtBase(Unit base) {
		if (shouldBuildWorkers(base)) {
			if (base.getTrainingQueueSize() == 0) {
				xvr.buildUnit(base, UnitManager.WORKER);
			}
		}

		// We have more than optimal number workers at base
		else {
			trySendingWorkersToOtherBaseFrom(base);
		}
	}

	private static void sendOneWorkerFromTo(Unit base, Unit otherBase) {
		Unit chosenWorker = null;
		for (Unit worker : getWorkersNearBase(base)) {
			if (worker.isGatheringMinerals() && !worker.isCarryingMinerals()) {
				chosenWorker = worker;
				break;
			}
		}

		if (chosenWorker != null) {
			UnitActions.moveTo(chosenWorker, otherBase);
		}
	}

	private static double defineNumberOfWorkersToOptimalNumberRatioFor(Unit base) {
		// int gasGatherersForBase = getNumberOfGasGatherersForBase(base);
		// int numRequiredWorkers = WORKERS_PER_GEYSER - gasGatherersForBase;
		int optimalMineralWorkersAtBase = getOptimalMineralGatherersAtBase(base) - WORKERS_PER_GEYSER;
		double mineralWorkersToOptimalRatio = (double) getWorkersNearBase(base).size() / optimalMineralWorkersAtBase;
		return mineralWorkersToOptimalRatio;
	}

	private static void checkNumberOfGasWorkersAt(Unit base) {
		if (!TerranCommandCenter.isExistingCompletedAssimilatorNearBase(base)) {
			return;
		}

		int maxGasGatherers = WORKERS_PER_GEYSER;

		// =========================================================
		// Limit gas gatherers if too many gas or gas not needed
		// if (xvr.canAfford(0, 100)
		// && (TerranVulture.getNumberOfUnits() == 0 || xvr.canAfford(0, 350)))
		// {
		// maxGasGatherers = 1;
		// }

		// =========================================================

		int gasGatherersForBase = getNumberOfGasGatherersForBase(base);
		if (gasGatherersForBase > maxGasGatherers) {
			int overLimitWorkers = gasGatherersForBase - maxGasGatherers;

			// Check whether the geyser isn't depleted
			if (xvr.getUnitOfTypeNearestTo(TerranRefinery.getBuildingType(), base).getResources() < 40) {
				overLimitWorkers = gasGatherersForBase - 1;
			}

			// We can send workers only if there's another base
			if (overLimitWorkers > 0) {
				haveOverLimitGasWorkers(base, overLimitWorkers);
			}
		} else {
			int numRequiredWorkers = maxGasGatherers - gasGatherersForBase;
			int optimalMineralWorkersAtBase = getOptimalMineralGatherersAtBase(base) - maxGasGatherers;
			double mineralWorkersToOptimalRatio = (double) getNumberOfMineralGatherersForBase(base)
					/ optimalMineralWorkersAtBase;
			if (mineralWorkersToOptimalRatio < 0.5) {
				numRequiredWorkers--;
			}
			if (mineralWorkersToOptimalRatio < 0.6) {
				numRequiredWorkers--;
			}
			if (mineralWorkersToOptimalRatio < 0.7) {
				numRequiredWorkers--;
			}
			if (numRequiredWorkers <= 0) {
				numRequiredWorkers = 1;
			}

			ArrayList<Unit> gatherers = getMineralWorkersNearBase(base);
			for (int i = 0; i < numRequiredWorkers && i < gatherers.size(); i++) {
				Unit gathererToAssign = gatherers.get(i);
				WorkerManager.gatherResources(gathererToAssign, base);
			}
		}
	}

	private static void haveOverLimitGasWorkers(Unit base, int overLimitWorkers) {
		ArrayList<Unit> gatherers = getGasWorkersNearBase(base);
		// for (int i = 0; i < overLimitWorkers && i < gatherers.size();
		// i++) {
		// UnitActions.moveTo(gatherers.get(i), base);
		// }
		// ArrayList<Unit> mineralsInNeihgborhood =
		// xvr.getUnitsOfGivenTypeInRadius(
		// UnitTypes.Resource_Mineral_Field, 25, base, false);
		int counter = 0;
		if (!gatherers.isEmpty()) {
			for (Unit worker : gatherers) {
				// WorkerManager
				// .forceGatherMinerals(
				// worker,
				// (Unit) RUtilities
				// .getRandomListElement(mineralsInNeihgborhood));
				WorkerManager.gatherMinerals(worker, base);

				if (counter++ >= overLimitWorkers) {
					break;
				}
			}
		}
	}

	private static void checkIfRemoveZeroMineralsCrystal(Unit base) {
		final int SEARCH_IN_RADIUS = 30;
		final int ACT_IF_AT_LEAST_N_WORKERS = 20;

		// Create list of mineral gatheres near base. It's essential not to use
		// defined method, as we need to significantly increase seek range.
		if (UnitCounter.getNumberOfUnits(UnitManager.WORKER) < ACT_IF_AT_LEAST_N_WORKERS) {
			return;
		}

		ArrayList<Unit> workers = xvr.getWorkers();
		ArrayList<Unit> mineralWorkersNearBase = new ArrayList<>();
		for (Unit worker : workers) {
			if (worker.isGatheringMinerals() && !worker.isConstructing()
					&& xvr.getDistanceBetween(base, worker) <= SEARCH_IN_RADIUS) {
				mineralWorkersNearBase.add(worker);
			}
		}

		// If we have at least X mineral workers near base, send one of them to
		// gather this lonely mineral-obstacle
		if (mineralWorkersNearBase.size() >= ACT_IF_AT_LEAST_N_WORKERS) {
			ArrayList<Unit> mineralsAroundTheBase = xvr.getUnitsOfGivenTypeInRadius(UnitTypes.Resource_Mineral_Field,
					SEARCH_IN_RADIUS, base, false);

			for (Unit mineral : mineralsAroundTheBase) {
				if (mineral.getResources() == 0 && !mineral.isBeingGathered()) {
					int max = 1;
					Unit unitToUse = mineralWorkersNearBase.get(RUtilities.rand(0, max));
					WorkerManager.forceGatherMinerals(unitToUse, mineral);
				}
			}
		}
	}

	private static BaseLocation getNearestFreeBaseLocation() {
		Unit expansionCenter = xvr.getFirstBase();
		// if (!xvr.getLastBase().equals(xvr.getFirstBase())) {
		// expansionCenter = xvr.getLastBase();
		// }
		if (expansionCenter == null) {
			return null;
		}
		Map map = xvr.getBwapi().getMap();
		BaseLocation nearestFreeBaseLocation = null;
		double nearestDistance = 999999;
		for (BaseLocation location : xvr.getBwapi().getMap().getBaseLocations()) {

			// If there's already a base there don't build. Check for both our
			// and enemy bases.
			if (existsBaseNear(location)) {
				continue;
			}

			// Check if the new base is connected to the main base by land.
			// Region newBaseRegion = xvr.getBwapi().getMap()
			// .getRegion(location.getX(), location.getY());
			// if (!map.isConnected(location, expansionCenter)) {
			// continue;
			// }

			// Look for for the closest base and remember it.
			double distance = map.getGroundDistance(location, expansionCenter) / 32;
			if (distance < 0) { // -1 means there's no path
				continue;
			}

			// double distance = xvr.getDistanceBetween(location.getX(),
			// location.getY(), mainBase.getX(), mainBase.getY());
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestFreeBaseLocation = location;
			}
		}

		return nearestFreeBaseLocation;
	}

	public static boolean existsBaseNear(MapPoint point) {
		return existsBaseNear(point.getX(), point.getY());
	}

	public static boolean existsBaseNear(int x, int y) {
		for (Unit unit : xvr.getUnitsInRadius(x, y, 8)) {
			if (unit.getType().isBase()) {
				return true;
			}
		}

		return false;
	}

	private static int getNumberOfWorkersNearBase(Unit base) {
		return xvr.countUnitsOfGivenTypeInRadius(UnitManager.WORKER, 15, base.getX(), base.getY(), true);
	}

	public static int getOptimalMineralGatherersAtBase(Unit base) {
		int numberOfMineralsNearbyBase = xvr.countMineralsInRadiusOf(12, base.getX(), base.getY());
		return (int) (2.59 * numberOfMineralsNearbyBase) + WORKERS_PER_GEYSER;
	}

	public static Unit getNearestBaseForUnit(MapPoint point) {
		double nearestDistance = 9999999;
		Unit nearestBase = null;

		for (Unit base : xvr.getUnitsOfTypeCompleted(buildingType)) {
			double distance = xvr.getDistanceBetween(base, point);
			if (distance < nearestDistance) {
				distance = nearestDistance;
				nearestBase = base;
			}
		}

		return nearestBase;
	}

	public static Unit getNearestMineralGathererForUnit(Unit base) {
		double nearestDistance = 9999999;
		Unit nearestBase = null;

		for (Unit scv : xvr.getWorkers()) {
			if (scv.isGatheringMinerals()) {
				double distance = xvr.getDistanceBetween(scv, base);
				if (distance < nearestDistance) {
					distance = nearestDistance;
					nearestBase = scv;
				}
			}
		}

		return nearestBase;
	}

	public static int getNumberOfGasGatherersForBase(Unit base) {
		int result = 0;
		int MAX_DISTANCE = 10;

		for (Unit worker : xvr.getWorkers()) {
			if (worker.isGatheringGas()) {
				double distance = xvr.getDistanceBetween(worker, base);
				if (distance < MAX_DISTANCE) {
					result++;
				}
			}
		}

		return result;
	}

	public static int getNumberOfMineralGatherersForBase(Unit base) {
		int result = 0;
		int MAX_DISTANCE = 12;

		for (Unit worker : xvr.getWorkers()) {
			if (worker.isGatheringMinerals()) {
				double distance = xvr.getDistanceBetween(worker, base);
				if (distance < MAX_DISTANCE) {
					result++;
				}
			}
		}

		return result;
	}

	public static ArrayList<Unit> getMineralsNearBase(Unit base) {
		return getMineralsNearBase(base, MAX_DIST_OF_MINERAL_FROM_BASE);
	}

	public static ArrayList<Unit> getMineralsNearBase(Unit base, int maxDist) {
		HashMap<Unit, Double> minerals = new HashMap<Unit, Double>();

		for (Unit mineral : xvr.getMineralsUnits()) {
			double distance = xvr.getDistanceBetween(mineral, base);
			// double distance = xvr.getBwapi().getMap()
			// .getGroundDistance(mineral, base) / 32;
			if (distance < 0) {
				continue;
			}

			if (distance <= maxDist && mineral.getResources() > 1) {
				minerals.put(mineral, distance);
			}
		}

		ArrayList<Unit> sortedList = new ArrayList<Unit>();
		sortedList.addAll(RUtilities.sortByValue(minerals, true).keySet());
		return sortedList;
	}

	public static ArrayList<Unit> getWorkersNearBase(Unit nearestBase) {
		ArrayList<Unit> units = new ArrayList<Unit>();
		for (Unit worker : xvr.getWorkers()) {
			if (worker.isGathering() && xvr.getDistanceBetween(nearestBase, worker) < 20) {
				units.add(worker);
			}
		}
		return units;
	}

	public static ArrayList<Unit> getMineralWorkersNearBase(Unit nearestBase) {
		ArrayList<Unit> units = new ArrayList<Unit>();
		for (Unit worker : xvr.getWorkers()) {
			if (worker.isGatheringMinerals() && xvr.getDistanceBetween(nearestBase, worker) < 20) {
				units.add(worker);
			}
		}
		return units;
	}

	public static ArrayList<Unit> getGasWorkersNearBase(Unit nearestBase) {
		ArrayList<Unit> units = new ArrayList<Unit>();
		for (Unit worker : xvr.getWorkers()) {
			if (worker.isGatheringGas() && xvr.getDistanceBetween(nearestBase, worker) < 12) {
				units.add(worker);
			}
		}
		return units;
	}

	public static void initialMineralGathering() {
		ArrayList<Unit> minerals = getMineralsNearBase(xvr.getFirstBase());

		int counter = 0;
		for (Unit unit : getWorkersNearBase(xvr.getFirstBase())) {
			WorkerManager.forceGatherMinerals(unit, minerals.get(counter));

			counter++;
		}
	}

	public static ArrayList<Unit> getBases() {
		return xvr.getUnitsOfType(buildingType);
	}

	public static UnitTypes getBuildingType() {
		return buildingType;
	}

	public static boolean isExistingCompletedAssimilatorNearBase(Unit nearestBase) {
		ArrayList<Unit> inRadius = xvr.getUnitsOfGivenTypeInRadius(TerranRefinery.getBuildingType(), 12, nearestBase,
				true);

		if (!inRadius.isEmpty() && inRadius.get(0).isCompleted() && inRadius.get(0).getResources() > 50) {
			return true;
		} else {
			return false;
		}
	}

	public static Unit getExistingCompletedAssimilatorNearBase(Unit nearestBase) {
		ArrayList<Unit> inRadius = xvr.getUnitsOfGivenTypeInRadius(TerranRefinery.getBuildingType(), 12, nearestBase,
				true);

		if (!inRadius.isEmpty() && inRadius.get(0).isCompleted() && inRadius.get(0).getResources() > 50) {
			return inRadius.get(0);
		} else {
			return null;
		}
	}

	public static Unit getRandomBase() {
		ArrayList<Unit> bases = getBases();
		if (bases.isEmpty()) {
			return null;
		}
		return (Unit) RUtilities.randomElement(bases);
	}

	public static MapPoint getSecondBaseLocation() {
		if (_secondBase != null) {
			return _secondBase;
		} else {
			_secondBase = TerranCommandCenter.findTileForNextBase(true);
			return _secondBase;
		}
	}

	public static void updateNextBaseToExpand() {
		findTileForNextBase(true);
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(buildingType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(buildingType);
	}

	public static MapPoint get_cachedNextBaseTile() {
		return _cachedNextBaseTile;
	}

}

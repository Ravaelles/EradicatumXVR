package ai.terran;

import java.util.ArrayList;
import java.util.Iterator;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.strategy.BotStrategyManager;
import ai.managers.units.UnitManager;
import ai.utils.RUtilities;

public class TerranSupplyDepot {

	// private static int INITIAL_DEPOT_MIN_DIST_FROM_BASE = 6;
	// private static int INITIAL_DEPOT_MAX_DIST_FROM_BASE = 18;
	private static int DEPOT_FROM_DEPOT_MIN_DISTANCE = 0;
	private static int DEPOT_FROM_DEPOT_MAX_DISTANCE = 7;

	private static final UnitTypes buildingType = UnitTypes.Terran_Supply_Depot;
	private static final UnitType unitType = UnitTypes.Terran_Supply_Depot.getType();
	private static XVR xvr = XVR.getInstance();

	public static void buildIfNecessary() {
		if (xvr.canAfford(100)) {

			// It only makes sense to build Supply Depot if supplies less than
			// X.
			if (shouldBuild()) {
				Constructing.construct(xvr, buildingType);
			}
		}
	}

	public static boolean shouldBuild() {
		boolean weAreBuilding = Constructing.weAreBuilding(buildingType);

		int free = xvr.getSuppliesFree();
		int total = xvr.getSuppliesTotal();
		int depots = UnitCounter.getNumberOfUnits(buildingType);
		int barracks = TerranBarracks.getNumberOfUnits();
		int workers = UnitCounter.getNumberOfUnits(UnitManager.WORKER);
		int engineeringBays = TerranEngineeringBay.getNumberOfUnits();

		// ZERG RUSH
		if (XVR.isEnemyZerg()) {
			if (barracks == 0) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
				return false;
			}

			if (barracks >= 1 && depots == 0 && xvr.canAfford(200)) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}

			if ((depots >= 1 || weAreBuilding) && TerranBunker.getNumberOfUnits() == 0) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
				return false;
			}
		}

		// NON-ZERG RUSH
		else {
			if (depots == 0) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
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

	private static Unit getRandomSupplyDepot() {
		ArrayList<Unit> pylons = getSupplyDepots();
		return (Unit) RUtilities.getRandomListElement(pylons);
	}

	public static double calculateExistingPylonsStrength() {
		double result = 0;

		for (Unit pylon : xvr.getUnitsOfType(buildingType)) {
			result += (double) pylon.getHP() / 500;
		}

		return result;
	}

	public static MapPoint findTileForDepot() {
		Unit builder = Constructing.getRandomWorker();

		if (UnitCounter.weHaveSupplyDepot()) {
			return findTileForNextDepot(builder);
		}

		// It's the first Depot
		else {
			return findTileForFirstDepot(builder, xvr.getFirstBase());
		}
	}

	private static MapPoint findTileForNextDepot(Unit builder) {

		// Or build near random depot.
		Unit supplyDepot = null;
		ArrayList<Unit> depotsNearMainBase = xvr.getUnitsOfGivenTypeInRadius(buildingType, 14,
				xvr.getFirstBase(), true);
		if (!depotsNearMainBase.isEmpty()) {
			supplyDepot = (Unit) RUtilities.getRandomElement(depotsNearMainBase);
		}
		if (supplyDepot == null) {
			supplyDepot = getRandomSupplyDepot();
		}

		MapPoint tile = findTileForSupplyDepotNearby(supplyDepot, DEPOT_FROM_DEPOT_MIN_DISTANCE,
				DEPOT_FROM_DEPOT_MAX_DISTANCE);
		if (tile != null) {
			return tile;
		} else {
			return Constructing.findTileForStandardBuilding(buildingType);
		}

	}

	private static MapPoint findTileForSupplyDepotNearby(MapPoint point, int minDist, int maxDist) {
		return findLegitTileForDepot(point, Constructing.getRandomWorker());
	}

	private static MapPoint findLegitTileForDepot(MapPoint buildNearToHere, Unit builder) {
		int tileX = buildNearToHere.getTx();
		int tileY = buildNearToHere.getTy();

		int currentDist = DEPOT_FROM_DEPOT_MIN_DISTANCE;
		while (currentDist <= DEPOT_FROM_DEPOT_MAX_DISTANCE) {
			for (int i = tileX - currentDist; i <= tileX + currentDist; i++) {
				if (i % 3 != 0 || i % 9 == 0) {
					continue;
				}
				for (int j = tileY - currentDist; j <= tileY + currentDist; j++) {
					if (j % 2 != 0 || j % 6 == 0) {
						continue;
					}
					int x = i * 32;
					int y = j * 32;
					if (Constructing.canBuildHere(builder, unitType, i, j)
							&& xvr.getUnitsOfGivenTypeInRadius(buildingType,
									DEPOT_FROM_DEPOT_MIN_DISTANCE - 1, x, y, true).isEmpty()) {
						MapPointInstance point = new MapPointInstance(x, y);
						if (!Constructing.isTooNearMineralsOrGeyser(buildingType.getType(), point)) {

							// Damn, try NOT to build in the middle of narrow
							// choke point.
							if (!Constructing.isTooCloseToAnyChokePoint(point)) {
								return point;
							}
						}
					}
					if (j % 4 == 0) {
						j += 2;
					}
				}
			}

			currentDist++;
		}
		return null;
	}

	private static ArrayList<Unit> getSupplyDepots() {
		ArrayList<Unit> depots = xvr.getUnitsOfType(buildingType);
		for (Iterator<Unit> iterator = depots.iterator(); iterator.hasNext();) {
			Unit unit = (Unit) iterator.next();
			if (!unit.isCompleted()) {
				iterator.remove();
			}
		}
		return depots;
	}

	private static MapPoint findTileForFirstDepot(Unit builder, Unit base) {
		if (base == null) {
			return null;
		}

		// Find point being in the middle of way base<->nearest choke point.
		ChokePoint choke = MapExploration.getNearestChokePointFor(base);
		MapPointInstance location = new MapPointInstance(
				(2 * base.getX() + choke.getCenterX()) / 3,
				(2 * base.getY() + choke.getCenterY()) / 3);
		// System.out.println();
		// System.out.println(choke.toStringLocation());
		// System.out.println(location.toStringLocation());

		return Constructing.getLegitTileToBuildNear(builder, buildingType, location, 0, 100);
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

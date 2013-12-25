package ai.managers.constructing;

import java.util.ArrayList;

import jnibwapi.JNIBWAPI;
import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.units.UnitManager;
import ai.terran.ProtossArbiterTribunal;
import ai.terran.ProtossCitadelOfAdun;
import ai.terran.ProtossFleetBeacon;
import ai.terran.TerranFactory;
import ai.terran.ProtossRoboticsSupportBay;
import ai.terran.ProtossShieldBattery;
import ai.terran.ProtossTemplarArchives;
import ai.terran.TerranAcademy;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranComsatStation;
import ai.terran.TerranEngineeringBay;
import ai.terran.TerranRefinery;
import ai.terran.TerranSupplyDepot;

public class Constructing {

	private static XVR xvr = XVR.getInstance();

	// ============================

	public static MapPoint findTileForNewBuilding(UnitTypes typeToBuild) {
		// Unit base = ProtossNexus.getRandomBase();
		Unit base = xvr.getFirstBase();
		if (base == null) {
			return null;
		}

		// return Constructing.getLegitTileToBuildNear(
		// xvr.getRandomWorker(), buildingType, pylon.getTileX(),
		// pylon.getTileY(), 0, PYLON_FROM_PYLON_MAX_DISTANCE);
		// return new Point(pylonWithLeastBuildings.getTileX(),
		// pylonWithLeastBuildings.getTileY());
		// int minDistanceBetweenPylons =
		// PYLON_FROM_PYLON_MIN_DISTANCE_RAND
		// + PYLON_FROM_PYLON_MIN_DISTANCE_RAND
		// - RUtilities.rand(0,
		// 2 * PYLON_FROM_PYLON_MIN_DISTANCE_RAND);

		int minDistFromBase = 9;
		// MapPoint secondBase = ProtossNexus.getSecondBaseLocation();
		// if (secondBase != null
		// && xvr.getDistanceBetween(pylon, secondBase) < 12) {
		// minDistFromPylon = 0;
		// }

		MapPoint tile = Constructing.getLegitTileToBuildNear(xvr.getRandomWorker(), typeToBuild,
				base, minDistFromBase, 50);
		if (tile != null) {
			return tile;
		}
		return null;
	}

	private static MapPoint getTileAccordingToBuildingType(UnitTypes building) {

		// Supply Depot
		if (TerranSupplyDepot.getBuildingType().ordinal() == building.ordinal()) {
			return TerranSupplyDepot.findTileForDepot();
		}

		// Bunker
		else if (TerranBunker.getBuildingType().ordinal() == building.ordinal()) {
			return TerranBunker.findTileForBunker();
		}

		// Refinery
		else if (TerranRefinery.getBuildingType().ordinal() == building.ordinal()) {
			return findTileForAssimilator();
		}

		// Base
		else if (TerranCommandCenter.getBuildingType().ordinal() == building.ordinal()) {
			return TerranCommandCenter.getTileForNextBase(false);
		}

		// Standard building
		else {

			// Let the Pylon class decide where to put buildings like Gateway,
			// Stargate etc.
			return findTileForNewBuilding(building);

			// // Get random worker, it doesn't matter now which one, because we
			// // will decide later. But for now we have to specify something.
			// Unit workerUnit = xvr.getRandomWorker();
			// if (workerUnit == null) {
			// return null;
			// } else {
			// return Constructing.findBuildTile(xvr, workerUnit.getID(),
			// building.ordinal(), buildInNeighborhoodOf.getX(),
			// buildInNeighborhoodOf.getY());
			// }
		}
	}

	/**
	 * @return if we need to build some building it will return non-null value,
	 *         being int array containing three elements: first is total amount
	 *         of minerals required all buildings that we need to build, while
	 *         second is total amount of gas required and third returns total
	 *         number of building types that we want to build. If we don't need
	 *         to build anything right now it returns null
	 * */
	public static int[] shouldBuildAnyBuilding() {
		int mineralsRequired = 0;
		int gasRequired = 0;
		int buildingsToBuildTypesNumber = 0;
		if (TerranCommandCenter.shouldBuild()) {
			mineralsRequired += 400;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (ProtossTemplarArchives.shouldBuild()) {
			mineralsRequired += 150;
			gasRequired += 200;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (TerranFactory.shouldBuild()) {
			mineralsRequired += 200;
			gasRequired += 200;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (ProtossRoboticsSupportBay.shouldBuild()) {
			mineralsRequired += 150;
			gasRequired += 100;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (TerranBarracks.shouldBuild()) {
			mineralsRequired += 150;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		// if (TerranBunker.shouldBuild()
		// && UnitCounter.getNumberOfUnits(UnitTypes.Protoss_Bunker) < 2) {
		// mineralsRequired += 100;
		// buildingsToBuildTypesNumber++;
		// }
		if (TerranRefinery.shouldBuild()) {
			mineralsRequired += 100;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (TerranSupplyDepot.shouldBuild()) {
			mineralsRequired += 100;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (TerranEngineeringBay.shouldBuild()) {
			mineralsRequired += 150;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (TerranAcademy.shouldBuild()) {
			mineralsRequired += 150;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (ProtossCitadelOfAdun.shouldBuild()) {
			mineralsRequired += 150;
			gasRequired += 100;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (TerranComsatStation.shouldBuild()) {
			mineralsRequired += 50;
			gasRequired += 100;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (ProtossArbiterTribunal.shouldBuild()) {
			mineralsRequired += 200;
			gasRequired += 150;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (ProtossFleetBeacon.shouldBuild()) {
			mineralsRequired += 300;
			gasRequired += 200;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}
		if (ProtossShieldBattery.shouldBuild()) {
			mineralsRequired += 100;
			buildingsToBuildTypesNumber++;
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		}

		if (buildingsToBuildTypesNumber > 0) {
			return new int[] { mineralsRequired + 8, gasRequired, buildingsToBuildTypesNumber };
		} else {
			return null;
		}
	}

	public static MapPoint findBuildTile(XVR xvr, int builderID, UnitTypes type, MapPoint place) {
		return findBuildTile(xvr, builderID, type.ordinal(), place.getX(), place.getY());
	}

	public static MapPoint findBuildTile(XVR xvr, int builderID, int buildingTypeID, int x, int y) {
		MapPoint tileToBuild = findTileForNewBuilding(UnitType.getUnitTypesByID(buildingTypeID));

		if (tileToBuild == null) {
			JNIBWAPI bwapi = xvr.getBwapi();
			bwapi.printText("Unable to find tile for new "
					+ bwapi.getUnitType(buildingTypeID).getName());
		}
		return tileToBuild;
	}

	public static MapPoint findTileForAssimilator() {
		Unit nearestGeyser = xvr.getUnitNearestFromList(xvr.getFirstBase(), xvr.getGeysersUnits());
		if (nearestGeyser != null
				&& xvr.getUnitsOfGivenTypeInRadius(UnitManager.BASE, 15, nearestGeyser, true)
						.isEmpty()) {
			return null;
		}

		// return new MapPointInstance(nearestGeyser.getX(),
		// nearestGeyser.getY());
		if (nearestGeyser != null) {
			return new MapPointInstance(nearestGeyser.getX() - 64, nearestGeyser.getY() - 32);
		} else {
			return null;
		}
	}

	public static MapPoint getLegitTileToBuildNear(UnitTypes type, MapPoint nearTo,
			int minimumDist, int maximumDist) {
		Unit worker = xvr.getRandomWorker();
		if (worker == null || type == null) {
			return null;
		}
		return getLegitTileToBuildNear(worker.getID(), type.ordinal(), nearTo.getTx(),
				nearTo.getTy(), minimumDist, maximumDist);
	}

	public static MapPoint getLegitTileToBuildNear(Unit worker, UnitTypes type, MapPoint nearTo,
			int minimumDist, int maximumDist) {
		if (worker == null || type == null) {
			return null;
		}
		return getLegitTileToBuildNear(worker.getID(), type.ordinal(), nearTo.getTx(),
				nearTo.getTy(), minimumDist, maximumDist);
	}

	public static MapPoint getLegitTileToBuildNear(Unit worker, UnitTypes type, int tileX,
			int tileY, int minimumDist, int maximumDist, boolean requiresPower) {
		if (worker == null || type == null) {
			return null;
		}
		return getLegitTileToBuildNear(worker.getID(), type.ordinal(), tileX, tileY, minimumDist,
				maximumDist);
	}

	public static MapPoint getLegitTileToBuildNear(int builderID, int buildingTypeID, int tileX,
			int tileY, int minimumDist, int maximumDist) {
		JNIBWAPI bwapi = XVR.getInstance().getBwapi();
		UnitType type = UnitType.getUnitTypeByID(buildingTypeID);
		boolean isBase = type.isBase();
		boolean isBunker = type.isBunker();
		boolean isDepot = type.isSupplyDepot();

		int currentDist = minimumDist;
		while (currentDist <= maximumDist) {
			for (int i = tileX - currentDist; i <= tileX + currentDist; i++) {
				for (int j = tileY - currentDist; j <= tileY + currentDist; j++) {
					if (bwapi.canBuildHere(builderID, i, j, buildingTypeID, false)) {
						// && isBuildTileFullyBuildableFor(builderID, i, j,
						// buildingTypeID)
						int x = i * 32;
						int y = j * 32;
						MapPointInstance place = new MapPointInstance(x, y);
						Unit optimalBuilder = xvr.getOptimalBuilder(place);
						if (optimalBuilder != null
								&& (isBunker || isBase || isBuildTileFreeFromUnits(
										optimalBuilder.getID(), i, j))) {
							if (!isTooNearMineralAndBase(place)
									&& (isDepot || isEnoughPlaceToOtherBuildings(place, type))
									&& (isBase || !isOverlappingNextNexus(place, type))
									&& (isBase || !isTooCloseToAnyChokePoint(place))) {

								// if (type.isPhotonCannon()) {
								// System.out.println("@@@@@@@ "
								// + xvr.getDistanceBetween(choke, place) +
								// "/"
								// + choke.getRadius());
								// }
								return place;
							}
						}
					}
				}
			}

			currentDist++;
		}

		return null;
	}

	public static boolean isTooCloseToAnyChokePoint(MapPointInstance place) {
		// for (ChokePoint choke : MapExploration.getChokePoints()) {
		// if (choke.getRadius() < 210
		// && (xvr.getDistanceBetween(choke, place) - choke.getRadius() / 32) <=
		// MIN_DIST_FROM_CHOKE_POINT) {
		// return true;
		// }
		// }
		return false;
	}

	private static boolean isOverlappingNextNexus(MapPoint place, UnitType type) {
		if (!type.isBase()
				&& UnitCounter.getNumberOfUnits(TerranSupplyDepot.getBuildingType()) >= 1) {
			return xvr.getDistanceSimple(place, TerranCommandCenter.getTileForNextBase(false)) <= 4;
		} else {
			return false;
		}
	}

	private static boolean isEnoughPlaceToOtherBuildings(MapPoint place, UnitType type) {
		// type.isPhotonCannon() ||
		if (type.isBase() || type.isOnGeyser()) {
			return true;
		}

		int wHalf = type.getTileWidth();
		int hHalf = type.getTileHeight();
		int maxDimension = wHalf > hHalf ? wHalf : hHalf;

		// Define center of the building
		MapPoint center = new MapPointInstance(place.getX() + wHalf, place.getY() + hHalf);

		ArrayList<Unit> buildingsNearby = xvr.getUnitsInRadius(center, maxDimension + 1,
				xvr.getUnitsBuildings());

		// System.out.println("FOR: " + type.getName());
		// for (Unit unit : buildingsNearby) {
		// System.out.println("   " + unit.getName() + ": " +
		// unit.distanceTo(center));
		// }
		// System.out.println();

		for (Unit unit : buildingsNearby) {
			if (unit.getType().isBuilding() && unit.distanceTo(center) <= maxDimension + 1) {
				return false;
			}
		}
		return true;
	}

	public static boolean isTooNearMineralAndBase(MapPoint point) {
		Unit nearestMineral = xvr.getUnitNearestFromList(point, xvr.getMineralsUnits());
		double distToMineral = xvr.getDistanceBetween(nearestMineral, point);
		if (distToMineral <= 3) {
			return true;
		}

		if (distToMineral <= 5) {
			Unit nearestBase = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, point);
			double distToBase = xvr.getDistanceBetween(nearestBase, point);
			if (distToBase < distToMineral) {
				return true;
			}
		}
		return false;
	}

	public static boolean isBuildTileFullyBuildableFor(int builderID, int i, int j,
			int buildingTypeID) {
		UnitType buildingType = UnitType.getUnitTypeByID(buildingTypeID);
		int wHalf = buildingType.getTileWidth() / 2;
		int hHalf = buildingType.getTileHeight() / 2;
		for (int tx = i - wHalf; tx < i + wHalf; tx++) {
			for (int ty = j - hHalf; ty < j + hHalf; ty++) {
				if (!xvr.getBwapi().isBuildable(tx, ty, true)) {
					return false;
				}
			}
		}

		// if (UnitCounter.weHaveBuildingFinished(UnitTypes.Protoss_Pylon)) {
		// MapPoint tileForNextBase = ProtossNexus.getTileForNextBase(false);
		// if (tileForNextBase != null
		// && xvr.getDistanceBetween(tileForNextBase,
		// new MapPointInstance(i * 32, j * 32)) < 3) {
		// return false;
		// }
		// }

		return true;
	}

	public static boolean isBuildTileFreeFromUnits(int builderID, int tileX, int tileY) {
		JNIBWAPI bwapi = XVR.getInstance().getBwapi();
		MapPointInstance point = new MapPointInstance(tileX * 32, tileY * 32);

		// Check if units are blocking this tile
		boolean unitsInWay = false;
		for (Unit u : bwapi.getAllUnits()) {
			if (u.getID() == builderID) {
				continue;
			}
			if (xvr.getDistanceBetween(u, point) <= 3) {
				// for (Unit unit : xvr.getUnitsInRadius(point, 4,
				// xvr.getBwapi().getMyUnits())) {
				// UnitActions.moveAwayFromUnitIfPossible(unit, point, 6);
				// }
				unitsInWay = true;
			}
		}
		if (!unitsInWay) {
			return true;
		}

		return false;
	}

	public static void construct(XVR xvr, UnitTypes building) {

		// Define tile where to build according to type of building.
		MapPoint buildTile = getTileAccordingToBuildingType(building);
		// System.out.println("buildTile FOR: " + building + " = " + buildTile);
		// Debug.message(xvr, "buildTile FOR: " + building + " = " + buildTile);

		// Check if build tile is okay.
		if (buildTile != null) {

			// If this building is base, make sure there's a pylon and at least
			// two cannons build nearby
			if (building.getType().isBase()) {
				handleBaseConstruction(building, buildTile);
			} else {

				// Proper construction order
				constructBuilding(xvr, building, buildTile);
			}

		}
	}

	/** The idea is to build pylon and cannon first, just then build Nexus. */
	private static void handleBaseConstruction(UnitTypes building, MapPoint buildTile) {
		boolean baseInterrupted = false;

		// System.out.println("Base build: " + buildTile);

		// ==============================

		// Try to find proper choke to reinforce
		ChokePoint choke = MapExploration.getImportantChokePointNear(buildTile);

		// Get point in between choke and base
		MapPointInstance point = MapPointInstance.getMiddlePointBetween(buildTile, choke);

		int cannonsNearby = xvr.countUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(), 16,
				point, true);

		// ==============================
		// Ensure there's a bunker nearby
		if (cannonsNearby < 0) {
			baseInterrupted = true;
			building = TerranBunker.getBuildingType();
			buildTile = TerranBunker.findTileForBunker();
		}

		// ==============================
		// We can build the base
		if (!baseInterrupted) {
			if (buildTile == null || !Constructing.canBuildAt(buildTile, UnitManager.BASE)) {
				// System.out.println("TEST cant Build At: " + buildTile);
				buildTile = TerranCommandCenter.getTileForNextBase(true);
			}
		}

		// System.out.println((buildTile != null ? buildTile.toStringLocation()
		// : buildTile) + " : "
		// + Constructing.canBuildAt(buildTile, UnitManager.BASE));

		constructBuilding(xvr, building, buildTile);
	}

	private static boolean canBuildAt(MapPoint point, UnitTypes type) {
		Unit randomWorker = xvr.getRandomWorker();
		if (randomWorker == null || point == null) {
			return false;
		}
		return xvr.getBwapi().canBuildHere(randomWorker.getID(), point.getTx(), point.getTy(),
				type.getID(), false);
	}

	protected static boolean constructBuilding(XVR xvr, UnitTypes building, MapPoint buildTile) {
		if (buildTile == null) {
			return false;
		}

		Unit workerUnit = xvr.getOptimalBuilder(buildTile);
		if (workerUnit != null) {

			// if we found a good build position, and we aren't already
			// constructing this building order our worker to build it
			// && (!xvr.weAreBuilding(building))
			if (buildTile != null) {
				// Debug.messageBuild(xvr, building);
				build(workerUnit, buildTile, building);

				// // If it's base then build pylon for new base
				// if (UnitType.getUnitTypeByID(building.getID()).isBase()) {
				// forceConstructionOfPylonNear(buildTile);
				// }
				return true;
			}
		}
		return false;
	}

	public static int ifWeAreBuildingItCountHowManyWorkersIsBuildingIt(UnitTypes type) {
		int result = 0;

		// if (_recentConstructionsInfo.containsKey(type)) {
		// result++;
		// }
		for (Unit unit : xvr.getWorkers()) {
			if (unit.getBuildTypeID() == type.ordinal()) {
				result++;
			}
		}
		return result;
	}

	public static boolean weAreBuilding(UnitTypes type) {
		return ConstructingManager.weAreBuilding(type);
	}

	private static void build(Unit builder, MapPoint buildTile, UnitTypes building) {
		boolean canProceed = false;

		// Disallow multiple building of all buildings, except cannons.
		if (building.getType().isBarracks()) {
			int builders = ifWeAreBuildingItCountHowManyWorkersIsBuildingIt(building);
			canProceed = builders == 0;
		} else {
			canProceed = !weAreBuilding(building);
		}

		if (canProceed) {
			xvr.getBwapi().build(builder.getID(), buildTile.getTx(), buildTile.getTy(),
					building.ordinal());
			ConstructingManager.addInfoAboutConstruction(building, builder, buildTile);
			// removeDuplicateConstructionsPending(builder);
		}
	}

	public static Unit getRandomWorker() {
		return xvr.getRandomWorker();
	}

	public static boolean canBuildHere(Unit builder, UnitTypes buildingType, int tx, int ty) {
		return xvr.getBwapi().canBuildHere(builder.getID(), tx, ty, buildingType.ordinal(), false);
		// && isBuildTileFreeFromUnits(builder.getID(), tx, ty)
	}

	public static void constructAddOn(Unit buildingWithNoAddOn, UnitTypes buildingType) {
		if (buildingWithNoAddOn == null || buildingType == null) {
			return;
		}
		xvr.getBwapi().buildAddon(buildingWithNoAddOn.getID(), buildingType.ordinal());
	}

	// /** This method shouldn't be used other than in very, very specific
	// cases. */
	// public static void forceConstructingPylonNear(MapPoint
	// tryBuildingAroundHere) {
	//
	// // First find proper place for building.
	// MapPoint tileForBuilding = getLegitTileToBuildNear(getRandomWorker(),
	// ProtossPylon.getBuildingType(), tryBuildingAroundHere, 5, 13, false);
	//
	// // Construct building here.
	// constructBuilding(xvr, ProtossPylon.getBuildingType(), tileForBuilding);
	// // if (!operation) {
	// // Debug.message(xvr, "Forced constr: no place for " + buildingtype);
	// // }
	// }

}

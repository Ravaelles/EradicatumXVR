package ai.managers.constructing;

import java.util.ArrayList;

import jnibwapi.JNIBWAPI;
import jnibwapi.model.ChokePoint;
import jnibwapi.model.Region;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.units.UnitManager;
import ai.terran.TerranAcademy;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranComsatStation;
import ai.terran.TerranEngineeringBay;
import ai.terran.TerranFactory;
import ai.terran.TerranMissileTurret;
import ai.terran.TerranRefinery;
import ai.terran.TerranSupplyDepot;

public class Constructing {

	private static XVR xvr = XVR.getInstance();

	private static int _skipCheckingForTurns = 0;

	// ============================

	public static void construct(XVR xvr, UnitTypes building) {

		// Define tile where to build according to type of building.
		MapPoint buildTile = getTileAccordingToBuildingType(building);
		// System.out.println("buildTile FOR: " + building + " = " + buildTile);
		// Debug.message(xvr, "buildTile FOR: " + building + " = " + buildTile);

		// Check if build tile is okay.
		if (buildTile != null) {
			// if (building.getType().isBase()) {
			// handleBaseConstruction(building, buildTile);
			// } else {
			constructBuilding(xvr, building, buildTile);
			// }
		}
	}

	// ============================

	public static MapPoint findTileForStandardBuilding(UnitTypes typeToBuild) {

		// There is a nasty bug: when we're losing badly Terran Barracks are
		// slowing down game terribly; try to limit search range.
		int MAX_RANGE = 70;
		if (xvr.getTimeSeconds() > 400
				&& typeToBuild.ordinal() == UnitTypes.Terran_Barracks.ordinal()) {
			MAX_RANGE = 20;
		}

		Unit base = xvr.getFirstBase();
		if (base == null) {
			return null;
		}

		MapPoint tile = Constructing.getLegitTileToBuildNear(xvr.getRandomWorker(), typeToBuild,
				base.translate(5, 2), 5, MAX_RANGE);

		return tile;
	}

	private static MapPoint getTileAccordingToBuildingType(UnitTypes building) {
		MapPoint buildTile = null;
		boolean disableReportOfNoPlaceFound = false;

		// Supply Depot
		if (TerranSupplyDepot.getBuildingType().ordinal() == building.ordinal()) {
			buildTile = TerranSupplyDepot.findTileForDepot();
		}

		// Bunker
		else if (TerranBunker.getBuildingType().ordinal() == building.ordinal()) {
			buildTile = TerranBunker.findTileForBunker();
		}

		// Missile Turret
		else if (TerranMissileTurret.getBuildingType().ordinal() == building.ordinal()) {
			buildTile = TerranMissileTurret.findTileForTurret();
		}

		// Refinery
		else if (TerranRefinery.getBuildingType().ordinal() == building.ordinal()) {
			buildTile = TerranRefinery.findTileForRefinery();
			disableReportOfNoPlaceFound = true;
		}

		// Base
		else if (TerranCommandCenter.getBuildingType().ordinal() == building.ordinal()) {
			buildTile = TerranCommandCenter.findTileForNextBase(false);
		}

		// Standard building
		else {
			if (_skipCheckingForTurns > 0) {
				_skipCheckingForTurns--;
				return null;
			}
			buildTile = findTileForStandardBuilding(building);
		}

		if (buildTile == null && !disableReportOfNoPlaceFound) {
			System.out.println("# No tile found for: " + building.getType().getName());
		}

		return buildTile;
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
		if (TerranFactory.shouldBuild()) {
			mineralsRequired += 200;
			gasRequired += 200;
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
		if (TerranComsatStation.shouldBuild()) {
			mineralsRequired += 50;
			gasRequired += 100;
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
		MapPoint tileToBuild = findTileForStandardBuilding(UnitType
				.getUnitTypesByID(buildingTypeID));

		if (tileToBuild == null) {
			JNIBWAPI bwapi = xvr.getBwapi();
			bwapi.printText("Unable to find tile for new "
					+ bwapi.getUnitType(buildingTypeID).getName());
		}
		return tileToBuild;
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
		// JNIBWAPI bwapi = XVR.getInstance().wgetBwapi();
		UnitType type = UnitType.getUnitTypeByID(buildingTypeID);
		boolean isBase = type.isBase();
		boolean isDepot = type.isSupplyDepot();
		// boolean checkExplored = type.isBunker();

		// boolean skipCheckingIsFreeFromUnits = type.isBase();
		boolean skipCheckingIsFreeFromUnits = false;
		boolean skipCheckingRegion = xvr.getTimeSeconds() > 250 || isBase || type.isBunker()
				|| type.isMissileTurret() || type.isAddon();

		int currentDist = minimumDist;
		while (currentDist <= maximumDist) {
			for (int i = tileX - currentDist; i <= tileX + currentDist; i++) {
				if (isDepot && (i % 3 != 0 || i % 9 == 0)) {
					continue;
				}
				for (int j = tileY - currentDist; j <= tileY + currentDist; j++) {
					if (isDepot && (j % 2 != 0 || j % 6 == 0)) {
						continue;
					}
					int x = i * 32;
					int y = j * 32;
					MapPointInstance place = new MapPointInstance(x, y);
					// bwapi.canBuildHere(builderID, i, j, buildingTypeID,
					// false)
					if (canBuildAt(place, type)) {
						// && isBuildTileFullyBuildableFor(builderID, i, j,
						// buildingTypeID)
						Unit optimalBuilder = xvr.getOptimalBuilder(place);
						if (optimalBuilder != null
								&& (skipCheckingIsFreeFromUnits || isBuildTileFreeFromUnits(
										optimalBuilder.getID(), i, j))) {
							if ((isBase || !isTooNearMineralsOrGeyser(type, place))
									&& (isBase || isEnoughPlaceToOtherBuildings(place, type))
									&& (isBase || !isOverlappingNextBase(place, type))
									&& (isBase || !isTooCloseToAnyChokePoint(place)
											&& (skipCheckingRegion || isInAllowedRegions(place)))) {

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

	private static boolean isInAllowedRegions(MapPoint place) {
		Region buildTileRegion = xvr.getMap().getRegion(place);
		if (buildTileRegion.equals(xvr.getFirstBase().getRegion())
				|| buildTileRegion.equals(TerranCommandCenter.getSecondBaseLocation().getRegion())) {
			return true;
		}
		return false;
	}

	public static boolean isTooCloseToAnyChokePoint(MapPoint place) {
		ChokePoint nearestChoke = MapExploration.getNearestChokePointFor(place);
		int chokeTiles = (int) (nearestChoke.getRadius() / 32);

		if (chokeTiles >= 6) {
			return false;
		} else {
			return place.distanceToChokePoint(nearestChoke) <= 4.5;
		}

		// for (ChokePoint choke : MapExploration.getChokePoints()) {
		// if (choke.getRadius() < 210
		// && (xvr.getDistanceBetween(choke, place) - choke.getRadius() / 32) <=
		// MIN_DIST_FROM_CHOKE_POINT) {
		// return true;
		// }
		// }
	}

	private static boolean isOverlappingNextBase(MapPoint place, UnitType type) {
		if (!type.isBase()
				&& UnitCounter.getNumberOfUnits(TerranSupplyDepot.getBuildingType()) >= 1) {
			return xvr.getDistanceSimple(place, TerranCommandCenter.findTileForNextBase(false)
					.translate(62, 0)) <= 6;
		} else {
			return false;
		}
	}

	private static boolean isEnoughPlaceToOtherBuildings(MapPoint place, UnitType type) {
		if (type.isBase() || type.isOnGeyser()) {
			return true;
		}
		boolean isDepot = type.isSupplyDepot();

		// ==============================
		// Define building dimensions
		int wHalf = type.getTileWidth() + (type.canHaveAddOn() ? 2 : 0);
		int hHalf = type.getTileHeight();
		int maxDimension = wHalf > hHalf ? wHalf : hHalf;

		// ==============================
		// Define center of the building
		MapPoint center = new MapPointInstance(place.getX() + wHalf, place.getY() + hHalf);

		// Define buildings that are near this build tile
		ArrayList<Unit> buildingsNearby = xvr.getUnitsInRadius(center, 10, xvr.getUnitsBuildings());

		// If this building can have an Add-On, it is essential we keep place
		// for it.
		int spaceBonus = 0;
		if (type.canHaveAddOn()) {
			// spaceBonus += 2;
			center = center.translate(96, 0);
		}

		// For each building nearby define if it's not too close to this build
		// tile. If so, reject this build tile.
		for (Unit unit : buildingsNearby) {
			if (unit.isLifted()) {
				continue;
			}

			// Supply Depots can be really close to each other, but only if
			// there're few of them
			if (isDepot
					&& type.isSupplyDepot()
					&& xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Supply_Depot, 5, place,
							true) <= 2
					&& xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Supply_Depot, 9, place,
							true) <= 3) {
				continue;
			}

			// Also: don't build in the place where there COULD BE Add-On for a
			// different, already existing building
			int dx = 0;
			int bonus = spaceBonus;
			UnitType unitType = unit.getType();
			if (type.canHaveAddOn()) {
				// bonus++;
				dx = 64;
			}
			if (unitType.isBase()) {
				dx += 32;
				bonus += 4;
			}

			// If this building is too close to our build tile, indicate this
			// fact.
			if (type.isBuilding() && !unit.isLifted()
					&& unit.translate(dx, 0).distanceTo(center) <= maxDimension + 1 + bonus) {
				return false;
			}
		}
		return true;
	}

	public static boolean isTooNearMineralsOrGeyser(UnitType type, MapPoint point) {
		int minDistBonus = type.canHaveAddOn() ? 2 : 0;

		// Check if isn't too near to geyser
		Unit nearestGeyser = xvr.getUnitNearestFromList(point, xvr.getGeysersUnits());
		double distToGeyser = xvr.getDistanceBetween(nearestGeyser, point);
		Unit nearestBase = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, point);
		if (distToGeyser <= 7 + minDistBonus) {
			double distBaseToGeyser = xvr.getDistanceBetween(nearestBase, nearestGeyser);
			if (distBaseToGeyser >= distToGeyser + minDistBonus) {
				return false;
			}
		}

		// ==================================
		// Check if isn't too near to mineral
		Unit nearestMineral = xvr.getUnitNearestFromList(point, xvr.getMineralsUnits());
		double distToMineral = xvr.getDistanceBetween(nearestMineral, point);
		if (distToMineral <= 7 + minDistBonus) {
			return true;
		}

		if (distToMineral <= 10 + minDistBonus) {
			if (nearestBase.distanceTo(point) <= 4 + minDistBonus) {
				return false;
			}

			double distBaseToMineral = xvr.getDistanceBetween(nearestBase, nearestMineral);
			if (distToMineral < distBaseToMineral + minDistBonus) {
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

	private static boolean canBuildAt(MapPoint point, UnitType type) {
		Unit randomWorker = xvr.getRandomWorker();
		if (randomWorker == null || point == null) {
			return false;
		}

		// Buildings that can have an add-on, must have additional space on
		// their right
		if (type.canHaveAddOn() && !type.isBase()) {
			if (!xvr.getBwapi().canBuildHere(randomWorker.getID(), point.getTx() + 2,
					point.getTy(), type.getUnitTypes().getID(), false)) {
				return false;
			}
		}
		return xvr.getBwapi().canBuildHere(randomWorker.getID(), point.getTx(), point.getTy(),
				type.getUnitTypes().getID(), false);
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
		return ConstructionManager.weAreBuilding(type);
	}

	private static void build(Unit builder, MapPoint buildTile, UnitTypes building) {
		boolean canProceed = false;

		// Disallow multiple building of all buildings, except barracks,
		// bunkers.
		if (building.getType().isBarracks()) {
			int builders = ifWeAreBuildingItCountHowManyWorkersIsBuildingIt(building);
			int barracks = TerranBarracks.getNumberOfUnits();
			if (barracks != 1) {
				canProceed = builders == 0;
			}
			if (barracks == 1) {
				canProceed = builders <= 1;
			}
		} else if (building.getType().isBunker()) {
			int builders = ifWeAreBuildingItCountHowManyWorkersIsBuildingIt(building);
			int bunkers = TerranBunker.getNumberOfUnits();
			if (bunkers != 1) {
				canProceed = builders == 0;
			}
			if (bunkers == 1) {
				canProceed = builders <= 1;
			}
		} else {
			canProceed = !weAreBuilding(building) || building.getType().isBase();
		}

		// If there aren't multiple orders to build one building given, we can
		// proceed
		if (canProceed) {
			xvr.getBwapi().build(builder.getID(), buildTile.getTx(), buildTile.getTy(),
					building.ordinal());
			ConstructionManager.addInfoAboutConstruction(building, builder, buildTile);
		}
	}

	public static Unit getRandomWorker() {
		return xvr.getRandomWorker();
	}

	public static boolean canBuildHere(Unit builder, UnitType buildingType, int tx, int ty) {
		return xvr.getBwapi().canBuildHere(builder.getID(), tx, ty,
				buildingType.getUnitTypes().ordinal(), false);
		// && isBuildTileFreeFromUnits(builder.getID(), tx, ty)
	}

	public static void constructAddOn(Unit buildingWithNoAddOn, UnitTypes buildingType) {
		if (buildingWithNoAddOn == null || buildingType == null) {
			return;
		}
		xvr.getBwapi().buildAddon(buildingWithNoAddOn.getID(), buildingType.ordinal());
	}

}

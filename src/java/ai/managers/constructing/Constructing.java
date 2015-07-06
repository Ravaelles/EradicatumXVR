package ai.managers.constructing;

import jnibwapi.JNIBWAPI;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.util.BWColor;
import ai.core.Painter;
import ai.core.XVR;
import ai.handling.map.MapPoint;
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

	private static final int MAX_SEARCH_RANGE = 50; // Max dist from base for
													// building

	// =========================================================

	public static boolean DEBUG_CONSTRUCTION_MODE = false;
	public static boolean debugConstruction = false;

	// private static int _skipCheckingForTurns = 0;

	// =========================================================

	public static void construct(UnitTypes building) {
		// xvr.getClient().sendText("Build: " + building.name());
		// System.err.println("Build: " + building.name());

		// Define tile where to build according to type of building.
		MapPoint buildTile = getTileAccordingToBuildingType(building);

		// Check if build tile is okay.
		if (buildTile != null && !isConstructionDuplicated(building)) {
			// Painter.message(xvr, "Construct: " + building.name());
			constructBuilding(building, buildTile);
		}
	}

	private static boolean isConstructionDuplicated(UnitTypes building) {
		// boolean canProceed = false;
		//
		// // Disallow multiple building of all buildings, except barracks,
		// // bunkers.
		// if (building.getType().isBarracks() || building.getType().isFactory()
		// || building.getType().isBase()) {
		// int builders =
		// ifWeAreBuildingItCountHowManyWorkersIsBuildingIt(building);
		// int numOfBuildings =
		// UnitCounter.getNumberOfUnits(building.getType().getUnitTypes());
		// if (numOfBuildings != 1) {
		// canProceed = builders == 0;
		// }
		// if (numOfBuildings == 1) {
		// canProceed = builders <= 1;
		// }
		// } else {
		// canProceed = !weAreBuilding(building);
		// }

		return weAreBuilding(building);
	}

	// =========================================================

	// public static boolean canBuildHere(Unit builder, UnitType buildingType,
	// int tx, int ty) {
	// // boolean onlyExplored = !buildingType.isBase();
	// return xvr.getBwapi().canBuildHere(builder.getID(), tx, ty,
	// buildingType.getUnitTypes().ordinal(), true);
	// }

	private static void issueBuildOrder(Unit builder, MapPoint buildTile, UnitTypes building) {
		xvr.getBwapi().build(builder.getID(), buildTile.getTx(), buildTile.getTy(), building.ordinal());
		ConstructingHelper.addInfoAboutConstruction(building, builder, buildTile);
	}

	public static MapPoint getLegitTileToBuildNear(UnitTypes buildingType, MapPoint point, int minSearchRadius,
			int maxSearchRadius) {
		UnitType type = buildingType.getType();

		boolean isSpecialBuilding = type.isBase() || type.isBunker() || type.isRefinery();

		// =========================================================
		// Try to find possible place to build starting in given point and
		// gradually increasing search radius
		int currSearchRadius = minSearchRadius;

		// int minTileX = Math.max(3, point.getTx() - currSearchRadius);
		// int maxTileX = Math.min(xvr.getMap().getWidth() - 3, point.getTx() +
		// currSearchRadius);
		// if (maxTileX < minTileX) {
		// maxTileX = minTileX;
		// }
		//
		// int minTileY = Math.max(3, point.getTy() - currSearchRadius);
		// int maxTileY = Math.min(xvr.getMap().getHeight() - 3, point.getTy() +
		// currSearchRadius);
		// if (maxTileY < minTileY) {
		// maxTileY = minTileY;
		// }

		// if (!isSpecialBuilding) {
		// currSearchRadius = 15 + (int) (xvr.getUnitsBuildings().size() / 2);
		// }

		// Start looking for a place to build this building in radius of given
		// arbitrary point. If you can't find point, increase search radius.
		ConstructionPlaceFinder.lastError = null;
		while (currSearchRadius <= maxSearchRadius) {
			int minTileX = point.getTx() - currSearchRadius;
			int maxTileX = point.getTx() + currSearchRadius;
			int minTileY = point.getTy() - currSearchRadius;
			int maxTileY = point.getTy() + currSearchRadius;

			ConstructionPlaceFinder.lastRadius = currSearchRadius;
			for (int tileX = minTileX; tileX <= maxTileX; tileX += 1) {

				// Leave space on some rows/columns so unit can have corridors
				if (!isSpecialBuilding && tileX % 7 == 0) {
					continue;
				}

				for (int tileY = minTileY; tileY <= maxTileY; tileY += 1) {

					// Leave space on some rows/columns so unit can have
					// corridors
					if (!isSpecialBuilding && tileY % 5 == 0) {
						continue;
					}

					// Check if it's possible and reasonable to build this type
					// of building in this place
					MapPoint position = ConstructionPlaceFinder.shouldBuildHere(type, tileX, tileY);
					if (position != null) {

						// Code for debugging: paint this build position as
						// green/red
						if (DEBUG_CONSTRUCTION_MODE && debugConstruction) {
							int x = tileX * 32;
							int y = tileY * 32;
							Painter.paintBuildingPosition(type, x, y, BWColor.GREEN, "OK");
						}

						return position;
					}

					// Code for debugging: paint this build position as
					// green/red
					// else {
					// if (DEBUG_CONSTRUCTION_MODE && debugConstruction) {
					// int x = tileX * 32;
					// int y = tileY * 32;
					// paintBuildingPosition(type, x, y, BWColor.RED, "BAD ");
					// }
					// }
				}
			}

			currSearchRadius += 1;
		}

		return null;
	}

	// =========================================================

	private static MapPoint getTileAccordingToBuildingType(UnitTypes building) {
		MapPoint buildTile = null;
		boolean disableReportOfNoPlaceFound = false;

		// Bunker
		if (TerranBunker.getBuildingType().ordinal() == building.ordinal()) {
			buildTile = TerranBunker.findTileForBunker();
		}

		// Supply Depot
		// if (TerranSupplyDepot.getBuildingType().ordinal() ==
		// building.ordinal()) {
		// buildTile = TerranSupplyDepot.findTileForDepot();
		// }

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
			// if (_skipCheckingForTurns > 0) {
			// _skipCheckingForTurns--;
			// return null;
			// }
			buildTile = findTileForStandardBuilding(building);
		}

		if (buildTile == null && !disableReportOfNoPlaceFound) {
			System.out.println("# No tile found for: " + building.getType().getName() + " // error: "
					+ ConstructionPlaceFinder.lastError + " // search radius: " + ConstructionPlaceFinder.lastRadius);
		} else {
			// System.out.println(building.name() + ": " + buildTile + " // search radius: "
			// + ConstructionPlaceFinder.lastRadius);
		}

		return buildTile;
	}

	// =========================================================

	public static MapPoint findTileForStandardBuilding(UnitTypes typeToBuild) {

		// There is a nasty bug: when we're losing badly Terran Barracks are
		// slowing down game terribly; try to limit search range.
		// int MAX_RANGE = 60;
		// if (xvr.getTimeSeconds() > 400
		// && typeToBuild.ordinal() == UnitTypes.Terran_Barracks.ordinal()) {
		// MAX_RANGE = 20;
		// }

		// Unit base = xvr.getFirstBase();
		Unit base = xvr.getRandomBase();
		if (base == null) {
			return null;
		}

		MapPoint tile = Constructing.getLegitTileToBuildNear(typeToBuild, base.translate(96, 0), 6, MAX_SEARCH_RANGE);

		return tile;
	}

	/**
	 * @return if we need to build some building it will return non-null value, being int array containing three
	 *         elements: first is total amount of minerals required all buildings that we need to build, while second is
	 *         total amount of gas required and third returns total number of building types that we want to build. If
	 *         we don't need to build anything right now it returns null
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

	public static MapPoint findBuildTile(int builderID, UnitTypes type, MapPoint place) {
		return findBuildTile(builderID, type.ordinal(), place.getX(), place.getY());
	}

	public static MapPoint findBuildTile(int builderID, int buildingTypeID, int x, int y) {
		MapPoint tileToBuild = findTileForStandardBuilding(UnitType.getUnitTypesByID(buildingTypeID));

		if (tileToBuild == null) {
			JNIBWAPI bwapi = xvr.getBwapi();
			bwapi.printText("Unable to find tile for new " + bwapi.getUnitType(buildingTypeID).getName());
		}
		return tileToBuild;
	}

	// public static MapPoint getLegitTileToBuildNear(UnitTypes type, MapPoint
	// nearTo,
	// int minimumDist, int maximumDist) {
	// Unit worker = xvr.getNearestWorkerTo(nearTo);
	// if (worker == null || type == null) {
	// return null;
	// }
	// return getLegitTileToBuildNear(type.ordinal(), nearTo.getTx(),
	// nearTo.getTy(), minimumDist,
	// maximumDist);
	// }

	// public static MapPoint getLegitTileToBuildNear(UnitTypes type, MapPoint
	// nearTo,
	// int minimumDist, int maximumDist) {
	// if (nearTo == null || type == null) {
	// return null;
	// }
	// return getLegitTileToBuildNear(type.ordinal(), nearTo.getTx(),
	// nearTo.getTy(), minimumDist,
	// maximumDist);
	// }
	//
	// public static MapPoint getLegitTileToBuildNear(UnitTypes type, int tileX,
	// int tileY,
	// int minimumDist, int maximumDist, boolean requiresPower) {
	// if (worker == null || type == null) {
	// return null;
	// }
	// return getLegitTileToBuildNear(worker.getID(), type.ordinal(), tileX,
	// tileY, minimumDist,
	// maximumDist);
	// }

	public static boolean isBuildTileFullyBuildableFor(int builderID, int i, int j, int buildingTypeID) {
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

	protected static boolean constructBuilding(UnitTypes building, MapPoint buildTile) {
		if (buildTile == null) {
			return false;
		}

		Unit workerUnit = xvr.getOptimalBuilder(buildTile);
		if (workerUnit != null) {

			// if we found a good build position, and we aren't already
			// constructing this building order our worker to build it
			// && (!xvr.weAreBuilding(building))
			if (buildTile != null) {
				// Debug.messageBuild(building);
				issueBuildOrder(workerUnit, buildTile, building);

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
		return ConstructingHelper.weAreBuilding(type);
	}

	public static void constructAddOn(Unit buildingWithNoAddOn, UnitTypes buildingType) {
		if (buildingWithNoAddOn == null || buildingType == null) {
			return;
		}
		xvr.getBwapi().buildAddon(buildingWithNoAddOn.getID(), buildingType.ordinal());
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

}

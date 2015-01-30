package ai.managers.constructing;

import jnibwapi.JNIBWAPI;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitCounter;
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

	// =========================================================

	public static void construct(XVR xvr, UnitTypes building) {

		// Define tile where to build according to type of building.
		MapPoint buildTile = getTileAccordingToBuildingType(building);

		// Check if build tile is okay.
		if (buildTile != null) {
			constructBuilding(xvr, building, buildTile);
		}
	}

	// =========================================================

	public static boolean canBuildHere(Unit builder, UnitType buildingType, int tx, int ty) {
		boolean onlyExplored = !buildingType.isBase();
		return xvr.getBwapi().canBuildHere(builder.getID(), tx, ty,
				buildingType.getUnitTypes().ordinal(), onlyExplored);
	}

	private static void build(Unit builder, MapPoint buildTile, UnitTypes building) {
		boolean canProceed = false;

		// Disallow multiple building of all buildings, except barracks,
		// bunkers.
		if (building.getType().isBarracks() || building.getType().isFactory()
				|| building.getType().isBase()) {
			int builders = ifWeAreBuildingItCountHowManyWorkersIsBuildingIt(building);
			int numOfBuildings = UnitCounter.getNumberOfUnits(building.getType().getUnitTypes());
			if (numOfBuildings != 1) {
				canProceed = builders == 0;
			}
			if (numOfBuildings == 1) {
				canProceed = builders <= 1;
			}
		} else {
			canProceed = !weAreBuilding(building);
		}

		// If there aren't multiple orders to build one building given, we can
		// proceed
		if (canProceed) {
			xvr.getBwapi().build(builder.getID(), buildTile.getTx(), buildTile.getTy(),
					building.ordinal());
			ConstructingHelper.addInfoAboutConstruction(building, builder, buildTile);
		}
	}

	public static MapPoint getLegitTileToBuildNear(int builderID, int buildingTypeID, int tileX,
			int tileY, int minimumDist, int maximumDist) {
		UnitType type = UnitType.getUnitTypeByID(buildingTypeID);

		boolean isSpecialBuilding = type.isBase() || type.isBunker() || type.isRefinery();

		// =========================================================
		// Try to find possible place to build starting in given point and
		// gradually increasing search radius
		int currentDist = minimumDist;
		// System.out.println();
		// System.out.println("TILE_X = " + tileX);
		// System.out.println("TILE_Y = " + tileY);
		while (currentDist <= maximumDist) {
			int step = Math.max(2 * currentDist, 1);
			for (int i = tileX - currentDist; i <= tileX + currentDist; i += step) {
				if (!isSpecialBuilding && i % 5 == 0) {
					continue;
				}

				// for (int j = tileY - currentDist; j <= tileY + currentDist;
				// j++) {
				for (int j = tileY - currentDist; j <= tileY + currentDist; j++) {
					// System.out.println(i + ", " + j);
					if (!isSpecialBuilding && j % 7 == 0) {
						continue;
					}

					// Draw base position as rectangle
					// int x = i * 32;
					// int y = j * 32;
					// xvr.getBwapi().drawBox(x, y,
					// x + type.getDimensionLeft() + type.getDimensionRight(),
					// y + type.getDimensionUp() + type.getDimensionDown(),
					// BWColor.TEAL,
					// false, false);
					// xvr.getBwapi().drawText(x, y + 3,
					// BWColor.getToStringHex(BWColor.GREEN) + type.getName(),
					// false);

					MapPoint position = ConstructionPlaceFinder.shouldBuildHere(type, i, j);
					if (position != null) {
						return position;
					}
				}
			}

			currentDist++;

			if (currentDist > 42) {
				break;
			}
		}

		return null;
	}

	// =========================================================

	public static MapPoint findTileForStandardBuilding(UnitTypes typeToBuild) {

		// There is a nasty bug: when we're losing badly Terran Barracks are
		// slowing down game terribly; try to limit search range.
		int MAX_RANGE = 70;
		if (xvr.getTimeSeconds() > 400
				&& typeToBuild.ordinal() == UnitTypes.Terran_Barracks.ordinal()) {
			MAX_RANGE = 20;
		}

		// Unit base = xvr.getFirstBase();
		Unit base = xvr.getRandomBase();
		if (base == null) {
			return null;
		}

		MapPoint tile = Constructing.getLegitTileToBuildNear(xvr.getRandomWorker(), typeToBuild,
				base.translate(96, -48), 3, MAX_RANGE);

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
			// System.out.println("       buildTile = " +
			// buildTile.toStringLocation());
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
		return ConstructingHelper.weAreBuilding(type);
	}

	public static Unit getRandomWorker() {
		return xvr.getRandomWorker();
	}

	public static void constructAddOn(Unit buildingWithNoAddOn, UnitTypes buildingType) {
		if (buildingWithNoAddOn == null || buildingType == null) {
			return;
		}
		xvr.getBwapi().buildAddon(buildingWithNoAddOn.getID(), buildingType.ordinal());
	}

}

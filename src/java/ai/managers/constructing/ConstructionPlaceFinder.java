package ai.managers.constructing;

import java.util.ArrayList;

import jnibwapi.JNIBWAPI;
import jnibwapi.model.ChokePoint;
import jnibwapi.model.Region;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.units.UnitManager;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranSupplyDepot;

public class ConstructionPlaceFinder {

	private static final String SUCCESS = "SUCCESS";
	private static final String ERROR_TOO_NEAR_MINERALS_OR_GEYSER = "TOO_NEAR_MINERALS_OR_GEYSER";
	private static final String ERROR_NOT_ENOUGH_PLACE_TO_OTHER_BUILDINGS = "NOT_ENOUGH_PLACE_TO_OTHER_BUILDINGS";
	private static final String ERROR_OVERLAPS_NEXT_BASE = "OVERLAPS_NEXT_BASE";
	private static final String ERROR_TOO_CLOSE_TO_CHOKEPOINT = "TOO_CLOSE_TO_CHOKEPOINT";
	private static final String ERROR_INVALID_REGION = "INVALID_REGION";

	// =========================================================

	protected static String lastError = null;
	protected static int lastRadius = -1;

	// =========================================================

	public static MapPoint shouldBuildHere(UnitType type, int tileX, int tileY) {
		boolean isBase = type.isBase();
		// boolean isDepot = type.isSupplyDepot();

		boolean skipCheckingIsFreeFromUnits = false;
		boolean skipCheckingRegion = xvr.getTimeSeconds() > 380 || isBase || type.isBunker()
				|| type.isMissileTurret() || type.isAddon();

		// =========================================================

		// int buildingPixelWidth = type.getDimensionLeft() +
		// type.getDimensionRight();
		// int buildingPixelHeight = type.getDimensionUp() +
		// type.getDimensionDown();
		int x = tileX * 32;
		int y = tileY * 32;
		// int x = tileX * 32 - buildingPixelWidth / 2;
		// int y = tileY * 32 - buildingPixelHeight / 2;
		MapPointInstance place = new MapPointInstance(x, y);

		// Is it physically possible to build here?
		if (canBuildAt(place, type)) {

			// If it's possible to build here, now check whether it
			// makes sense. If it's not in stupid place or colliding
			// etc.
			Unit builderUnit = xvr.getNearestWorkerTo(place);
			if (builderUnit != null
					&& (skipCheckingIsFreeFromUnits || isBuildTileFreeFromUnits(type,
							builderUnit.getID(), tileX, tileY))) {

				// if ((isBase || !isTooNearMineralsOrGeyser(type, place))
				// && (isBase || isEnoughPlaceToOtherBuildings(place, type))
				// && (isBase || !isOverlappingNextBase(place, type))
				// && (isBase || !isTooCloseToAnyChokePoint(place))
				// && (isBase || skipCheckingRegion ||
				// isInAllowedRegions(place))) {
				// return place;
				// }

				// We should avoid building place:
				// - between workers and minerals
				// - right next to another building
				// - in the place where next base should be built
				// - too close to a chokepoint (passage problem)
				// - that are in other region (wrong neighborhood)

				if (!isBase && !type.isBunker() && isTooNearMineralsOrGeyser(type, place)) {
					lastError = ERROR_TOO_NEAR_MINERALS_OR_GEYSER;
					return null;
				}

				if (!isBase && !isEnoughPlaceToOtherBuildings(place, type)) {
					lastError = ERROR_NOT_ENOUGH_PLACE_TO_OTHER_BUILDINGS;
					return null;
				}

				if (!isBase && isOverlappingNextBase(place, type)) {
					lastError = ERROR_OVERLAPS_NEXT_BASE;
					return null;
				}

				if (!isBase && isTooCloseToAnyChokePoint(place)) {
					lastError = ERROR_TOO_CLOSE_TO_CHOKEPOINT;
					return null;
				}

				if (!isBase && !skipCheckingRegion && !isInAllowedRegions(place)) {
					lastError = ERROR_INVALID_REGION;
					return null;
				}

				else {
					return place;
				}
			}
		}

		// No place has been found, return null.
		return null;
	}

	// =========================================================
	// Hi-level abstraction methods

	private static boolean isInAllowedRegions(MapPoint place) {
		Region buildTileRegion = xvr.getMap().getRegion(place);

		if (xvr.getFirstBase().getRegion() == null
				|| TerranCommandCenter.getSecondBaseLocation() == null) {
			return true;
		}

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
			return place.distanceToChokePoint(nearestChoke) <= 3.3;
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
		if (place != null && !type.isBase()
				&& UnitCounter.getNumberOfUnits(TerranSupplyDepot.getBuildingType()) >= 1) {
			MapPoint nextBase = TerranCommandCenter.findTileForNextBase(false);
			if (nextBase == null) {
				return false;
			} else {
				int minDistance = type.isBunker() ? 4 : 6;
				return xvr.getDistanceSimple(place, nextBase.translate(96, 0)) <= minDistance;
			}
		} else {
			return false;
		}
	}

	private static boolean isEnoughPlaceToOtherBuildings(MapPoint place, UnitType buildingType) {

		// Base and refinery can be placed anywhere
		if (buildingType.isBase() || buildingType.isOnGeyser()) {
			return true;
		}

		// Define buildings that are near this build tile
		ArrayList<Unit> buildingsNearby = xvr.getUnitsInRadius(place, 8, xvr.getUnitsBuildings());

		for (Unit otherBuilding : buildingsNearby) {

			// // Allow stacking Supply Depots
			// if (buildingType.isSupplyDepot() &&
			// otherBuilding.isSupplyDepot()) {
			// continue;
			// }
			//
			// // Disallow stacking
			// else {
			// if (!canBuildAt(place.translateSafe(-1, -1), buildingType)) {
			// return false;
			// }
			//
			// if (!canBuildAt(place.translateSafe(1, 1), buildingType)) {
			// return false;
			// }
			//
			Unit builder = xvr.getNearestWorkerTo(place);
			if (wouldNewBuildingCollideWith(place, buildingType, otherBuilding, builder)) {
				return false;
			}
			// }
		}

		// No building collides, allow this building location
		return true;

		// if (type.isBase() || type.isOnGeyser()) {
		// return true;
		// }
		//
		// // ==============================
		// // Define building dimensions
		// int wHalf = type.getTileWidth() * 32;
		// int hHalf = type.getTileHeight() * 32;
		// int maxDimension = wHalf > hHalf ? wHalf : hHalf;
		//
		// // ==============================
		// // Define center of the building
		// MapPoint center = new MapPointInstance(place.getX() + wHalf,
		// place.getY() + hHalf);
		//
		// // Define buildings that are near this build tile
		// ArrayList<Unit> buildingsNearby = xvr.getUnitsInRadius(center, 8,
		// xvr.getUnitsBuildings());
		//
		// // If this building can have an Add-On, it is essential we keep place
		// // for it.
		// int spaceBonus = 0;
		// if (type.canHaveAddOn()) {
		// spaceBonus += 1;
		// center = center.translate(64, 0);
		// }
		//
		// // For each building nearby define if it's not too close to this
		// build
		// // tile. If so, reject this build tile.
		// for (Unit unit : buildingsNearby) {
		// if (unit.isLifted() || unit.isSupplyDepot() || !unit.isExists()) {
		// continue;
		// }
		//
		// // Supply Depots can be really close to each other, but only if
		// // there're few of them
		// if (type.isSupplyDepot()
		// && (xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Supply_Depot,
		// 5, place,
		// true) <= 2 || xvr.countUnitsOfGivenTypeInRadius(
		// UnitTypes.Terran_Supply_Depot, 8, place, true) <= 3)) {
		// continue;
		// }
		//
		// // Also: don't build in the place where there COULD BE Add-On for a
		// // different, already existing building
		// int dx = 0;
		// int bonus = spaceBonus;
		// UnitType unitType = unit.getType();
		// if (unitType.canHaveAddOn() && !unit.hasAddOn()) {
		// // bonus++;
		// dx = 64;
		// }
		// if (unitType.isBase()) {
		// // dx += 96;
		// bonus += 5;
		// }
		//
		// // If this building is too close to our build tile, indicate this
		// // fact.
		// if (type.isBuilding() && !unit.isLifted()
		// && unit.translate(dx, 0).distanceTo(center) <= maxDimension + bonus)
		// {
		// return false;
		// }
		// }
		// return true;
	}

	private static boolean wouldNewBuildingCollideWith(MapPoint buildingPlace,
			UnitType buildingType, Unit existingBuilding, Unit builder) {

		// Consider space for add-ons
		if (buildingType.canHaveAddOn() || existingBuilding.getType().canHaveAddOn()) {
			if (!isPhysicallyPossibleToBuildAt(builder, buildingPlace.translate(64, 0),
					buildingType)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isTooNearMineralsOrGeyser(UnitType type, MapPoint point) {
		// if (type.canHaveAddOn()) {
		// point = point.translate(64, 0);
		// }
		Unit nearestBase = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, point);
		double distToBase = nearestBase.distanceTo(point);

		// =========================================================

		if (type.isOnGeyser()) {
			return false;
		}

		// =========================================================
		// Check if isn't too near to geyser
		// MapPoint nearestGeyser = xvr.getUnitNearestFromList(point,
		// xvr.getGeysersUnits());
		// if (nearestGeyser != null) {
		// if (distToBase <= 6 && nearestGeyser.distanceTo(point) <= 4.9) {
		// return true;
		// }
		// }

		// Unit nearestBase = xvr.getUnitOfTypeNearestTo(UnitManager.BASE,
		// point);
		// if (distToGeyser <= 5 + minDistBonus) {
		// double distBaseToGeyser = xvr.getDistanceBetween(nearestBase,
		// nearestGeyser);
		// if (distBaseToGeyser >= distToGeyser + minDistBonus) {
		// return false;
		// }
		// }

		// =========================================================
		// Check if isn't too near to mineral
		Unit nearestMineral = xvr.getUnitNearestFromList(point, xvr.getMineralsUnits());

		if (nearestMineral != null) {
			double distToMineral = nearestMineral.distanceTo(point);

			if (distToMineral <= 3.7 && distToBase <= 6) {
				return true;
			}
		}

		return false;
	}

	public static boolean isBuildTileFreeFromUnits(UnitType type, int builderID, int tileX,
			int tileY) {
		JNIBWAPI bwapi = XVR.getInstance().getBwapi();
		// MapPointInstance point = new MapPointInstance((int) (tileX - 1.5) *
		// 32, (tileY - 1) * 32);
		MapPointInstance point = new MapPointInstance((int) (tileX) * 32, (tileY) * 32);

		double buildingApprxDimension = type.getTileWidth() + 0.5;

		// Check if units are blocking this tile
		boolean unitsInWay = false;
		for (Unit u : bwapi.getAllUnits()) {
			if (u.getID() == builderID) {
				continue;
			}
			if (xvr.getDistanceBetween(u, point) <= buildingApprxDimension) {
				// for (Unit unit : xvr.getUnitsInRadius(point, 4,
				// xvr.getBwapi().getMyUnits())) {
				// UnitActions.moveAwayFromUnitIfPossible(unit, point, 6);
				// }
				unitsInWay = true;
			}
		}

		return !unitsInWay;
	}

	// =========================================================
	// Lo-level abstraction methods

	public static boolean canBuildAt(MapPoint point, UnitType type) {
		Unit builder = xvr.getNearestWorkerTo(point);
		if (builder == null || point == null) {
			return false;
		}

		// boolean checkExplored = shouldCheckOnlyExplored(type);
		boolean checkExplored = false;

		// Buildings that can have an add-on, must have additional space on
		// their right
		// if (type.canHaveAddOn() && !type.isBase()) {
		// // builder.getID(),
		// if (!xvr.getBwapi().canBuildHere(builder.getID(), point.getTx() + 2,
		// point.getTy(),
		// type.getUnitTypes().getID(), checkExplored)) {
		// return false;
		// }
		// }

		// builder.getID(),
		return xvr.getBwapi().canBuildHere(point.getTx(), point.getTy(),
				type.getUnitTypes().getID(), checkExplored);
	}

	private static boolean shouldCheckOnlyExplored(UnitType type) {
		return !type.isBase() && !type.isBunker();
	}

	public static boolean isPhysicallyPossibleToBuildAt(Unit builder, MapPoint point, UnitType type) {
		// boolean checkExplored = shouldCheckOnlyExplored(type);
		boolean checkExplored = false;
		// builder.getID(),
		return xvr.getBwapi().canBuildHere(point.getTx(), point.getTy(),
				type.getUnitTypes().getID(), checkExplored);
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

}

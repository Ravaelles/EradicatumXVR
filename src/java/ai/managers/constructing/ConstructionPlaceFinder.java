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

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static MapPoint shouldBuildHere(UnitType type, int i, int j) {
		boolean isBase = type.isBase();
		boolean isDepot = type.isSupplyDepot();

		boolean skipCheckingIsFreeFromUnits = false;
		boolean skipCheckingRegion = xvr.getTimeSeconds() > 250 || isBase || type.isBunker()
				|| type.isMissileTurret() || type.isAddon();

		// =========================================================

		// if (isDepot && (j % 2 != 0 || j % 6 == 0)) {
		// continue;
		// }
		int x = i * 32;
		int y = j * 32;
		MapPointInstance place = new MapPointInstance(x, y);

		// Is it physically possibly to build here?
		if (canBuildAt(place, type)) {

			// If it's possible to build here, now check whether it
			// makes sense. If it's not in stupid place or colliding
			// etc.
			Unit builderUnit = xvr.getRandomWorker();
			if (builderUnit != null
					&& (skipCheckingIsFreeFromUnits || isBuildTileFreeFromUnits(
							builderUnit.getID(), i, j))) {

				// We should avoid building place:
				// - between workers and minerals
				// - right next to other building
				// - in the place where next base should be built
				// - too close to a chokepoint (passage problem)
				// - that are in other region (wrong neighborhood)
				if ((isBase || !isTooNearMineralsOrGeyser(type, place))
						&& (isBase || isEnoughPlaceToOtherBuildings(place, type))
						&& (isBase || !isOverlappingNextBase(place, type))
						&& (isBase || !isTooCloseToAnyChokePoint(place)
								&& (isBase || skipCheckingRegion || isInAllowedRegions(place)))) {
					return place;
				}
			}
		}

		// No place were found, return null.
		return null;
	}

	// =========================================================
	// Hi-level abstraction methods

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
				return xvr.getDistanceSimple(place, nextBase.translate(62, 48)) <= 6;
			}
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
			center = center.translate(32, 0);
		}

		// For each building nearby define if it's not too close to this build
		// tile. If so, reject this build tile.
		for (Unit unit : buildingsNearby) {
			if (unit.isLifted()) {
				continue;
			}

			// Supply Depots can be really close to each other, but only if
			// there're few of them
			// if (isDepot
			// && type.isSupplyDepot()
			// &&
			// xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Supply_Depot,
			// 5, place,
			// true) <= 2
			// &&
			// xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Supply_Depot,
			// 9, place,
			// true) <= 3) {
			// continue;
			// }

			// Also: don't build in the place where there COULD BE Add-On for a
			// different, already existing building
			int dx = 0;
			int bonus = spaceBonus;
			UnitType unitType = unit.getType();
			if (type.canHaveAddOn() && !unit.hasAddOn()) {
				// bonus++;
				dx = 32;
			}
			if (unitType.isBase()) {
				dx += 32;
				bonus += 4;
			}

			// If this building is too close to our build tile, indicate this
			// fact.
			if (type.isBuilding() && !unit.isLifted()
					&& unit.translate(dx, 0).distanceTo(center) <= maxDimension + 0.5 + bonus) {
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

	// =========================================================
	// Lo-level abstraction methods

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

}

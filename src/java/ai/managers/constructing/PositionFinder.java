package ai.managers.constructing;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;

public class PositionFinder {

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static MapPoint shouldBuildHere(UnitType type, int i, int j) {
		boolean isBase = type.isBase();
		boolean isDepot = type.isSupplyDepot();

		boolean skipCheckingIsFreeFromUnits = false;
		boolean skipCheckingRegion = xvr.getTimeSeconds() > 250 || isBase || type.isBunker() || type.isMissileTurret()
				|| type.isAddon();

		// =========================================================

		if (isDepot && (j % 2 != 0 || j % 6 == 0)) {
			continue;
		}
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
					&& (skipCheckingIsFreeFromUnits || isBuildTileFreeFromUnits(builderUnit.getID(), i, j))) {

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
	}

	// =========================================================

	private static boolean canBuildAt(MapPoint point, UnitType type) {
		Unit randomWorker = xvr.getRandomWorker();
		if (randomWorker == null || point == null) {
			return false;
		}

		// Buildings that can have an add-on, must have additional space on
		// their right
		if (type.canHaveAddOn() && !type.isBase()) {
			if (!xvr.getBwapi().canBuildHere(randomWorker.getID(), point.getTx() + 2, point.getTy(),
					type.getUnitTypes().getID(), false)) {
				return false;
			}
		}
		return xvr.getBwapi().canBuildHere(randomWorker.getID(), point.getTx(), point.getTy(),
				type.getUnitTypes().getID(), false);
	}

}

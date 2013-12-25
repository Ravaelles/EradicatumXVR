package ai.handling.army;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.units.UnitManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;

public class ArmyPlacing {

	private static XVR xvr = XVR.getInstance();

	public static MapPoint getArmyGatheringPointFor(Unit unit) {
		int bases = UnitCounter.getNumberOfUnits(UnitManager.BASE);

		MapPoint runTo = null;

		// If only one base, then go to nearest cannon
		if (bases == 1) {
			MapPoint base = TerranCommandCenter.getSecondBaseLocation();
			runTo = base;

			Unit bunker = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), base);
			if (bunker != null) {
				// UnitActions.loadUnitInto(unit, bunker);
				// return null;
				runTo = bunker;
			}

			if (runTo == null) {
				Unit barracks = xvr.getUnitOfTypeNearestTo(TerranBarracks.getBuildingType(), base);
				if (barracks != null) {
					runTo = barracks;
				}
			}
		}

		// Try to go to the base nearest to enemy
		else if (bases > 1) {

			// If there's stub for new base, go there
			if (xvr.countUnitsInRadius(TerranCommandCenter.getTileForNextBase(false), 10, true) >= 2) {
				runTo = TerranCommandCenter.getTileForNextBase(false);
			} else {
				Unit baseNearestToEnemy = xvr.getBaseNearestToEnemy();
				if (baseNearestToEnemy.equals(xvr.getFirstBase())) {
					runTo = xvr.getLastBase();
				} else {
					runTo = baseNearestToEnemy;
				}
			}

			// Try to find a bunker new new base
			Unit nearestBunker = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), runTo);
			if (nearestBunker != null && nearestBunker.distanceTo(runTo) < 10) {
				runTo = nearestBunker;
			}
		}

		if (runTo == null) {
			if (!TerranBarracks.getAllObjects().isEmpty()) {
				runTo = TerranBarracks.getAllObjects().get(0);
			}
		}
		
		// ==================================
		
		unit.setProperPlaceToBe(runTo);

		if (runTo == null) {
			return null;
		} else {
			return new MapPointInstance(runTo.getX(), runTo.getY()).translate(-4, 0);
		}
	}

	public static void goToSafePlaceIfNotAlreadyThere(Unit unit) {

		// First, just escape.
		MapPoint safePlace = getArmyGatheringPointFor(unit);
		if (safePlace == null) {
			return;
		}

		if (xvr.getDistanceSimple(unit, safePlace) >= 30) {
			UnitActions.moveTo(unit, safePlace);
		} else {
			UnitActions.attackTo(unit, safePlace);
		}
	}

	public static MapPoint getArmyCenterPoint() {
		int totalX = 0;
		int totalY = 0;
		int counter = 0;
		for (Unit unit : xvr.getUnitsNonWorker()) {
			totalX += unit.getX();
			totalY += unit.getY();
			counter++;
			if (counter > 10) {
				break;
			}
		}
		return new MapPointInstance((int) ((double) totalX / counter),
				(int) ((double) totalY / counter));
	}

}

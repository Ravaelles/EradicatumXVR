package ai.handling.army;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitActions;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranSiegeTank;

public class ArmyPlacing {

	private static XVR xvr = XVR.getInstance();

	public static MapPoint getArmyGatheringPointFor(Unit unit) {
		MapPoint secondBaseLocation = TerranCommandCenter.getSecondBaseLocation();

		// Initially, go to the second base location
		MapPoint runTo = secondBaseLocation;

		// =====================================================
		// Define bunker where most of the units should head to.
		runTo = defineRendezvousBunkerIfPossible();

		// =====================================================
		// Try to go to the barracks if no bunker available
		if (runTo == null) {
			runTo = defineRendezvousBarracksIfPossible();
		}

		// ==================================
		// Tell the unit where to be
		unit.setProperPlaceToBe(runTo);

		if (runTo == null) {
			return null;
		} else {
			return new MapPointInstance(runTo.getX(), runTo.getY()).translate(-4, 0);
		}
	}

	private static MapPoint defineRendezvousBarracksIfPossible() {
		Unit barracks = xvr.getUnitOfTypeNearestTo(TerranBarracks.getBuildingType(),
				TerranCommandCenter.getSecondBaseLocation());
		if (barracks != null) {
			return barracks;
		} else {
			return null;
		}
	}

	private static MapPoint defineRendezvousBunkerIfPossible() {
		if (TerranBunker.getNumberOfUnits() == 0) {
			return null;
		}

		Unit nearestEnemyBuilding = MapExploration.getNearestEnemyBuilding();
		MapPoint bunkersNearestTo = nearestEnemyBuilding != null ? nearestEnemyBuilding
				: TerranCommandCenter.getSecondBaseLocation();

		// Get the list of bunkers that are near to the specified point.
		ArrayList<Unit> bunkersNearby = xvr.getUnitsInRadius(bunkersNearestTo, 300,
				xvr.getUnitsOfType(UnitTypes.Terran_Bunker));
		for (Unit bunker : bunkersNearby) {
			return bunker;
		}
		return null;

		// MapPoint bunkerNearThisPoint = MapExploration.getNearestEnemyBase();
		// if (bunkerNearThisPoint == null) {
		// bunkerNearThisPoint = TerranCommandCenter.getSecondBaseLocation();
		// }
		//
		// Unit bunker =
		// xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(),
		// bunkerNearThisPoint);
		// if (bunker != null) {
		// return bunker;
		// } else {
		// return null;
		// }
	}

	public static void goToSafePlaceIfNotAlreadyThere(Unit unit) {
		MapPoint safePlace = null;

		if (unit.shouldFollowTanks()) {
			if (TerranSiegeTank.getNumberOfUnitsCompleted() > 0) {
				Unit tank = xvr.getNearestTankTo(unit);
				safePlace = tank;
			}
		}

		if (safePlace == null) {
			safePlace = getArmyGatheringPointFor(unit);
			if (safePlace == null) {
				return;
			}
		}

		if (xvr.getDistanceSimple(unit, safePlace) >= 4.2) {
			UnitActions.moveTo(unit, safePlace);
		} else {
			// UnitActions.moveTo(unit, safePlace);
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

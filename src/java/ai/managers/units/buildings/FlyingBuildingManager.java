package ai.managers.units.buildings;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.units.UnitActions;
import ai.managers.units.army.FlyerManager;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.managers.units.workers.RepairAndSons;
import ai.terran.TerranBarracks;
import ai.terran.TerranEngineeringBay;
import ai.terran.TerranSiegeTank;

public class FlyingBuildingManager {

	private static final double MAX_ALLOWED_DIST_FROM_TANK = 5.5;
	private static XVR xvr = XVR.getInstance();
	private static Unit flyingBuilding1 = null;
	private static Unit flyingBuilding2 = null;

	// ========================================

	public static void act() {

		// Barracks
		if (shouldHaveFlyingBuilding1()) {
			defineFlyingBuilding1();
			act(flyingBuilding1);
			FlyerManager.tryAvoidingAntiAirUnits(flyingBuilding1);
		}

		// E-Bay
		if (shouldHaveFlyingBuilding2()) {
			defineFlyingBuilding2();
			act(flyingBuilding2);
			FlyerManager.tryAvoidingAntiAirUnits(flyingBuilding2);
		}
	}

	public static void act(Unit flyingBuilding) {
		if (flyingBuilding == null) {
			return;
		}

		if (FlyerManager.tryAvoidingAntiAirUnits(flyingBuilding)) {
			return;
		}
		moveBuildingToProperPlace(flyingBuilding);
		tryRepairingIfNeeded(flyingBuilding);
	}

	private static void tryRepairingIfNeeded(Unit flyingBuilding) {
		if (flyingBuilding.isWounded()) {
			RepairAndSons.issueTicketToRepairIfHasnt(flyingBuilding);
		}
	}

	// ========================================

	private static void moveBuildingToProperPlace(Unit flyingBuilding) {
		Unit medianTank = ArmyRendezvousManager.getRendezvousTankForFlyers();
		if (medianTank != null) {

			// Lift building if isn't lifted yet
			if (!flyingBuilding.isLifted()) {
				xvr.getBwapi().lift(flyingBuilding.getID());
			}

			// If building is close to tank
			if (medianTank.distanceTo(flyingBuilding) < MAX_ALLOWED_DIST_FROM_TANK) {
				if (MapExploration.getNumberOfKnownEnemyBases() > 0) {
					Unit enemyBuilding = MapExploration.getNearestEnemyBuilding();
					UnitActions.moveTo(flyingBuilding, enemyBuilding);
				}
			}

			// Building is far from tank
			else {
				UnitActions.moveTo(flyingBuilding, medianTank);
			}
		}
	}

	private static void defineFlyingBuilding1() {

		// If we haven't decide yet which building should fly
		if (flyingBuilding1 == null || !flyingBuilding1.isExists()) {
			if (TerranBarracks.getNumberOfUnitsCompleted() > 0) {
				Unit buildingToFly = TerranBarracks.getAllObjects().get(0);
				if (buildingToFly.isTraining()) {
					xvr.getBwapi().cancelTrain(buildingToFly.getID(), 0);
				}
				flyingBuilding1 = buildingToFly;
			}
		}
	}

	private static void defineFlyingBuilding2() {

		// Always lift Engineering Bay
		if (TerranEngineeringBay.getNumberOfUnitsCompleted() > 0) {
			flyingBuilding2 = TerranEngineeringBay.getOneNotBusy();
			if (flyingBuilding2 != null && !flyingBuilding2.isLifted()) {
				xvr.getBwapi().lift(flyingBuilding2.getID());
			}
		}
	}

	private static boolean shouldHaveFlyingBuilding1() {
		return TerranSiegeTank.getNumberOfUnits() >= 2;
	}

	private static boolean shouldHaveFlyingBuilding2() {
		return TerranEngineeringBay.getNumberOfUnitsCompleted() > 0;
	}

	public static Unit getFlyingBuilding1() {
		return flyingBuilding1;
	}

	public static Unit getFlyingBuilding2() {
		return flyingBuilding2;
	}

}

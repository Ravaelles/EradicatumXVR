package ai.managers.units.buildings;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.units.army.ArmyCreationManager;
import ai.managers.units.army.FlyerManager;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.managers.units.coordination.ArmyUnitBasicBehavior;
import ai.managers.units.workers.RepairAndSons;
import ai.strategies.TerranOffensiveBunker;
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
		if (shouldHaveFlyingBuilding1() || flyingBuilding1 != null) {
			defineFlyingBuilding1();
			act(flyingBuilding1);
		}

		// E-Bay
		if (shouldHaveFlyingBuilding2() || flyingBuilding2 != null) {
			defineFlyingBuilding2();
			act(flyingBuilding2);
		}
	}

	public static void act(Unit flyingBuilding) {
		if (flyingBuilding == null) {
			return;
		}

		if (FlyerManager.tryAvoidingAntiAirUnits(flyingBuilding)) {
			flyingBuilding.setAiOrder("Avoid AA units");
			return;
		}

		if (ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(flyingBuilding)) {
			flyingBuilding.setAiOrder("Avoid building");
			return;
		}

		// Ask for repairers to come, if needed
		tryRepairingIfNeeded(flyingBuilding);

		// Fly to the proper place
		moveBuildingToProperPlace(flyingBuilding);
	}

	private static void tryRepairingIfNeeded(Unit flyingBuilding) {
		if (flyingBuilding.isWounded()) {
			flyingBuilding.setAiOrder("Ask for repair");
			Unit repairer = RepairAndSons.issueTicketToRepairIfHasnt(flyingBuilding);
			if (repairer != null && !flyingBuilding.isHPAtLeastNPercent(60)) {
				if (repairer.distanceTo(flyingBuilding) > 2) {
					UnitActions.moveTo(flyingBuilding, repairer);
				} else {
					UnitActions.holdPosition(flyingBuilding);
				}
			}
		}
	}

	// ========================================

	private static void moveBuildingToProperPlace(Unit flyingBuilding) {
		MapPoint rendezvousPoint = null;

		if (TerranOffensiveBunker.isStrategyActive()) {
			// rendezvousPoint =
			// MapExploration.getNearestEnemyBuilding(rendezvousPoint);
			int tanks = TerranSiegeTank.getNumberOfUnits();
			if (tanks > 3) {
				Unit enemyBuilding = MapExploration.getNearestEnemyBuilding(rendezvousPoint);
				rendezvousPoint = MapPointInstance.getMiddlePointBetween(
						TerranSiegeTank.getMedianTank(), enemyBuilding);
			} else {
				rendezvousPoint = TerranOffensiveBunker.getTerranOffensiveBunkerPosition();
			}
		} else {
			rendezvousPoint = ArmyRendezvousManager.getRendezvousTankForFlyers();
		}

		if (rendezvousPoint != null) {

			// Lift building if isn't lifted yet
			if (!flyingBuilding.isLifted()) {
				xvr.getBwapi().lift(flyingBuilding.getID());
			}

			// If building is close to tank
			if (rendezvousPoint.distanceTo(flyingBuilding) < MAX_ALLOWED_DIST_FROM_TANK) {
				if (MapExploration.getNumberOfKnownEnemyBases() > 0) {
					Unit enemyBuilding = MapExploration.getNearestEnemyBuilding();
					UnitActions.moveTo(flyingBuilding, enemyBuilding);
				}
			}

			// Building is far from tank
			else {
				UnitActions.moveTo(flyingBuilding, rendezvousPoint);
			}

			flyingBuilding.setAiOrder("They see me flyin' They hatin'");
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
		if (TerranOffensiveBunker.isStrategyActive()) {
			return UnitCounter.getNumberOfInfantryUnits() >= ArmyCreationManager.MINIMUM_MARINES;
		}

		return TerranSiegeTank.getNumberOfUnits() >= 2 && TerranBarracks.MAX_BARRACKS > 1;
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

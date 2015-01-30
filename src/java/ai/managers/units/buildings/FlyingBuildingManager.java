package ai.managers.units.buildings;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.FlyerManager;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.managers.units.workers.RepairAndSons;
import ai.terran.TerranBarracks;
import ai.terran.TerranEngineeringBay;
import ai.terran.TerranFactory;
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

	// =========================================================

	private static boolean shouldHaveFlyingBuilding1() {
		if (TerranFactory.ONLY_TANKS && TerranBarracks.MAX_BARRACKS >= 1) {
			return true;
		}

		return (UnitCounter.getNumberOfInfantryUnitsCompleted() >= 4 || TerranSiegeTank
				.getNumberOfUnitsCompleted() >= 1)
				&& TerranBarracks.MAX_BARRACKS >= 1
				&& TerranBarracks.getOneNotBusy() != null;
	}

	private static boolean shouldHaveFlyingBuilding2() {
		return TerranEngineeringBay.getNumberOfUnitsCompleted() > 0;
	}

	// =========================================================

	private static void moveBuildingToProperPlace(Unit flyingBuilding) {
		if (TerranFactory.ONLY_TANKS) {
			if (StrategyManager.getTargetUnit() != null) {
				UnitActions.moveTo(flyingBuilding, StrategyManager.getTargetUnit());
			} else if (StrategyManager.getTargetPoint() != null) {
				UnitActions.moveTo(flyingBuilding, StrategyManager.getTargetPoint());
			} else {
				if (MapExploration.getNearestEnemyBase() != null) {
					UnitActions.moveTo(flyingBuilding, MapExploration.getNearestEnemyBase());
				}
			}
		}

		// =========================================================

		Unit medianTank = ArmyRendezvousManager.getRendezvousTankForFlyers();
		if (medianTank != null) {

			// Lift building if isn't lifted yet
			if (!flyingBuilding.isLifted()) {
				xvr.getBwapi().lift(flyingBuilding.getID());
			}

			// If building is close to tank
			double maxDistanceBonus = xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Bunker, 8,
					flyingBuilding, true) > 0 ? 3 : 0;
			if (medianTank.distanceTo(flyingBuilding) < MAX_ALLOWED_DIST_FROM_TANK
					+ maxDistanceBonus) {
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

	private static void tryRepairingIfNeeded(Unit flyingBuilding) {
		if (flyingBuilding.isWounded()) {
			RepairAndSons.issueTicketToRepairIfHasnt(flyingBuilding);
		}
	}

	// ========================================

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

	public static Unit getFlyingBuilding1() {
		return flyingBuilding1;
	}

	public static Unit getFlyingBuilding2() {
		return flyingBuilding2;
	}

}

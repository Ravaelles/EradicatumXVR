package ai.managers.units.buildings;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.units.UnitActions;
import ai.terran.TerranBarracks;
import ai.terran.TerranSiegeTank;

public class FlyingBuildingManager {

	private static XVR xvr = XVR.getInstance();
	private static Unit flyingBuilding = null;

	// ========================================

	public static void act() {
		if (shouldHaveFlyingBuilding()) {
			defineFlyingBuilding();
			moveBuildingToProperPlace();
		}
	}

	// ========================================

	private static void moveBuildingToProperPlace() {
		Unit medianTank = TerranSiegeTank.getMedianTank();
		if (medianTank != null) {

			// Lift building if isn't lifted yet
			if (!flyingBuilding.isLifted()) {
				xvr.getBwapi().lift(flyingBuilding.getID());
			}

			// If building is close to tank
			if (medianTank.distanceTo(flyingBuilding) < 3) {
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

	private static void defineFlyingBuilding() {

		// If we haven't decide yet which building should fly
		if (flyingBuilding == null || !flyingBuilding.isExists()) {
			if (TerranBarracks.getNumberOfUnitsCompleted() > 0) {
				Unit buildingToFly = TerranBarracks.getAllObjects().get(0);
				if (buildingToFly.isTraining()) {
					xvr.getBwapi().cancelTrain(buildingToFly.getID(), 0);
				}
				flyingBuilding = buildingToFly;
			}
		}
	}

	private static boolean shouldHaveFlyingBuilding() {
		return TerranSiegeTank.getNumberOfUnits() >= 2;
	}

	public static Unit getFlyingBuilding() {
		return flyingBuilding;
	}

}

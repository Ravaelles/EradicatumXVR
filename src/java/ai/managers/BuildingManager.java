package ai.managers;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.units.UnitActions;

public class BuildingManager {

	// private static final int REPAIR_BUILDING_WITH_WORKERS = 3;
	private static XVR xvr = XVR.getInstance();

	public static void act(Unit building) {
		UnitType buildingType = building.getType();
		if (buildingType == null) { // || !buildingType.isBuilding()
			return;
		}

		handleDamagedBuilding(building);

		// If under attack always call for help
		if (building.isUnderAttack()) {
			UnitActions.callForHelp(building, true);
		}

		// Cancel construction of buildings under attack and severely damaged
		checkIfShouldCancelConstruction(building, buildingType);

		// // Bunker
		// // if (buildingType.isBunker()) {
		// if (building.isUnderAttack()
		// || building.getHitPoints() < building.getInitialHitPoints()) {
		// for (int workerCounter = 0; workerCounter <
		// REPAIR_BUILDING_WITH_WORKERS; workerCounter++) {
		// Unit repairer = WorkerManager.findNearestRepairerTo(building);
		// UnitActions.repair(repairer, building);
		// }
		//
		// if (building.isUnderAttack()) {
		// UnitActions.callForHelp(building);
		// }
		// }
		// // }
	}

	private static void handleDamagedBuilding(Unit building) {
		UnitType buildingType = building.getType();

		// Act only if building is not fully healthy
		if (building.getHitPoints() < buildingType.getMaxHitPoints()) {

			// Define number of repairers for this building
			int numberOfRequiredRepairers = defineNumberOfRepairersFor(building);

			for (int i = 0; i < numberOfRequiredRepairers; i++) {
				Unit repairer = WorkerManager.findNearestRepairerTo(building);
				UnitActions.repair(repairer, building);
			}
		}
	}

	private static int defineNumberOfRepairersFor(Unit building) {
		if (building.getType().isBunker()) {
			int enemiesNearBunker = xvr.getNumberOfUnitsInRadius(building, 11,
					xvr.getEnemyArmyUnits());
			return Math.min(5, Math.min(1, enemiesNearBunker / 3));
		} else {
			return 1;
		}
	}

	private static void checkIfShouldCancelConstruction(Unit building, UnitType buildingType) {
		if (building.isUnderAttack() && (!building.isCompleted())) {
			System.out.println("BUILDING ATTACKED");
			boolean shouldCancelConstruction = false;

			// If this is normal building and it's severely damaged.
			if (building.getHitPoints() < 0.4 * buildingType.getMaxHitPoints()) {
				shouldCancelConstruction = true;
			}

			// If it's base and...
			if (buildingType.isBase()) {

				// // It's lil damaged and there's not too many units nearby.
				// if (building.getHitPoints() < 0.8 * buildingType
				// .getMaxHitPoints()
				// && xvr.getNumberOfUnitsInRadius(building, 10,
				// xvr.getUnitsNonWorker()) < 3) {
				// shouldCancelConstruction = true;
				// }
				//
				// // It's pretty damaged, better scrap it
				// if (building.getHitPoints() < 0.55 * buildingType
				// .getMaxHitPoints()
				// && xvr.getNumberOfUnitsInRadius(building, 10,
				// xvr.getUnitsNonWorker()) < 7) {
				// shouldCancelConstruction = true;
				// }

				// It's pretty damaged, better scrap it
				shouldCancelConstruction = true;
			}

			if (shouldCancelConstruction) {
				System.out.println("TEST CANCEL BASE BUT WILL PROBBALY FAIL");
				xvr.getBwapi().cancelConstruction(building.getID());
			}
		}
	}

}

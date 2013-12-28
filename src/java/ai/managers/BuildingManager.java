package ai.managers;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.managers.units.UnitManager;
import ai.terran.TerranComsatStation;

public class BuildingManager {

	private static XVR xvr = XVR.getInstance();

	public static void act(Unit building) {
		UnitType type = building.getType();
		if (type == null) { // || !buildingType.isBuilding()
			return;
		}

		handleDamagedBuilding(building);

		// If under attack always call for help
		if (building.isUnderAttack()) {
			UnitActions.callForHelp(building, true);
		}

		// Cancel construction of buildings under attack and severely damaged
		handleUnfinishedBuildings(building, type);

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

		if (type.getID() == UnitTypes.Terran_Comsat_Station.ordinal()) {
			TerranComsatStation.act(building);
		}
	}

	private static void handleDamagedBuilding(Unit building) {
		UnitType buildingType = building.getType();

		// Act only if building is not fully healthy
		if (building.getHP() < buildingType.getMaxHitPoints()) {

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
			return Math.min(5, Math.max(1, enemiesNearBunker / 3));
		} else {
			return 1;
		}
	}

	private static void handleUnfinishedBuildings(Unit building, UnitType buildingType) {
		// System.out.println("TEST " + building.isUnderAttack() + " " +
		// !building.isCompleted());
		if (building.isUnderAttack() && !building.isCompleted()) {
			System.out.println("BUILDING ATTACKED");
			boolean shouldCancelConstruction = false;

			// If this is normal building and it's severely damaged.
			if (building.getHP() < 0.4 * buildingType.getMaxHitPoints()) {
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

		// If building still isn't completed check if it has a builder,
		// something might have accidentally killed him, like e.g. a lonely
		// lurker, looking for some love
		if (!building.isCompleted()) {
			Unit builder = getWorkerBuilding(building);
			if (builder == null) {
				System.out.println("###### No builder is building: " + building.getName());
				Unit newBuilder = xvr.getOptimalBuilder(building);
				if (newBuilder != null) {
					UnitActions.rightClick(newBuilder, building);
				}
			}
		}
	}

	private static Unit getWorkerBuilding(Unit building) {
		for (Unit worker : xvr.getUnitsOfType(UnitManager.WORKER)) {
			if (worker.isConstructing() && worker.getBuildUnitID() == building.getID()) {
				return worker;
			}
		}
		return null;
	}
}

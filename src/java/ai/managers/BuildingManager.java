package ai.managers;

import java.util.HashMap;

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

		// Cancel construction of buildings under attack and severely damaged
		handleUnfinishedBuildings(building, type);

		// ===========================================
		// Repairing of building works slightly differently than repairing units
		handleBuildingsNeedingRepair(building);

		// If under attack always call for help
		if (building.isUnderAttack()) {
			UnitActions.callForHelp(building, true);
		}

		// ==========================================
		// Act with SPECIAL BUILDINGS
		if (type.getID() == UnitTypes.Terran_Comsat_Station.ordinal()) {
			TerranComsatStation.act(building);
		}
	}

	private static void handleBuildingsNeedingRepair(Unit building) {
		UnitType buildingType = building.getType();

		int specialCaseRepairersNeeded = isSpecialCaseRepair(building);
		if (specialCaseRepairersNeeded > 0) {
			// Painter.message(xvr,
			// specialCaseRepairersNeeded + " SCV should repair " +
			// building.getName());
			for (int i = 0; i < specialCaseRepairersNeeded; i++) {
				Unit repairer = WorkerManager.findNearestRepairerTo(building);
				UnitActions.repair(repairer, building);
			}
		}

		// Act only if building is not fully healthy
		if (building.getHP() < buildingType.getMaxHitPoints()) {

			// Define number of repairers for this building
			int numberOfRequiredRepairers = defineOptimalNumberOfRepairersFor(building);

			for (int i = 0; i < numberOfRequiredRepairers; i++) {
				Unit repairer = WorkerManager.findNearestRepairerTo(building);
				UnitActions.repair(repairer, building);
			}
		}
	}

	private static int isSpecialCaseRepair(Unit building) {

		// It makes sense to foresee enemy attack on bunker and send repairers
		// before the bunker is actually damaged, otherwise we will never make
		// it
		if (xvr.getTimeSeconds() < 700 && building.getType().isBunker()) {
			int enemiesNearBunker = xvr.countUnitsEnemyInRadius(building, 17);
			if (enemiesNearBunker >= 2) {
				int oursNearBunker = xvr.countUnitsOursInRadius(building, 8);
				if (XVR.isEnemyProtoss()) {
					oursNearBunker /= 2;
				}

				int enemyAdvantage = enemiesNearBunker - oursNearBunker;
				// System.out.println("enemyAdvantage = " + enemyAdvantage);
				return Math.min(1, (int) (enemyAdvantage / 2.5));
			}
		}
		return 0;
	}

	private static int defineOptimalNumberOfRepairersFor(Unit building) {
		if (building.getType().isBunker()) {
			int enemiesNearBunker = xvr.getNumberOfUnitsInRadius(building, 11,
					xvr.getEnemyArmyUnits());
			return Math.min(5, Math.max(1, enemiesNearBunker / 3));
		} else {
			return 1;
		}
	}

	private static HashMap<Unit, Integer> _buildingsInConstructionHP = new HashMap<>();

	private static void handleUnfinishedBuildings(Unit building, UnitType buildingType) {
		// System.out.println("TEST " + building.isUnderAttack() + " " +
		// !building.isCompleted());
		if (!building.isCompleted() && isBuildingAttacked(building)) {
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
			if (builder == null
					|| (builder != null && (!builder.isExists() || !builder.isConstructing()))) {
				// System.out.println("###### No builder is building: " +
				// building.getName());
				Unit newBuilder = xvr.getOptimalBuilder(building);
				if (newBuilder != null) {
					UnitActions.rightClick(newBuilder, building);
				}
			}
		}
	}

	private static boolean isBuildingAttacked(Unit building) {
		if (!_buildingsInConstructionHP.containsKey(building)) {
			_buildingsInConstructionHP.put(building, building.getHP());
			return false;
		} else {
			int oldHP = _buildingsInConstructionHP.get(building);
			_buildingsInConstructionHP.put(building, building.getHP());
			return building.getHP() < oldHP;
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

package ai.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.managers.constructing.Constructing;
import ai.managers.units.UnitManager;
import ai.terran.TerranComsatStation;

public class BuildingManager {

	private static XVR xvr = XVR.getInstance();
	private static HashMap<Unit, Unit> repairersToBuildings = new HashMap<>();
	private static ArrayList<Unit> buildingsNeedingRepair = new ArrayList<>();

	// ========================================

	public static void act(Unit building) {
		UnitType type = building.getType();
		if (type == null) { // || !buildingType.isBuilding()
			return;
		}

		// Remove buildings that have full health
		removeHealthyBuildings();

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

	private static void removeHealthyBuildings() {
		for (Iterator<Unit> iterator = buildingsNeedingRepair.iterator(); iterator.hasNext();) {
			Unit unit = (Unit) iterator.next();
			if (!unit.isWounded()) {
				iterator.remove();
			}
		}

		// for (Unit worker : ) {
		for (Iterator<Unit> iterator = repairersToBuildings.keySet().iterator(); iterator.hasNext();) {
			Unit worker = (Unit) iterator.next();
			Unit building = repairersToBuildings.get(worker);
			if (worker == null || building == null || !worker.isExists() || !worker.isRepairing()
					|| worker.getTargetUnitID() != building.getID()) {
				iterator.remove();
			}
		}
	}

	private static void handleBuildingsNeedingRepair(Unit building) {
		// UnitType buildingType = building.getType();
		int currentRepairers = -1;
		boolean isBuildingDamaged = building.isWounded();

		int specialCaseRepairersNeeded = getSpecialCaseRepairers(building);
		if (specialCaseRepairersNeeded > 0) {
			// Painter.message(xvr,
			// specialCaseRepairersNeeded + " SCV should repair " +
			// building.getName());
			if (currentRepairers == -1) {
				currentRepairers = countNumberOfRepairersForBuilding(building);
			}

			// if (specialCaseRepairersNeeded > 0) {
			// System.out.println("# specialNeeded / current " +
			// specialCaseRepairersNeeded
			// + " / " + currentRepairers);
			// }

			for (int i = 0; i < specialCaseRepairersNeeded - currentRepairers; i++) {
				Unit repairer = WorkerManager.findNearestRepairerTo(building);
				repairBuilding(repairer, building);
			}
		}

		// Act only if building is not fully healthy
		if (isBuildingDamaged) {

			// Define number of repairers for this building
			int numberOfRequiredRepairers = defineOptimalNumberOfRepairersFor(building);
			if (currentRepairers == -1) {
				currentRepairers = countNumberOfRepairersForBuilding(building);
			}

			// if (numberOfRequiredRepairers > 0) {
			// System.out.println("@ Needed / current " +
			// numberOfRequiredRepairers + " / "
			// + currentRepairers);
			// }

			for (int i = 0; i < numberOfRequiredRepairers - currentRepairers; i++) {
				Unit repairer = WorkerManager.findNearestRepairerTo(building);
				repairBuilding(repairer, building);
			}
		}
	}

	private static void repairBuilding(Unit repairer, Unit building) {
		UnitActions.repair(repairer, building);
		repairersToBuildings.put(repairer, building);
		buildingsNeedingRepair.add(building);
	}

	public static int countNumberOfRepairersForBuilding(Unit building) {
		int total = 0;
		for (Unit worker : repairersToBuildings.keySet()) {
			if (repairersToBuildings.get(worker).equals(building)) {
				total++;
			}
		}
		return total;
	}

	public static Unit getBuildingToRepairBy(Unit worker) {
		return repairersToBuildings.get(worker);
	}

	public static int getSpecialCaseRepairers(Unit building) {

		// It makes sense to foresee enemy attack on bunker and send repairers
		// before the bunker is actually damaged, otherwise we will never make
		// it
		if (xvr.getTimeSeconds() < 900 && building.getType().isBunker()) {
			int enemiesNearBunker = xvr.countUnitsEnemyInRadius(building, 16);
			// System.out.println("|" + building.getName() + "|" +
			// building.toStringLocation() + "|"
			// + enemiesNearBunker);
			if (enemiesNearBunker >= 2) {
				int oursNearBunker = xvr.countUnitsOursInRadius(building, 9);
				if (XVR.isEnemyProtoss()) {
					oursNearBunker /= 2;
				}

				// if (enemiesNearBunker > 0) {
				// System.out.println("enemiesNearBunker - oursNearBunker = " +
				// enemiesNearBunker
				// + " / " + oursNearBunker);
				// }

				int enemyAdvantage = (int) (enemiesNearBunker - oursNearBunker * 0.6);
				return Math.min(2, (int) (enemyAdvantage / 2.3));
			}
		}
		return 0;
	}

	private static int defineOptimalNumberOfRepairersFor(Unit building) {
		if (building.getType().isBunker()) {
			int enemiesNearBunker = xvr.getNumberOfUnitsInRadius(building, 11,
					xvr.getEnemyArmyUnits());
			return (int) Math.min(5, Math.max(2, enemiesNearBunker / 2.4));
		} else {
			return 2;
		}
	}

	private static HashMap<Unit, Integer> _buildingsInConstructionHP = new HashMap<>();

	private static void handleUnfinishedBuildings(Unit building, UnitType buildingType) {

		// If building still isn't completed check if it has a builder,
		// something might have accidentally killed him, like e.g. a lonely
		// lurker, looking for some love
		if (!building.isCompleted()) {
			Unit builder = getWorkerBuilding(building);
			if (builder == null
					|| (builder != null && (!builder.isExists() || !builder.isConstructing()))) {
				Unit newBuilder = xvr.getOptimalBuilder(building);
				if (newBuilder != null) {
					UnitActions.rightClick(newBuilder, building);
				}
			}
		}

		// System.out.println("TEST " + building.isUnderAttack() + " " +
		// !building.isCompleted());
		if (!building.isCompleted() && isBuildingAttacked(building)) {
			System.out.println("BUILDING ATTACKED: " + building.getName());
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
				System.out.println("CANCELLING CONSTRUCTION: " + building.getName() + " at "
						+ building.toStringLocation());
				xvr.getBwapi().cancelConstruction(building.getID());
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

	/**
	 * If we're building a building of this type it will return progress (in
	 * percent) of the closest construction to the finish. It's basically hit
	 * points / max hit points.
	 */
	public static int countConstructionProgress(UnitTypes buildingType) {
		int maxProgress = 0;
		if (Constructing.weAreBuilding(buildingType)) {
			for (Unit building : xvr.getUnitsOfType(buildingType)) {
				if (!building.isCompleted()) {
					int progress = 100 * building.getHP()
							/ buildingType.getType().getMaxHitPoints();
					if (progress > maxProgress) {
						maxProgress = progress;
					}
				}
			}
		}
		return maxProgress;
	}

}

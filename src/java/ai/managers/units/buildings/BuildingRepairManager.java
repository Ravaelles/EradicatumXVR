package ai.managers.units.buildings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.managers.units.workers.ProfessionalRepairers;
import ai.managers.units.workers.WorkerManager;

public class BuildingRepairManager {

	private static XVR xvr = XVR.getInstance();
	private static HashMap<Unit, Unit> repairersToBuildings = new HashMap<>();
	private static ArrayList<Unit> buildingsNeedingRepair = new ArrayList<>();

	// =========================================================

	protected static void handleBuildingsNeedingRepair(Unit building) {
		if (!building.isBunker() && !building.isMissileTurret()) {
			return;
		}

		// =========================================================
		// Do not repair buildings far from base other than bunker
		if (!building.getType().isBunker()) {
			if (building.distanceTo(xvr.getFirstBase()) >= 14) {
				return;
			}
		}

		// =========================================================

		// int currentRepairers = -1;

		// currentRepairers = handleSpecialCaseRepairs(building);
		handleOrdinaryRepairs(building);
	}

	/**
	 * Try repairing normal buildings if necessary.
	 * 
	 * @param building
	 * @param currentRepairers
	 */
	private static void handleOrdinaryRepairs(Unit building) {
		boolean isBuildingDamaged = building.isWounded();

		// Act only if building is not fully healthy
		if (isBuildingDamaged && !building.isConstructing()) {

			// Ignore damages of non military buildings if not severe
			if (shouldIgnoreBuildingDamage(building)) {
				return;
			}

			// Define number of repairers for this building
			int currentRepairers = countNumberOfRepairersForBuilding(building);
			int numberOfRequiredRepairers = defineOptimalNumberOfRepairersFor(building);

			for (int i = 0; i < numberOfRequiredRepairers - currentRepairers; i++) {
				Unit repairer = WorkerManager.findBestRepairerNear(building);
				// System.out.println("    repairer: "
				// + (repairer != null ? repairer.getID() : "null"));

				// UGLY FIX, only 1/3 workers can repair
				if (repairer != null
						&& (repairer.getID() % 3 == 0 || ProfessionalRepairers
								.isProfessionalRepairer(repairer))) {
					repairBuilding(repairer, building);
				}
			}

			for (Unit worker : xvr.getWorkers()) {
				if (ProfessionalRepairers.isProfessionalRepairer(worker) && !worker.isRepairing()) {
					repairBuilding(worker, building);
				}
			}
		}
	}

	private static boolean shouldIgnoreBuildingDamage(Unit building) {
		return !building.isBunker() && !building.isMissileTurret() && building.getHPPercent() > 50;
	}

	// private static int handleSpecialCaseRepairs(Unit building) {
	// int currentRepairers = -1;
	//
	// // Define if building needs extra repairers (e.g. bunkers need more
	// // than one repairer which is extra case as normally you'd use only 1)
	// int specialCaseRepairersNeeded = getSpecialCaseRepairers(building);
	// if (specialCaseRepairersNeeded > 0) {
	//
	// // Define how many repairers are already assign to this building
	// if (currentRepairers == -1) {
	// currentRepairers = countNumberOfRepairersForBuilding(building);
	// }
	//
	// // If needed, find new repairers for this building
	// int numberOfNewRepairersNeeded = specialCaseRepairersNeeded -
	// currentRepairers;
	// for (int i = 0; i < numberOfNewRepairersNeeded; i++) {
	// Unit repairer = WorkerManager.findBestRepairerNear(building);
	// repairBuilding(repairer, building);
	// }
	// }
	//
	// return currentRepairers;
	// }

	// =========================================================

	private static void repairBuilding(Unit repairer, Unit building) {
		UnitActions.repair(repairer, building);
		repairersToBuildings.put(repairer, building);
		buildingsNeedingRepair.add(building);
	}

	public static int countNumberOfRepairersForBuilding(Unit building) {
		int total = 0;
		for (Unit worker : repairersToBuildings.keySet()) {
			if (repairersToBuildings.get(worker).equals(building)
					|| worker.getTargetUnitID() == building.getID()) {
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
			int enemiesNearBunker = xvr.countUnitsEnemyInRadius(building, 26);
			// System.out.println("|" + building.getName() + "|" +
			// building.toStringLocation() + "|"
			// + enemiesNearBunker);
			if (enemiesNearBunker >= 2) {
				int oursNearBunker = xvr.countUnitsOursInRadius(building, 9);
				if (xvr.isEnemyProtoss()) {
					oursNearBunker /= 2;
				}

				// if (enemiesNearBunker > 0) {
				// System.out.println("enemiesNearBunker - oursNearBunker = " +
				// enemiesNearBunker
				// + " / " + oursNearBunker);
				// }

				int enemyAdvantage = (int) (enemiesNearBunker - oursNearBunker * 0.77);
				return Math.min(5, Math.min(2, (int) (enemyAdvantage / 2.3)));
			}
		}
		return 0;
	}

	private static int defineOptimalNumberOfRepairersFor(Unit building) {
		// if (building.getType().isBunker()) {
		// int enemiesNearBunker = xvr.getNumberOfUnitsInRadius(building, 11,
		// xvr.getEnemyArmyUnits());
		// return (int) Math.min(5,
		// Math.max(2, enemiesNearBunker *
		// defineBunkerRepairersPerEnemyRatio()));
		// } else {
		// return 2;
		// }
		return 1;
	}

	private static double defineBunkerRepairersPerEnemyRatio() {
		if (xvr.isEnemyProtoss()) {
			return 1;
		} else {
			return 0.6;
		}
	}

	protected static void removeHealthyBuildingsFromRepairQueue() {
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

}

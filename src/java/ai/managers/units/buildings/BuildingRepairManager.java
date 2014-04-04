package ai.managers.units.buildings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.managers.units.workers.WorkerManager;

public class BuildingRepairManager {

	private static XVR xvr = XVR.getInstance();
	private static HashMap<Unit, Unit> repairersToBuildings = new HashMap<>();
	private static ArrayList<Unit> buildingsNeedingRepair = new ArrayList<>();

	// =========================================================

	protected static void handleBuildingsNeedingRepair(Unit building) {
		int currentRepairers = -1;
		boolean isBuildingDamaged = building.isWounded();

		int specialCaseRepairersNeeded = getSpecialCaseRepairers(building);
		if (specialCaseRepairersNeeded > 0) {
			if (currentRepairers == -1) {
				currentRepairers = countNumberOfRepairersForBuilding(building);
			}

			for (int i = 0; i < specialCaseRepairersNeeded - currentRepairers; i++) {
				Unit repairer = WorkerManager.findBestRepairerNear(building);
				repairBuilding(repairer, building);
			}
		}

		// Act only if building is not fully healthy
		if (isBuildingDamaged && !building.isConstructing()) {

			// Define number of repairers for this building
			int numberOfRequiredRepairers = defineOptimalNumberOfRepairersFor(building);
			if (currentRepairers == -1) {
				currentRepairers = countNumberOfRepairersForBuilding(building);
			}

			for (int i = 0; i < numberOfRequiredRepairers - currentRepairers; i++) {
				Unit repairer = WorkerManager.findBestRepairerNear(building);
				// System.out.println("    repairer: "
				// + (repairer != null ? repairer.getID() : "null"));
				repairBuilding(repairer, building);
			}
		}
	}

	// =========================================================

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
			int enemiesNearBunker = xvr.countUnitsEnemyInRadius(building, 26);
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

				int enemyAdvantage = (int) (enemiesNearBunker - oursNearBunker * 0.77);
				return Math.min(2, (int) (enemyAdvantage / 2.3));
			}
		}
		return 0;
	}

	private static int defineOptimalNumberOfRepairersFor(Unit building) {
		if (building.getType().isBunker()) {
			int enemiesNearBunker = xvr.getNumberOfUnitsInRadius(building, 11,
					xvr.getEnemyArmyUnits());
			int optimalRepairers = (int) Math.min(5,
					Math.max(2, enemiesNearBunker * defineBunkerRepairersPerEnemyRatio()));
			return XVR.isEnemyTerran() ? Math.min(1, optimalRepairers) : optimalRepairers;
		} else {
			return 2;
		}
	}

	private static double defineBunkerRepairersPerEnemyRatio() {
		if (XVR.isEnemyProtoss()) {
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

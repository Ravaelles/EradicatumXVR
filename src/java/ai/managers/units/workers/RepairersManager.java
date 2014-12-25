package ai.managers.units.workers;

import java.util.ArrayList;
import java.util.HashMap;

import jnibwapi.model.Unit;
import ai.core.XVR;

public class RepairersManager {

	private static XVR xvr = XVR.getInstance();
	public static ArrayList<Unit> repairers = new ArrayList<>();
	public static HashMap<Unit, Unit> repairersToBuildings = new HashMap<>();
	public static ArrayList<Unit> buildingsNeedingRepair = new ArrayList<>();

	// =========================================================
	// Logic

	public static void removeNonRepairers() {
		ArrayList<Unit> toRemoveList = new ArrayList<>();

		for (Unit unit : repairers) {
			if (unit == null || !unit.isExists() || !unit.isMoving() || !unit.isRepairing()
					|| unit.isIdle()) {
				toRemoveList.add(unit);
				repairersToBuildings.remove(unit);
			}
		}

		repairers.removeAll(toRemoveList);
	}

	public static int countNumberOfRepairersForBuilding(Unit building) {
		if (building == null) {
			return 0;
		}

		int total = 0;
		for (Unit worker : repairers) {
			if (worker == null) {
				return 0;
			}

			if (repairersToBuildings.get(worker) == null) {
				continue;
			}

			if (repairersToBuildings.get(worker).equals(building)
					|| building.equals(worker.getTargetUnitID())) {
				total++;
			}
		}
		return total;
	}

	// =========================================================
	// Actions

	public static void addRepairer(Unit repairer, Unit beingRepaired) {
		repairers.remove(repairer);
		repairersToBuildings.remove(repairer);

		repairers.add(repairer);

		if (beingRepaired != null && beingRepaired.isBuilding()) {
			repairersToBuildings.put(repairer, beingRepaired);
			buildingsNeedingRepair.add(beingRepaired);
		}
	}

	// =========================================================
	// Getters

	public static Unit getBuildingToRepairBy(Unit worker) {
		return repairersToBuildings.get(worker);
	}

}

package ai.managers.units;

import java.util.HashMap;

import jnibwapi.model.Unit;
import ai.managers.WorkerManager;

public class RepairAndSons {

	// private static XVR xvr = XVR.getInstance();

	private static HashMap<Unit, Unit> unitsToRepairers = new HashMap<>();
	private static HashMap<Unit, Unit> repairersToUnits = new HashMap<>();

	public static void issueTicketToRepair(Unit unit) {
		if (unit == null) {
			return;
		}

		// Check if repairer is alive
		if (unitsToRepairers.containsKey(unit)) {
			Unit currentRepairer = unitsToRepairers.get(unit);
			if (currentRepairer.getHP() > 0 && currentRepairer.isExists()) {
				return;
			}
		}

		Unit repairer = WorkerManager.findNearestRepairerTo(unit);
		if (repairer == null) {
			return;
		}
		unitsToRepairers.put(unit, repairer);
		repairersToUnits.put(repairer, unit);
	}

	public static Unit getUnitAssignedToRepairBy(Unit worker) {
		return repairersToUnits.get(worker);
	}

	public static Unit getRepairerForUnit(Unit wounded) {
		return unitsToRepairers.get(wounded);
	}

	public static void removeTicketFor(Unit previouslyWoundedUnit, Unit repairer) {
		unitsToRepairers.remove(previouslyWoundedUnit);
		repairersToUnits.remove(repairer);
	}

}

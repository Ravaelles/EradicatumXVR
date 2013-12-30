package ai.managers.units;

import java.util.HashMap;

import jnibwapi.model.Unit;
import ai.handling.units.UnitActions;
import ai.managers.WorkerManager;

public class RepairAndSons {

	// private static XVR xvr = XVR.getInstance();

	private static HashMap<Unit, Unit> unitsToRepairers = new HashMap<>();
	private static HashMap<Unit, Unit> repairersToUnits = new HashMap<>();

	public static boolean tryRepairingSomethingIfNeeded(Unit worker) {
		Unit repairThisUnit = RepairAndSons.getUnitAssignedToRepairBy(worker);
		if (repairThisUnit != null) {
			if (repairThisUnit.isWounded()) {

				// Don't repair Vultures that are far away. They must come to
				// the worker. It's because they tend to be wounded all the time
				// and this way SCV gets killed too often.
				if (repairThisUnit.getType().isVulture() && repairThisUnit.distanceTo(worker) >= 6) {
					return false;
				} else {
					UnitActions.repair(worker, repairThisUnit);
					worker.setAiOrder("Repair " + repairThisUnit.getName());
					return true;
				}
			}

			// This repair order is obsolete, remove it.
			else {
				RepairAndSons.removeTicketFor(repairThisUnit, worker);
			}
		}
		return false;
	}

	public static void issueTicketToRepairIfHasnt(Unit unit) {
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
		unit.setBeingRepaired(true);
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

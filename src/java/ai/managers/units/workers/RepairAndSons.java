package ai.managers.units.workers;

import java.util.HashMap;

import jnibwapi.model.Unit;
import ai.handling.units.UnitActions;

public class RepairAndSons {

	// private static XVR xvr = XVR.getInstance();

	private static HashMap<Unit, Unit> unitsToRepairers = new HashMap<>();
	private static HashMap<Unit, Unit> repairersToUnits = new HashMap<>();

	// =========================================================

	public static void tryIssuingRepairOrderIfPossible(Unit unit) {
		if (unit.isRepairable() && unit.isWounded()) {
			issueTicketToRepairIfHasnt(unit);
		}
	}

	// =========================================================

	public static boolean tryRepairingSomethingIfNeeded(Unit worker) {
		Unit repairThisUnit = RepairAndSons.getUnitAssignedToRepairBy(worker);

		// This worker has assigned unit to repair
		if (repairThisUnit != null) {

			// If unit is damaged and it still exists, try repairing it.
			if (repairThisUnit.isWounded() && repairThisUnit.isExists()) {

				// Don't repair Vultures that are far away. They must come to
				// the worker. It's because they tend to be wounded all the time
				// and this way SCV gets killed too often.
				if (repairThisUnit.getType().isVulture() && repairThisUnit.distanceTo(worker) >= 7) {
					return false;
				} else {
					UnitActions.repair(worker, repairThisUnit);
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

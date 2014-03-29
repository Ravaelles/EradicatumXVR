package ai.managers.units.workers;

import java.util.HashMap;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.units.UnitActions;

public class RepairAndSons {

	private static XVR xvr = XVR.getInstance();

	private static HashMap<Unit, Unit> unitsToRepairers = new HashMap<>();
	private static HashMap<Unit, Unit> repairersToUnits = new HashMap<>();

	// =========================================================

	public static boolean tryGoingToRepairIfNeeded(Unit unit) {

		// Only wounded mechanical units should consider being repaired
		if (!unit.isWounded() || !unit.isRepairable()) {
			return false;
		}

		// =========================================================
		// Issue an ticket to repair this unit
		RepairAndSons.tryIssuingRepairOrderIfPossible(unit);

		// Go to repairer if distance is big
		Unit repairerForUnit = getRepairerForUnit(unit);
		if (repairerForUnit != null) {
			if (repairerForUnit.distanceTo(unit) > 3) {
				UnitActions.moveTo(unit, repairerForUnit);
				return true;
			}
		}
		// else {
		// System.out.println("TEST no repairer for " + unit.toStringType());
		// }
		return false;
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

		// // No units assigned to repait, but check if there're some crucial
		// units
		// // to repair like e.g. tanks
		// else {
		// tryExtraRepairingTank(worker);
		// }

		return false;
	}

	// =========================================================

	public static void tryIssuingRepairOrderIfPossible(Unit unit) {
		if (unit.isRepairable() && unit.isWounded()) {
			issueTicketToRepairIfHasnt(unit);
		}
	}

	public static void issueTicketToRepairIfHasnt(Unit unit) {
		if (unit == null) {
			return;
		}

		// Check if repairer is alive
		if (unitsToRepairers.containsKey(unit)) {
			Unit currentRepairer = unitsToRepairers.get(unit);
			if (!unit.getType().isTank()) {
				if (currentRepairer.getHP() > 0 && currentRepairer.isExists()) {
					return;
				}
			} else {
				if (currentRepairer != null && currentRepairer.getHP() > 0
						&& calculateNumberOfRepairersFor(unit) <= 2) {
					return;
				}
			}
		}

		Unit repairer = WorkerManager.findBestRepairerNear(unit);
		if (repairer == null && xvr.getWorkers().size() > 0) {
			// System.out.println("------------ No repairer found for unit: " +
			// unit.getName());
			return;
		}
		// System.out.println("### Repairer for unit: " + unit.getName() + " (#"
		// + unit.getID()
		// + ") is " + " SCV (#" + repairer.getID() + "), distance: "
		// + repairer.distanceTo(unit));
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

	public static int calculateNumberOfRepairersFor(Unit wounded) {
		int counter = 0;
		for (Unit repairer : repairersToUnits.keySet()) {
			if (repairersToUnits.get(repairer).equals(wounded)) {
				counter++;
			}
		}
		return counter;
	}

	public static void removeTicketFor(Unit previouslyWoundedUnit, Unit repairer) {
		unitsToRepairers.remove(previouslyWoundedUnit);
		repairersToUnits.remove(repairer);
	}

	public static boolean isUnitBeingRepaired(Unit unit) {
		if (unit.isBeingRepaired()) {
			return true;
		}

		Unit repairerForUnit = getRepairerForUnit(unit);
		if (repairerForUnit != null && repairerForUnit.distanceTo(unit) <= 5) {
			return true;
		}

		return false;
	}

}

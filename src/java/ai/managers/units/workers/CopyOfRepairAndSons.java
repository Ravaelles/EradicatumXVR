package ai.managers.units.workers;

import java.util.HashMap;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.units.UnitActions;

public class CopyOfRepairAndSons {

	private static final int MAX_GOOD_WILL_REPAIRERS = 3;
	private static final int REPAIR_IF_NOT_ASKED_MAX_DISTANCE_FROM_UNIT = 8;
	private static final int REPAIR_IF_NOT_ASKED_MIN_DISTANCE_FROM_BASE = 10;

	private static XVR xvr = XVR.getInstance();

	private static HashMap<Unit, Unit> unitsToRepairers = new HashMap<>();
	private static HashMap<Unit, Unit> repairersToUnits = new HashMap<>();

	// =========================================================

	public static boolean tryGoingToRepairIfNeeded(Unit unit) {

		// Only wounded mechanical units should consider being repaired
		if (!unit.isWounded() || !unit.isRepairable()) {
			return false;
		}

		if (!isUnitReallyWoundedAndNotAScratch(unit)) {
			return false;
		}

		// =========================================================
		// Issue an ticket to repair this unit
		CopyOfRepairAndSons.tryIssuingRepairOrderIfPossible(unit);

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

	private static boolean isUnitReallyWoundedAndNotAScratch(Unit unit) {
		double ratio = 0.4;
		if (unit.getType().isTank()) {
			ratio = 0.65;
		}
		return unit.getHP() < unit.getMaxHP() * ratio;
	}

	// =========================================================

	public static boolean tryRepairingSomethingIfNeeded(Unit worker) {

		// If we're out of minerals, don't repair
		if (!xvr.canAfford(10)) {
			return false;
		}

		// Disallow wounded workers to repair; this way we can save many lives
		if (worker.getHP() < 45) {
			return false;
		}

		// Only units far from base can repair
		Unit firstBase = xvr.getFirstBase();
		if (firstBase != null) {
			if (firstBase.distanceTo(worker) < REPAIR_IF_NOT_ASKED_MIN_DISTANCE_FROM_BASE) {
				return false;
			}
		}

		// =========================================================

		Unit repairThisUnit = CopyOfRepairAndSons.getUnitAssignedToRepairBy(worker);

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
					worker.setAiOrder("Repair " + repairThisUnit.getNameShort());
					UnitActions.repair(worker, repairThisUnit);
					return true;
				}
			}

			// This repair order is obsolete, remove it.
			else {
				CopyOfRepairAndSons.removeTicketFor(repairThisUnit, worker);
			}
		}

		// No-one asked us specifically to repair, but ensure if there isn't
		// someone who can re repaired
		else {
			if (tryRepairingSomethingEvenIfNotAsked(worker)) {
				return true;
			}
		}

		// // No units assigned to repait, but check if there're some crucial
		// units
		// // to repair like e.g. tanks
		// else {
		// tryExtraRepairingTank(worker);
		// }

		repairersToUnits.remove(worker);

		return false;
	}

	// =========================================================

	private static boolean tryRepairingSomethingEvenIfNotAsked(Unit worker) {
		for (Unit otherUnit : xvr.getUnitsInRadius(worker,
				REPAIR_IF_NOT_ASKED_MAX_DISTANCE_FROM_UNIT, xvr.getBwapi().getMyUnits())) {
			if (otherUnit.isRepairable() && !otherUnit.isConstructing() && otherUnit.isWounded()
					&& !otherUnit.getType().isOnGeyser()) {

				// Make sure too many repairers aren't repairing single unit
				int currentRepairers = 0;
				for (Unit repairedUnit : repairersToUnits.values()) {
					if (repairedUnit.equals(otherUnit)) {
						currentRepairers++;
					}
				}

				if (currentRepairers < MAX_GOOD_WILL_REPAIRERS) {
					repairersToUnits.put(worker, otherUnit);
					UnitActions.repair(worker, otherUnit);
				}
				return true;
			}
		}

		return false;
	}

	public static void tryIssuingRepairOrderIfPossible(Unit unit) {
		if (unit.isRepairable() && unit.isWounded()) {
			issueTicketToRepairIfHasnt(unit);
		} else {
			unitsToRepairers.remove(unit);
		}
	}

	public static Unit issueTicketToRepairIfHasnt(Unit unit) {
		if (unit == null) {
			return null;
		}

		// Check if repairer is alive
		if (unitsToRepairers.containsKey(unit)) {
			Unit currentRepairer = unitsToRepairers.get(unit);
			if (unit != null && currentRepairer != null) {
				if (!unit.getType().isTank()) {
					if (currentRepairer.getHP() > 0 && currentRepairer.isExists()) {
						return currentRepairer;
					}
				} else {
					if (currentRepairer != null && currentRepairer.getHP() > 0
							&& calculateNumberOfRepairersFor(unit) <= 2) {
						return currentRepairer;
					}
				}
			}
		}

		Unit repairer = WorkerManager.findBestRepairerNear(unit);
		if (repairer == null && xvr.getWorkers().size() > 0) {
			// System.out.println("------------ No repairer found for unit: " +
			// unit.getName());
			return null;
		}
		// System.out.println("### Repairer for unit: " + unit.getName() + " (#"
		// + unit.getID()
		// + ") is " + " SCV (#" + repairer.getID() + "), distance: "
		// + repairer.distanceTo(unit));
		unitsToRepairers.put(unit, repairer);
		repairersToUnits.put(repairer, unit);
		unit.setBeingRepaired(true);

		return repairer;
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

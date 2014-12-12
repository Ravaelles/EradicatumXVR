package ai.managers.units.workers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranRefinery;
import ai.utils.RUtilities;

public class GathererManager {

	private static XVR xvr = XVR.getInstance();

	public static void gatherResources(Unit worker, Unit nearestBase) {
		if (worker == null) {
			return;
		}

		// =========================================================
		// If nearest base is null, it means we should find the nearest base
		// ourselves

		if (nearestBase == null) {
			nearestBase = TerranCommandCenter.getNearestBaseForUnit(worker);
		}

		// if it's still null, just quit
		if (nearestBase == null) {
			return;
		}

		// =========================================================

		boolean existsAssimilatorNearBase = TerranCommandCenter.isExistingCompletedAssimilatorNearBase(nearestBase);

		int gatheringGas = TerranCommandCenter.getNumberOfGasGatherersForBase(nearestBase);
		int gatheringMinerals = TerranCommandCenter.getNumberOfMineralGatherersForBase(nearestBase);

		if (existsAssimilatorNearBase
				&& gatheringGas < TerranCommandCenter.WORKERS_PER_GEYSER
				&& (gatheringMinerals >= 5 * gatheringGas || TerranCommandCenter.getMineralsNearBase(nearestBase)
						.size() <= 4)) {
			gatherGas(worker, nearestBase);
		} else {
			gatherMinerals(worker, nearestBase);
		}
	}

	private static void gatherGas(Unit worker, Unit base) {
		Unit onGeyser = xvr.getUnitOfTypeNearestTo(TerranRefinery.getBuildingType(), base);
		if (onGeyser != null) {
			xvr.getBwapi().rightClick(worker.getID(), onGeyser.getID());
		}
	}

	private static void gatherMinerals(Unit gathererToAssign, Unit nearestBase) {
		Unit mineral = getOptimalMineralForGatherer(gathererToAssign, nearestBase);
		if (mineral != null) {
			xvr.getBwapi().rightClick(gathererToAssign.getID(), mineral.getID());
		}
	}

	public static void forceGatherMinerals(Unit gathererToAssign, Unit mineral) {
		if (gathererToAssign.isCarryingMinerals()) {
			Unit nearBase = TerranCommandCenter.getNearestBaseForUnit(gathererToAssign);
			xvr.getBwapi().rightClick(gathererToAssign.getID(), nearBase.getID());
			return;
		} else if (gathererToAssign.isCarryingGas()) {
			Unit nearBase = TerranCommandCenter.getNearestBaseForUnit(gathererToAssign);
			xvr.getBwapi().rightClick(gathererToAssign.getID(), nearBase.getID());
			return;
		}

		if (mineral != null) {
			xvr.getBwapi().rightClick(gathererToAssign.getID(), mineral.getID());
		}
	}

	public static void forceGatherGas(Unit gathererToAssign, Unit nearestBase) {
		Unit onGeyser = TerranCommandCenter.getExistingCompletedAssimilatorNearBase(nearestBase);

		if (gathererToAssign.isCarryingMinerals()) {
			Unit nearBase = TerranCommandCenter.getNearestBaseForUnit(gathererToAssign);
			xvr.getBwapi().rightClick(gathererToAssign.getID(), nearBase.getID());
			return;
		} else if (gathererToAssign.isCarryingGas()) {
			Unit nearBase = TerranCommandCenter.getNearestBaseForUnit(gathererToAssign);
			xvr.getBwapi().rightClick(gathererToAssign.getID(), nearBase.getID());
			return;
		}

		if (onGeyser != null) {
			xvr.getBwapi().rightClick(gathererToAssign.getID(), onGeyser.getID());
		}
	}

	private static Unit getOptimalMineralForGatherer(Unit gathererToAssign, Unit nearestBase) {

		// Get the minerals that are closes to the base.
		ArrayList<Unit> minerals = TerranCommandCenter.getMineralsNearBase(nearestBase);
		int counter = 0;
		while (minerals.isEmpty()) {
			minerals = TerranCommandCenter.getMineralsNearBase(nearestBase, 15 + 10 * counter++);
		}

		// if (minerals.isEmpty()) {
		// // minerals = xvr.getMineralsUnits();
		// minerals = xvr
		// .getUnitsInRadius(nearestBase, 17 + (UnitCounter
		// .getNumberOfUnits(UnitManager.BASE) - 1) * 13, xvr
		// .getMineralsUnits());
		// }

		// Get workers
		ArrayList<Unit> workers = TerranCommandCenter.getWorkersNearBase(nearestBase);

		// Build mapping of number of worker to mineral
		HashMap<Unit, Integer> workersAtMineral = new HashMap<Unit, Integer>();
		for (Unit worker : workers) {
			// System.out.println();
			// System.out.println("scv.getTargetUnitID() = " +
			// scv.getTargetUnitID());
			// System.out.println("scv.getOrderTargetID() = " +
			// scv.getOrderTargetID());
			// System.out.println("scv.isGatheringMinerals() = " +
			// scv.isGatheringMinerals());
			// System.out.println("scv.getLastCommand() = " +
			// scv.getLastCommand());
			if (worker.isGatheringMinerals()) {
				Unit mineral = Unit.getByID(worker.getTargetUnitID());
				// System.out.println(mineral);
				// }
				// if (scv.isGatheringMinerals()) {

				if (workersAtMineral.containsKey(mineral)) {
					workersAtMineral.put(mineral, workersAtMineral.get(mineral) + 1);
				} else {
					workersAtMineral.put(mineral, 1);
				}
			}
		}

		// Get minimal value of gatherers assigned to one mineral
		int minimumGatherersAssigned = workersAtMineral.isEmpty() ? 0 : 9999;
		for (Integer value : workersAtMineral.values()) {
			if (minimumGatherersAssigned > value) {
				minimumGatherersAssigned = value;
			}
		}

		// Get the nearest mineral which has minimumGatherersAssigned
		Collections.shuffle(minerals);
		for (Unit mineral : minerals) {
			if (!workersAtMineral.containsKey(mineral) || workersAtMineral.get(mineral) <= minimumGatherersAssigned) {
				return mineral;
			}
		}
		return minerals.isEmpty() ? null : (Unit) RUtilities.getRandomListElement(minerals);
	}

}

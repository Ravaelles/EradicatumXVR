package ai.managers.units.workers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.Painter;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.strength.StrengthRatio;
import ai.handling.units.UnitActions;
import ai.managers.units.UnitManager;
import ai.managers.units.army.RunManager;
import ai.managers.units.buildings.BuildingRepairManager;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranRefinery;
import ai.terran.TerranSiegeTank;
import ai.utils.RUtilities;

public class WorkerManager {

	public static final int WORKER_INDEX_GUY_TO_CHASE_OTHERS = 1;
	// public static final int WORKER_INDEX_PROFESSIONAL_REPAIRER = 3;
	// public static final ArrayList<Integer> EXTRA_PROFESSIONAL_REPAIRERERS =
	// new ArrayList<>();
	public static int EXPLORER_INDEX = 6; // 6
	public static int DEFEND_BASE_RADIUS = 23;

	private static XVR xvr = XVR.getInstance();

	private static int _counter;
	// private static Unit professionalRepairer = null;
	private static List<Integer> professionalRepairersIndices = new ArrayList<>();
	private static List<Unit> lastProfessionalRepairers = new ArrayList<>();
	private static Unit guyToChaseOthers = null;

	// ======================

	@SuppressWarnings("unused")
	private static ProfessionalRepairersSettings instance = new ProfessionalRepairersSettings();

	private static class ProfessionalRepairersSettings {

		private ProfessionalRepairersSettings() {
			professionalRepairersIndices.clear();
			professionalRepairersIndices.add(19);

			if (!xvr.isEnemyTerran()) {
				professionalRepairersIndices.add(20);
			}
		}

	}

	private static void handleProfessionalRepairer(Unit unit) {
		unit.setAiOrder("Bunker repairer");

		Unit beHere = null;
		// professionalRepairer = unit;
		lastProfessionalRepairers.add(unit);

		if (unit.isRepairing() || unit.isConstructing()) {
			return;
		}

		if (TerranSiegeTank.getNumberOfUnitsCompleted() > 0) {
			MapPoint centerPoint = MapExploration.getNearestEnemyBuilding();
			if (centerPoint == null) {
				centerPoint = MapExploration.getMostDistantBaseLocation(unit);
			}
			beHere = xvr.getNearestTankTo(centerPoint);
		} else {

			beHere = xvr.getUnitOfTypeMostFarTo(TerranBunker.getBuildingType(), xvr.getFirstBase(),
					true);
		}

		if (unit.distanceTo(beHere) >= 3) {
			UnitActions.moveTo(unit, beHere);
		} else {
			UnitActions.holdPosition(unit);
		}
	}

	// ======================

	public static void act() {
		lastProfessionalRepairers.clear();
		_counter = 0;

		// ==================================
		// Handle every worker
		for (Unit worker : xvr.getUnitsOfType(UnitManager.WORKER)) {
			if (!worker.isCompleted()) {
				continue;
			}

			// Check if worker isn't supposed to repair non-existing building
			// or building that isn't damaged at all.
			checkStatusOfBuildingsNeedingRepair(worker);

			if (_counter == WORKER_INDEX_GUY_TO_CHASE_OTHERS) {
				guyToChaseOthers = worker;
			}

			// It may happen that this unit is supposed to repair other unit. If
			// so, this would have the highest priority.
			if (!RepairAndSons.tryRepairingSomethingIfNeeded(worker)) {

				// Unit can act as either a simple worker or as an explorer.
				if (_counter != EXPLORER_INDEX) {
					WorkerManager.act(worker);
				} else {
					ExplorerManager.explore(worker);
				}
			}

			_counter++;
		}
	}

	public static void act(Unit unit) {
		// if (true) {
		// return;
		// }

		if (unit.isIdle()) {
			gatherResources(unit, xvr.getFirstBase());
		}

		if (unit.equals(ExplorerManager.getExplorer())) {
			return;
		}

		// Don't interrupt when REPAIRING
		if (unit.isRepairing()) {
			return;
		}

		if (xvr.getTimeSeconds() < 300) {
			defendBase(unit);
		}

		if (unit.isAttacking()
				&& (unit.distanceTo(xvr.getFirstBase()) < 17 || unit.distanceTo(xvr.getFirstBase()) < 17)) {
			return;
		}

		// ==================================

		if (isProfessionalRepairer(unit) && TerranBunker.getNumberOfUnits() > 0) {
			handleProfessionalRepairer(unit);
			return;
		}

		if (unit.isWounded() && RunManager.runFromCloseOpponentsIfNecessary(unit)) {
			unit.setAiOrder("Fuck...");
			return;
		}

		// ==================================

		// If we should destroy this unit
		// if (unit.isShouldScrapUnit()) {
		// UnitActions
		// .attackTo(unit, MapExploration.getNearestEnemyBuilding());
		// return;
		// }

		// If this worker is attacking, and he's far from base, make him go
		// back.
		int distToMainBase = xvr.getDistanceSimple(unit, xvr.getFirstBase());
		if (unit.isAttacking()
				&& distToMainBase >= 7
				|| (unit.isConstructing() && unit.getHP() < 21 && StrengthRatio
						.isStrengthRatioCriticalFor(unit))) {
			UnitActions.moveTo(unit, xvr.getFirstBase());
			return;
		}

		// Act with worker that is under attack
		if (unit.isUnderAttack()) {

			// If nearest enemy is worker, attack this bastard!
			Unit nearestEnemy = xvr.getUnitNearestFromList(unit, xvr.getBwapi().getEnemyUnits());
			if (nearestEnemy != null) {
				if (xvr.getDistanceSimple(unit, xvr.getFirstBase()) <= 9 && !unit.isConstructing()) {
					UnitActions.attackEnemyUnit(unit, nearestEnemy);
					return;
				}
			}

			// ================================
			// Don't attack, do something else
			MapPoint goTo = null;

			// Try to go to the nearest bunker
			Unit defensiveBuildings = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(),
					unit);
			if (defensiveBuildings != null) {
				goTo = defensiveBuildings;
			} else {
				goTo = xvr.getFirstBase();
			}

			if (goTo != null) {
				if (xvr.getDistanceSimple(unit, goTo) >= 15) {
					UnitActions.moveTo(unit, goTo.getX(), goTo.getY());
				} else {
					UnitActions.moveTo(unit, goTo.getX() + 5 - RUtilities.rand(0, 12), goTo.getY()
							+ 5 - RUtilities.rand(0, 12));
					UnitActions.callForHelp(unit, false);
				}
			}
		}

		// Act with idle worker
		if (unit.isIdle() && !unit.isGatheringGas() && !unit.isGatheringMinerals()
				&& !unit.isAttacking()) {

			// Find the nearest base for this SCV
			Unit nearestBase = TerranCommandCenter.getNearestBaseForUnit(unit);

			// If base exists try to gather resources
			if (nearestBase != null) {
				gatherResources(unit, nearestBase);
				return;
			}
		}

		// Act with unit that is possibly stuck e.g. by just built Protoss
		// building, yeah it happens this shit.
		else if (unit.isConstructing() && !unit.isMoving()) {
			UnitActions.moveTo(unit, TerranCommandCenter.getNearestBaseForUnit(unit));
			return;
		}

		// // If unit is building something check if there's no duplicate
		// // constructions going on
		// else if (unit.isConstructing()) {
		// Constructing.removeDuplicateConstructionsPending(unit);
		// }
	}

	// =========================================================

	private static void checkStatusOfBuildingsNeedingRepair(Unit worker) {
		if (worker.isRepairing() && BuildingRepairManager.getBuildingToRepairBy(worker) != null) {
			Unit buildingToRepair = BuildingRepairManager.getBuildingToRepairBy(worker);
			boolean targetValid = buildingToRepair.isExists() && buildingToRepair.isWounded();
			if (!targetValid) {
				UnitActions.holdPosition(worker);
			}
		}
	}

	/**
	 * Used in case of early in-game emergency situation like e.g. Probe trying
	 * to build offensive Photon Cannon at the back of the base. See: AIUR's
	 * cheese strategy.
	 */
	private static void defendBase(Unit worker) {
		if (TerranCommandCenter.getNumberOfUnits() > 1) {
			return;
		}

		// Check for any enemy workers
		if (_counter == WORKER_INDEX_GUY_TO_CHASE_OTHERS) {
			Unit enemyWorkerNearMainBase = xvr.getEnemyWorkerInRadius(38, xvr.getFirstBase());
			UnitActions.attackEnemyUnit(worker, enemyWorkerNearMainBase);
			return;
		}

		// =================
		// Look for potential dangers to the main base
		Unit enemyToFight = xvr.getNearestEnemyInRadius(xvr.getFirstBase(), DEFEND_BASE_RADIUS,
				true, false);
		if (enemyToFight == null) {
			Unit enemyBuilding = xvr.getUnitNearestFromList(worker, xvr.getEnemyBuildings(), true,
					false);
			if (enemyBuilding != null && enemyBuilding.distanceTo(worker) <= DEFEND_BASE_RADIUS) {
				enemyToFight = enemyBuilding;
				UnitActions.attackEnemyUnit(worker, enemyToFight);
				return;
			}

			if (enemyToFight == null) {
				return;
			}
		}

		boolean isEnemyWorker = enemyToFight.isWorker();
		double distToEnemy = worker.distanceTo(enemyToFight);
		UnitType type = enemyToFight.getType();
		boolean isCriticalUnit = type.isLurker() || type.isTank() || type.isReaver();

		// ==========================
		// If there's enemy army unit nearby, run
		if (!isEnemyWorker && !enemyToFight.getType().isZergling()) {
			int numberOfEnemies = xvr.getEnemyUnitsInRadius(8, worker).size();
			if (isCriticalUnit || numberOfEnemies > 2) {
				Unit safeCannon = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(),
						xvr.getLastBase());
				if (safeCannon != null) {
					UnitActions.moveTo(worker, safeCannon);
					return;
				} else {
					UnitActions.moveAwayFrom(worker, enemyToFight);
					return;
				}
			}
		}

		// ==============================
		boolean shouldThisWorkerConsiderAttack = distToEnemy > 0 && distToEnemy < 17
				&& !worker.isAttacking() && enemyToFight.isDetected();
		boolean isTargetDangerous = !isEnemyWorker
				|| (isEnemyWorker && (enemyToFight.isConstructing()));
		boolean isTargetExtremelyDangerous = isEnemyWorker && enemyToFight.isConstructing()
				&& distToEnemy < 22;
		boolean isEnemyWorkerAttackingUs = isEnemyWorker && enemyToFight.isAttacking()
				&& distToEnemy <= 2;
		if ((shouldThisWorkerConsiderAttack && (isTargetDangerous || isEnemyWorkerAttackingUs) || isTargetExtremelyDangerous)
				&& distToEnemy < DEFEND_BASE_RADIUS) {
			// System.out.println("        ######## ATATCK");
			// Debug.message(xvr, "Attack unit: " + enemyToFight.getName());
			UnitActions.attackTo(worker, enemyToFight);
			return;
		}

		// ==========================
		// Dont allow enemy to build any buildings at our base!
		Unit enemyBuildingNearBase = xvr.getUnitNearestFromList(xvr.getFirstBase(),
				xvr.getEnemyBuildings());
		if (enemyBuildingNearBase != null) {
			double distToBuilding = xvr.getDistanceBetween(xvr.getFirstBase(),
					enemyBuildingNearBase);
			if (distToBuilding > 0 && distToBuilding <= DEFEND_BASE_RADIUS
					&& enemyBuildingNearBase.getType().isBuilding()) {
				Painter.message(xvr, "Attack building: " + enemyBuildingNearBase.getName());
				UnitActions.attackEnemyUnit(worker, enemyBuildingNearBase);
				return;
			}
		}
	}

	public static boolean isProfessionalRepairer(Unit unit) {
		return professionalRepairersIndices.contains(_counter)
				|| lastProfessionalRepairers.contains(unit);
		// return _counter == WORKER_INDEX_PROFESSIONAL_REPAIRER
		// || (EXTRA_PROFESSIONAL_REPAIRERERS.contains(_counter));
	}

	public static void gatherResources(Unit worker, Unit nearestBase) {
		boolean existsAssimilatorNearBase = TerranCommandCenter
				.isExistingCompletedAssimilatorNearBase(nearestBase);

		int gatheringGas = TerranCommandCenter.getNumberOfGasGatherersForBase(nearestBase);
		int gatheringMinerals = TerranCommandCenter.getNumberOfMineralGatherersForBase(nearestBase);

		if (existsAssimilatorNearBase
				&& gatheringGas < TerranCommandCenter.WORKERS_PER_GEYSER
				&& (gatheringMinerals >= 5 * gatheringGas || TerranCommandCenter
						.getMineralsNearBase(nearestBase).size() <= 4)) {
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
			if (!workersAtMineral.containsKey(mineral)
					|| workersAtMineral.get(mineral) <= minimumGatherersAssigned) {
				return mineral;
			}
		}
		return minerals.isEmpty() ? null : (Unit) RUtilities.getRandomListElement(minerals);
	}

	public static Unit findNearestWorkerTo(int x, int y) {
		Unit base = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, x, y, false);
		if (base == null) {
			return null;
		}

		double nearestDistance = 999999;
		Unit nearestUnit = null;

		for (Unit otherUnit : xvr.getUnitsOfType(UnitManager.WORKER)) {
			if (!otherUnit.isCompleted()
					|| (!otherUnit.isGatheringMinerals() && !otherUnit.isGatheringGas())
					|| otherUnit.isConstructing()) {
				continue;
			}

			double distance = xvr.getDistanceBetween(otherUnit, base);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestUnit = otherUnit;
			}
		}

		return nearestUnit;
	}

	public static Unit findNearestWorkerTo(Unit building) {
		return findNearestWorkerTo(building.getX(), building.getY());
	}

	public static Unit findBestRepairerNear(Unit unit) {
		double nearestDistance = 999999;
		Unit nearestUnit = null;

		boolean onlyHealthy = true;
		int counter = 0;

		while (nearestUnit == null && counter < 2) {
			for (Unit otherUnit : xvr.getUnitsOfType(UnitManager.WORKER)) {
				if (!otherUnit.isCompleted() || otherUnit.isRepairing()
						|| otherUnit.isConstructing()) {
					continue;
				}

				// In first cycle, try to find nearest healthy repairer
				if (onlyHealthy && unit.isWounded()) {
					continue;
				}

				// Explorer shouldn't be repairer
				if (unit.equals(ExplorerManager.getExplorer())) {
					continue;
				}

				double distance = xvr.getDistanceBetween(otherUnit, unit);
				if (distance < nearestDistance) {

					// && RepairAndSons.getUnitAssignedToRepairBy(otherUnit) ==
					// null
					nearestDistance = distance;
					nearestUnit = otherUnit;
				}
			}

			counter++;
			onlyHealthy = false;
		}

		return nearestUnit;
	}

	// public static Unit getProfessionalRepairer() {
	// return professionalRepairer;
	// }

	public static Unit getGuyToChaseOthers() {
		return guyToChaseOthers;
	}

}

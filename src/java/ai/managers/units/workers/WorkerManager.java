package ai.managers.units.workers;

import jnibwapi.model.Unit;
import jnibwapi.types.OrderType.OrderTypes;
import jnibwapi.types.UnitType;
import ai.core.Painter;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.strength.StrengthRatio;
import ai.handling.units.UnitActions;
import ai.managers.enemy.HiddenEnemyUnitsManager;
import ai.managers.units.UnitManager;
import ai.managers.units.army.RunManager;
import ai.managers.units.army.tanks.EnemyTanksManager;
import ai.managers.units.buildings.BuildingRepairManager;
import ai.managers.units.coordination.ArmyUnitBasicBehavior;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.utils.RUtilities;

public class WorkerManager {

	public static final int WORKER_INDEX_GUY_TO_CHASE_OTHERS = 1;
	// public static final int WORKER_INDEX_PROFESSIONAL_REPAIRER = 3;
	// public static final ArrayList<Integer> EXTRA_PROFESSIONAL_REPAIRERERS =
	// new ArrayList<>();
	public static int EXPLORER_INDEX = 6; // 6
	public static int DEFEND_BASE_RADIUS = 23;

	private static XVR xvr = XVR.getInstance();

	protected static int _counter;
	private static Unit guyToChaseOthers = null;

	// ======================

	public static void act() {
		RepairAndSons.refreshStatus();
		_counter = 0;

		// ==================================
		// Handle every worker
		for (Unit worker : xvr.getUnitsOfType(UnitManager.WORKER)) {
			if (!worker.isCompleted()) {
				continue;
			}

			// =========================================================

			// Check if worker isn't supposed to repair non-existing building
			// or building that isn't damaged at all.
			checkStatusOfBuildingsNeedingRepair(worker);

			if (_counter == WORKER_INDEX_GUY_TO_CHASE_OTHERS) {
				guyToChaseOthers = worker;
			}

			// =========================================================

			// Unit can act as either a simple worker or as an explorer.
			if (_counter == EXPLORER_INDEX) {
				actWhenIsExplorer(worker);
			}

			if (_counter != EXPLORER_INDEX) {
				worker.setIsNotExplorer();
				WorkerManager.act(worker);
			}

			_counter++;
		}
	}

	// =========================================================

	private static void actWhenIsExplorer(Unit worker) {
		worker.setIsExplorer();
		if (TerranBarracks.getNumberOfUnits() > 0) {
			ExplorerManager.explore(worker);
		} else {
			if (worker.isIdle()) {
				GathererManager.gatherResources(worker, xvr.getFirstBase());
			}
		}
	}

	public static void act(Unit worker) {

		// =========================================================
		// Section of methods from UNIT MANAGER

		// Make idle units gather resources
		if (isUnitActuallyIdle(worker)) {
			GathererManager.gatherResources(worker, null);
			return;
		}

		// Avoid enemy tanks in Siege Mode
		if (EnemyTanksManager.tryAvoidingEnemyTanks(worker)) {
			return;
		}

		// Disallow units to move close to defensive buildings
		if (ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(worker)) {
			return;
		}

		// It may happen that this unit is supposed to repair other unit. If
		// so, this would have the highest priority.
		if (RepairAndSons.tryRepairingSomethingIfNeeded(worker)) {
			return;
		}

		// Wounded units should avoid being killed (if possible you know...)
		if (ArmyUnitBasicBehavior.tryRunningIfSeriouslyWounded(worker)) {
			worker.setAiOrder("Badly wounded");
			return;
		}

		// If enemy has got very close near to us, move away
		if (RunManager.runFromCloseOpponentsIfNecessary(worker)) {
			return;
		}

		// Run from hidden Lurkers, Dark Templars etc.
		if (HiddenEnemyUnitsManager.tryAvoidingHiddenUnitsIfNecessary(worker)) {
			return;
		}

		// =========================================================

		// if (unit.equals(ExplorerManager.getExplorer()) &&
		// TerranBarracks.getNumberOfUnits() > 0) {
		// return;
		// }

		// Don't interrupt when REPAIRING
		if (worker.isRepairing()) {
			actWhenIsRepairing(worker);
			return;
		}

		if (xvr.getTimeSeconds() < 300) {
			defendBase(worker);
		}

		if (worker.isAttacking()
				&& (worker.distanceTo(xvr.getFirstBase()) < 17 || worker.distanceTo(xvr.getFirstBase()) < 17)) {
			return;
		}

		// ==================================

		// if (RepairersManager.isProfessionalRepairer(worker) &&
		// TerranBunker.getNumberOfUnits() > 0) {
		// RepairersManager.handleProfessionalRepairer(worker);
		// return;
		// }

		if (worker.isWounded() && RunManager.runFromCloseOpponentsIfNecessary(worker)) {
			worker.setAiOrder("Fuck...");
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
		int distToMainBase = xvr.getDistanceSimple(worker, xvr.getFirstBase());
		if (worker.isAttacking() && distToMainBase >= 7
				|| (worker.isConstructing() && worker.getHP() < 21 && StrengthRatio.isStrengthRatioCriticalFor(worker))) {
			UnitActions.moveTo(worker, xvr.getFirstBase());
			return;
		}

		// Act with worker that is under attack
		if (worker.isUnderAttack()) {

			// If nearest enemy is worker, attack this bastard!
			Unit nearestEnemy = xvr.getUnitNearestFromList(worker, xvr.getBwapi().getEnemyUnits());
			if (nearestEnemy != null) {
				if (xvr.getDistanceSimple(worker, xvr.getFirstBase()) <= 9 && !worker.isConstructing()) {
					UnitActions.attackEnemyUnit(worker, nearestEnemy);
					return;
				}
			}

			// ================================
			// Don't attack, do something else
			MapPoint goTo = null;

			// Try to go to the nearest bunker
			Unit defensiveBuildings = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), worker);
			if (defensiveBuildings != null) {
				goTo = defensiveBuildings;
			} else {
				goTo = xvr.getFirstBase();
			}

			if (goTo != null) {
				if (xvr.getDistanceSimple(worker, goTo) >= 15) {
					UnitActions.moveTo(worker, goTo.getX(), goTo.getY());
				} else {
					UnitActions.moveTo(worker, goTo.getX() + 5 - RUtilities.rand(0, 12),
							goTo.getY() + 5 - RUtilities.rand(0, 12));
					UnitActions.callForHelp(worker, false);
				}
			}
		}

		// Act with idle worker
		if (worker.isIdle() && !worker.isGatheringGas() && !worker.isGatheringMinerals() && !worker.isAttacking()) {

			// Find the nearest base for this SCV
			Unit nearestBase = TerranCommandCenter.getNearestBaseForUnit(worker);

			// If base exists try to gather resources
			if (nearestBase != null) {
				GathererManager.gatherResources(worker, nearestBase);
				return;
			}
		}

		// Act with unit that is possibly stuck e.g. by just built Protoss
		// building, yeah it happens this shit.
		else if (worker.isConstructing() && !worker.isMoving()) {
			UnitActions.moveTo(worker, TerranCommandCenter.getNearestBaseForUnit(worker));
			return;
		}

		// // If unit is building something check if there's no duplicate
		// // constructions going on
		// else if (unit.isConstructing()) {
		// Constructing.removeDuplicateConstructionsPending(unit);
		// }
	}

	private static void actWhenIsRepairing(Unit worker) {
		Unit repairedUnit = Unit.getMyUnitByID(worker.getTargetUnitID());
		if (repairedUnit != null && repairedUnit.isWounded()) {
			if (xvr.getTimeSeconds() % 3 == 0) {
				UnitActions.repair(worker, repairedUnit);
			}
		} else {
			UnitActions.moveToMainBase(worker);
		}
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
		Unit enemyToFight = xvr.getNearestEnemyInRadius(xvr.getFirstBase(), DEFEND_BASE_RADIUS, true, false);
		if (enemyToFight == null) {
			Unit enemyBuilding = xvr.getUnitNearestFromList(worker, xvr.getEnemyBuildings(), true, false);
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
				Unit safeCannon = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), xvr.getLastBase());
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
		boolean shouldThisWorkerConsiderAttack = distToEnemy > 0 && distToEnemy < 17 && !worker.isAttacking()
				&& enemyToFight.isDetected();
		boolean isTargetDangerous = !isEnemyWorker || (isEnemyWorker && (enemyToFight.isConstructing()));
		boolean isTargetExtremelyDangerous = isEnemyWorker && enemyToFight.isConstructing() && distToEnemy < 22;
		boolean isEnemyWorkerAttackingUs = isEnemyWorker && enemyToFight.isAttacking() && distToEnemy <= 2;
		if ((shouldThisWorkerConsiderAttack && (isTargetDangerous || isEnemyWorkerAttackingUs) || isTargetExtremelyDangerous)
				&& distToEnemy < DEFEND_BASE_RADIUS) {
			// System.out.println("        ######## ATATCK");
			// Debug.message(xvr, "Attack unit: " + enemyToFight.getName());
			UnitActions.attackTo(worker, enemyToFight);
			return;
		}

		// ==========================
		// Dont allow enemy to build any buildings at our base!
		Unit enemyBuildingNearBase = xvr.getUnitNearestFromList(xvr.getFirstBase(), xvr.getEnemyBuildings());
		if (enemyBuildingNearBase != null) {
			double distToBuilding = xvr.getDistanceBetween(xvr.getFirstBase(), enemyBuildingNearBase);
			if (distToBuilding > 0 && distToBuilding <= DEFEND_BASE_RADIUS
					&& enemyBuildingNearBase.getType().isBuilding()) {
				Painter.message(xvr, "Attack building: " + enemyBuildingNearBase.getName());
				UnitActions.attackEnemyUnit(worker, enemyBuildingNearBase);
				return;
			}
		}
	}

	public static Unit findNearestWorkerTo(int x, int y) {
		Unit base = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, x, y, false);
		if (base == null) {
			return null;
		}

		double nearestDistance = 999999;
		Unit nearestUnit = null;

		for (Unit otherUnit : xvr.getUnitsOfType(UnitManager.WORKER)) {
			if (!otherUnit.isCompleted() || (!otherUnit.isGatheringMinerals() && !otherUnit.isGatheringGas())
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
				if (!otherUnit.isCompleted() || otherUnit.isRepairing() || otherUnit.isConstructing()) {
					continue;
				}

				// In first cycle, try to find nearest healthy repairer
				if (onlyHealthy && unit.isWounded()) {
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

	// =========================================================
	// Auxiliary

	private static boolean isUnitActuallyIdle(Unit unit) {
		return unit.isIdle() || unit.getOrderID() == OrderTypes.None.ordinal();
	}

	public static Unit getGuyToChaseOthers() {
		return guyToChaseOthers;
	}

}

package ai.managers.units.workers;

import java.util.ArrayList;
import java.util.List;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.terran.TerranBunker;
import ai.units.SelectUnits;
import ai.utils.RUtilities;

public class ProfessionalRepairers {

	private static XVR xvr = XVR.getInstance();

	protected static List<Integer> professionalRepairersIndices = new ArrayList<>();
	protected static List<Unit> lastProfessionalRepairers = new ArrayList<>();

	// =========================================================

	public static void handleProfessionalRepairer(Unit unit) {
		unit.setAiOrder("Cool guy");

		lastProfessionalRepairers.add(unit);

		if (unit.isRepairing() || unit.isConstructing()) {
			return;
		}

		// if (TerranSiegeTank.getNumberOfUnitsCompleted() > 0) {
		// MapPoint centerPoint = MapExploration.getNearestEnemyBuilding();
		// if (centerPoint == null) {
		// centerPoint = MapExploration.getMostDistantBaseLocation(unit);
		// }
		// beHere = xvr.getNearestTankTo(centerPoint);
		// } else {
		//
		// beHere = xvr.getUnitOfTypeMostFarTo(TerranBunker.getBuildingType(),
		// xvr.getFirstBase(),
		// true);
		// }
		//
		// if (unit.distanceTo(beHere) >= 3) {
		// UnitActions.moveTo(unit, beHere);
		// } else {
		// UnitActions.holdPosition(unit);
		// }

		// =========================================================
		// Repair bunker

		if (tryRepairingBunkersIfNeeded(unit)) {
			unit.setAiOrder("Bunker!");
			return;
		}

		// =========================================================
		// Go to proper place according to attack or peace

		// Global attack
		if (StrategyManager.isGlobalAttackInProgress()) {
			unit.setAiOrder("Help 'em");
			actWhenAttackInProgress(unit);
		}

		// Peace
		else {
			unit.setAiOrder("Cool guy");
			if (actWhenPeace(unit)) {
				return;
			}
		}

		// =========================================================
		// Repair units in neighbourhood if needed

		if (tryRepairingWoundedUnitsIfPossible(unit)) {
			unit.setAiOrder("Repair");
			return;
		}
	}

	private static boolean actWhenPeace(Unit unit) {
		// MapPoint defensivePoint =
		// ArmyRendezvousManager.getDefensivePoint(unit);
		// MapPoint placeToBe = MapPointInstance
		// .getPointBetween(defensivePoint, xvr.getFirstBase(), 1);
		//
		// if (placeToBe != null) {
		// if (placeToBe.distanceTo(unit) > 5) {
		// UnitActions.moveTo(unit, placeToBe);
		// }
		// else {
		// }
		// }
		// Unit bunker =
		// SelectUnits.our().ofType(UnitTypes.Terran_Bunker).units().first();
		Unit bunker = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), unit);
		if (bunker != null && bunker.distanceTo(unit) > 2.5) {
			UnitActions.moveTo(unit, bunker);
			return true;
		}

		return false;
	}

	private static void actWhenAttackInProgress(Unit unit) {
		MapPoint placeToBe = ArmyRendezvousManager.getRendezvousTankForGroundUnits();
		if (placeToBe != null && placeToBe.distanceTo(unit) > 5) {
			UnitActions.moveTo(unit, placeToBe);
		}
	}

	// =========================================================

	private static boolean tryRepairingWoundedUnitsIfPossible(Unit worker) {
		// for (Unit woundedUnit :
		// SelectUnits.our().toRepair().nearestTo(worker).list()) {
		// UnitActions.repair(worker, woundedUnit);
		// }

		Unit nearestWoundedUnit = SelectUnits.our().toRepair().nearestTo(worker);
		if (nearestWoundedUnit != null && nearestWoundedUnit.isCompleted()
				&& nearestWoundedUnit.distanceTo(worker) < 13) {
			UnitActions.repair(worker, nearestWoundedUnit);
		}

		return false;
	}

	// =========================================================

	private static boolean tryRepairingBunkersIfNeeded(Unit worker) {
		for (Unit bunker : xvr.getUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(), 15,
				worker, true)) {
			if (bunker.isCompleted() && bunker.getHPPercent() < 100) {
				UnitActions.repair(worker, bunker);
				return true;
			}
		}

		return false;
	}

	public static boolean isProfessionalRepairer(Unit unit) {
		return professionalRepairersIndices.contains(WorkerManager._counterWorkerIndexInLoop)
				|| lastProfessionalRepairers.contains(unit)
				|| professionalRepairersIndices.contains(unit);
		// return _counter == WORKER_INDEX_PROFESSIONAL_REPAIRER
		// || (EXTRA_PROFESSIONAL_REPAIRERERS.contains(_counter));
	}

	public static void resetInfo() {
		if (lastProfessionalRepairers != null) {
			lastProfessionalRepairers.clear();
		}
	}

	public static Unit getOne() {
		for (Unit repairer : lastProfessionalRepairers) {
			if (!repairer.isRepairing()) {
				return repairer;
			}
		}

		if (lastProfessionalRepairers != null && !lastProfessionalRepairers.isEmpty()) {
			return (Unit) RUtilities.getRandomElement(lastProfessionalRepairers);
		}

		return null;
	}

	// =========================================================

	@SuppressWarnings("unused")
	private static ProfessionalRepairersConfig instance = new ProfessionalRepairersConfig();

	private static class ProfessionalRepairersConfig {

		private ProfessionalRepairersConfig() {
			professionalRepairersIndices.clear();

			if (xvr.isEnemyProtoss()) {
				professionalRepairersIndices.add(12);
				professionalRepairersIndices.add(17);
			} else if (xvr.isEnemyZerg()) {
				professionalRepairersIndices.add(10);
			} else {
				professionalRepairersIndices.add(16);
			}
		}

	}

}

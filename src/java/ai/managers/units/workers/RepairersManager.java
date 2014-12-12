package ai.managers.units.workers;

import java.util.ArrayList;
import java.util.List;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.strategies.TerranOffensiveBunker;
import ai.terran.TerranBunker;
import ai.terran.TerranSiegeTank;

public class RepairersManager {

	private static XVR xvr = XVR.getInstance();

	// private static Unit professionalRepairer = null;
	private static List<Integer> professionalRepairersIndices = new ArrayList<>();
	private static List<Unit> lastProfessionalRepairers = new ArrayList<>();

	// =========================================================

	@SuppressWarnings("unused")
	private static ProfessionalRepairersSettings instance = new ProfessionalRepairersSettings();

	// =========================================================

	public static void handleProfessionalRepairer(Unit unit) {
		unit.setAiOrder("Ima Repairer, boetch");

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

			beHere = xvr.getUnitOfTypeMostFarTo(TerranBunker.getBuildingType(), xvr.getFirstBase(), true);
		}

		if (unit.distanceTo(beHere) >= 3) {
			UnitActions.moveTo(unit, beHere);
		} else {
			UnitActions.holdPosition(unit);
		}
	}

	public static boolean isProfessionalRepairer(Unit unit) {
		int _counter = WorkerManager._counter;
		return professionalRepairersIndices.contains(_counter) || lastProfessionalRepairers.contains(unit);
		// return _counter == WORKER_INDEX_PROFESSIONAL_REPAIRER
		// || (EXTRA_PROFESSIONAL_REPAIRERERS.contains(_counter));
	}

	// =========================================================

	private static class ProfessionalRepairersSettings {

		private ProfessionalRepairersSettings() {
			professionalRepairersIndices.clear();

			if (TerranOffensiveBunker.isStrategyActive()) {
				professionalRepairersIndices.add(5);
				// professionalRepairersIndices.add(16);
				// professionalRepairersIndices.add(17);
				// professionalRepairersIndices.add(18);
			} else {
				professionalRepairersIndices.add(19);
				if (!XVR.isEnemyTerran()) {
					professionalRepairersIndices.add(20);
				}
			}
		}

	}

}

package ai.managers.units.workers;

import java.util.ArrayList;
import java.util.List;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.terran.TerranBunker;
import ai.terran.TerranSiegeTank;

public class ProfessionalRepairers {

	private static XVR xvr = XVR.getInstance();

	protected static List<Integer> professionalRepairersIndices = new ArrayList<>();
	protected static List<Unit> lastProfessionalRepairers = new ArrayList<>();

	// =========================================================

	@SuppressWarnings("unused")
	private static ProfessionalRepairersConfig instance = new ProfessionalRepairersConfig();

	private static class ProfessionalRepairersConfig {

		private ProfessionalRepairersConfig() {
			// System.out.println("KURWAAAAAAAAA");
			// System.exit(-2);

			professionalRepairersIndices.clear();

			if (xvr.isEnemyProtoss()) {
				professionalRepairersIndices.add(14);
			} else if (xvr.isEnemyZerg()) {
				professionalRepairersIndices.add(10);
			} else {
				professionalRepairersIndices.add(16);
			}
		}

	}

	// =========================================================

	public static void handleProfessionalRepairer(Unit unit) {
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

}

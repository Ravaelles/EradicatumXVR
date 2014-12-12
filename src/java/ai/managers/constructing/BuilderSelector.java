package ai.managers.constructing;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.managers.units.UnitManager;
import ai.managers.units.workers.ExplorerManager;
import ai.managers.units.workers.RepairersManager;
import ai.managers.units.workers.WorkerManager;
import ai.strategies.TerranOffensiveBunker;
import ai.terran.TerranBunker;

public class BuilderSelector {

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static Unit getOptimalBuilder(MapPoint buildTile) {
		return getOptimalBuilder(buildTile, null);
	}

	public static Unit getOptimalBuilder(MapPoint buildTile, UnitTypes building) {

		// STRATEGY: Offensive Bunker
		if (building != null && TerranOffensiveBunker.isStrategyActive()) {

			// If we're building bunker, make sure we do it with the explorer
			if (building.ordinal() == TerranBunker.getBuildingType().ordinal() && TerranBunker.getNumberOfUnits() == 0) {
				Unit explorer = ExplorerManager.getExplorer();
				if (explorer != null) {
					return explorer;
				}
			}
		}

		// =========================================================

		ArrayList<Unit> freeWorkers = new ArrayList<Unit>();
		// System.out.println();
		// System.out.println("INITIAL: " + xvr.getWorkers().size());
		for (Unit worker : xvr.getWorkers()) {
			// System.out.println("expl: " + worker.isExplorer() +
			// " / profRep: "
			// + WorkerManager.isProfessionalRepairer(worker) + " / const: "
			// + worker.isConstructing());
			if (worker.isCompleted()
					&& !worker.isConstructing()
					&& !worker.isRepairing()
					&& (xvr.getTimeSeconds() >= 100 || xvr.getTimeSeconds() <= 100
							&& !worker.equals(ExplorerManager.getExplorer()))
					&& (!worker.isExplorer() || (building != null && building.getType().isBunker()))) {

				if (!RepairersManager.isProfessionalRepairer(worker)) {
					if (!worker.equals(WorkerManager.getGuyToChaseOthers()) || TerranOffensiveBunker.isStrategyActive()) {
						freeWorkers.add(worker);
					}
				}

				// && (xvr.getTimeSeconds() <= 100 || (xvr.getTimeSeconds() >
				// 100
				// && !WorkerManager.isProfessionalRepairer(worker) && !worker
				// .equals(WorkerManager.getGuyToChaseOthers())))) {
			}
		}
		// System.out.println("freeWorkers.size() = " + freeWorkers.size());

		// Return the closest builder to the tile
		Unit builder = xvr.getUnitNearestFromList(buildTile, freeWorkers, true, false);
		// if (builder == null) {
		// System.out.println("Chosen builder: " + builder + " / from: " +
		// freeWorkers.size());
		// }

		return builder;
	}

	public static Unit getRandomWorker() {
		for (Unit unit : xvr.getBwapi().getMyUnits()) {
			if (unit.getTypeID() == UnitManager.WORKER.ordinal() && !unit.isConstructing()) {
				return unit;
			}
		}
		return null;
	}

}

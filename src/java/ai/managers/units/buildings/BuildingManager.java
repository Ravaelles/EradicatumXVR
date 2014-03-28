package ai.managers.units.buildings;

import java.util.HashMap;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.managers.constructing.Constructing;
import ai.managers.units.UnitManager;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranComsatStation;

public class BuildingManager {

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static void act(Unit building) {
		UnitType type = building.getType();
		if (type == null) { // || !buildingType.isBuilding()
			return;
		}

		// Remove buildings that have full health
		BuildingRepairManager.removeHealthyBuildingsFromRepairQueue();

		// Cancel construction of buildings under attack and severely damaged
		handleUnfinishedBuildings(building, type);

		// ===========================================
		// Repairing of building works slightly differently than repairing units
		BuildingRepairManager.handleBuildingsNeedingRepair(building);

		// If under attack always call for help
		if (building.isUnderAttack()) {
			UnitActions.callForHelp(building, true);
		}

		// ==========================================
		// Act with SPECIAL BUILDINGS
		if (type.getID() == UnitTypes.Terran_Comsat_Station.ordinal()) {
			TerranComsatStation.act(building);
		}
	}

	// =========================================================

	private static HashMap<Unit, Integer> _buildingsInConstructionHP = new HashMap<>();

	private static void handleUnfinishedBuildings(Unit building, UnitType buildingType) {

		// If building still isn't completed check if it has a builder,
		// something might have accidentally killed him, like e.g. a lonely
		// lurker, looking for some love
		if (!building.isCompleted() && !building.getType().isAddon()) {
			Unit builder = getBuilderFor(building);
			if (builder == null
					|| (builder != null && (!builder.isExists() || !builder.isConstructing()))) {
				Unit newBuilder = xvr.getOptimalBuilder(building);
				if (newBuilder != null) {
					UnitActions.rightClick(newBuilder, building);
				}
			}
		}

		// System.out.println("TEST " + building.isUnderAttack() + " " +
		// !building.isCompleted());
		if (!building.isCompleted() && isBuildingAttacked(building)) {
			// System.out.println("BUILDING ATTACKED: " + building.getName());
			boolean shouldCancelConstruction = false;

			// If this is normal building and it's severely damaged.
			if (building.getHP() < 0.4 * buildingType.getMaxHitPoints()) {
				shouldCancelConstruction = true;
			}

			// If it's base and...
			if (buildingType.isBase()) {

				// // It's lil damaged and there's not too many units nearby.
				// if (building.getHitPoints() < 0.8 * buildingType
				// .getMaxHitPoints()
				// && xvr.getNumberOfUnitsInRadius(building, 10,
				// xvr.getUnitsNonWorker()) < 3) {
				// shouldCancelConstruction = true;
				// }
				//
				// // It's pretty damaged, better scrap it
				// if (building.getHitPoints() < 0.55 * buildingType
				// .getMaxHitPoints()
				// && xvr.getNumberOfUnitsInRadius(building, 10,
				// xvr.getUnitsNonWorker()) < 7) {
				// shouldCancelConstruction = true;
				// }

				// It's pretty damaged, better scrap it
				shouldCancelConstruction = true;
			}

			if (shouldCancelConstruction && !buildingType.isBunker()) {
				System.out.println("CANCELLING CONSTRUCTION: " + building.getName() + " at "
						+ building.toStringLocation());
				xvr.getBwapi().cancelConstruction(building.getID());
			}
		}
	}

	private static boolean isBuildingAttacked(Unit building) {
		if (!_buildingsInConstructionHP.containsKey(building)) {
			_buildingsInConstructionHP.put(building, building.getHP());
			return false;
		} else {
			int oldHP = _buildingsInConstructionHP.get(building);
			_buildingsInConstructionHP.put(building, building.getHP());
			return building.getHP() < oldHP;
		}
	}

	public static Unit getBuilderFor(Unit building) {
		for (Unit worker : xvr.getUnitsOfType(UnitManager.WORKER)) {
			if (worker.isConstructing() && worker.getBuildUnitID() == building.getID()) {
				return worker;
			}
		}
		return null;
	}

	/**
	 * If we're building a building of this type it will return progress (in
	 * percent) of the closest construction to the finish. It's basically hit
	 * points / max hit points.
	 */
	public static int countConstructionProgress(UnitTypes buildingType) {
		int maxProgress = 0;
		if (Constructing.weAreBuilding(buildingType)) {
			for (Unit building : xvr.getUnitsOfType(buildingType)) {
				if (!building.isCompleted()) {
					int progress = 100 * building.getHP()
							/ buildingType.getType().getMaxHitPoints();
					if (progress > maxProgress) {
						maxProgress = progress;
					}
				}
			}
		}
		return maxProgress;
	}

	public static Unit getNextBaseBuilder() {
		for (Unit worker : xvr.getUnitsOfType(UnitManager.WORKER)) {
			if (worker.getBuildTypeID() == TerranCommandCenter.getBuildingType().ordinal()) {
				return worker;
			}
		}
		return null;
	}

}

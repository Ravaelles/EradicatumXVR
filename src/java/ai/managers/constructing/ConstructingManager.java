package ai.managers.constructing;

import ai.core.XVR;
import ai.terran.TerranAcademy;
import ai.terran.TerranArmory;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranComsatStation;
import ai.terran.TerranEngineeringBay;
import ai.terran.TerranFactory;
import ai.terran.TerranMachineShop;
import ai.terran.TerranMissileTurret;
import ai.terran.TerranRefinery;
import ai.terran.TerranScienceFacility;
import ai.terran.TerranSupplyDepot;

public class ConstructingManager {

	static XVR xvr = XVR.getInstance();

	private static int _actCounter = 0;

	// private static int _lastCheckedForProlongated = -1;

	// ====================================

	public static void act() {
		_actCounter++;
		if (_actCounter >= 9) {
			_actCounter = 0;
		}

		// Store info about constructing given building for 3 acts, then
		// remove all data
		if (ConstructingHelper._recentConstructionsCounter++ >= 6) {
			ConstructingHelper.resetInfoAboutConstructions();
		}

		// Check only every N frames
		boolean shouldBuildHQ = TerranCommandCenter.shouldBuild();
		boolean canBuildOtherThingThanHQ = !shouldBuildHQ || xvr.canAfford(420);

		TerranBarracks.buildIfNecessary();
		TerranBunker.buildIfNecessary();

		if (_actCounter == 0 && shouldBuildHQ) {
			TerranCommandCenter.buildIfNecessary();
		} else if (_actCounter < 5 && canBuildOtherThingThanHQ) {
			TerranAcademy.buildIfNecessary();
			TerranFactory.buildIfNecessary();
			TerranComsatStation.buildIfNecessary();
			// TerranControlTower.buildIfNecessary();
			TerranEngineeringBay.buildIfNecessary();
			TerranRefinery.buildIfNecessary();
		} else if (canBuildOtherThingThanHQ) {
			TerranMissileTurret.buildIfNecessary();
			TerranSupplyDepot.buildIfNecessary();
			// TerranStarport.buildIfNecessary();
			TerranMachineShop.buildIfNecessary();
			TerranArmory.buildIfNecessary();
			TerranScienceFacility.buildIfNecessary();
		}

		// It can happen that damned worker will stuck somewhere (what a retard)
		// if (xvr.getTimeSeconds() - _lastCheckedForProlongated >= 8) {
		// checkForProlongatedConstructions();
		// _lastCheckedForProlongated = xvr.getTimeSeconds();
		// }
	}
	// private static void checkForProlongatedConstructions() {
	// int now = xvr.getFrames();
	// for (Unit builder : _recentConstructionsTimes.keySet()) {
	// if (!builder.isConstructing()) {
	// continue;
	// }
	//
	// if (now - _recentConstructionsTimes.get(builder) >
	// PROLONGATED_CONSTRUCTION_TIME) {
	// MapPoint buildTile = _recentConstructionsPlaces.get(builder);
	// UnitTypes building = _recentConstructionsUnitToType.get(builder);
	//
	// // Issue new construction order
	// Constructing.constructBuilding(xvr, building, buildTile);
	//
	// // Cancel previous construction by moving the unit
	// UnitActions.moveTo(builder, xvr.getFirstBase());
	// }
	// }
	// }

}

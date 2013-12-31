package ai.managers.constructing;

import java.util.HashMap;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.terran.TerranAcademy;
import ai.terran.TerranArmory;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranComsatStation;
import ai.terran.TerranControlTower;
import ai.terran.TerranEngineeringBay;
import ai.terran.TerranFactory;
import ai.terran.TerranMachineShop;
import ai.terran.TerranMissileTurret;
import ai.terran.TerranRefinery;
import ai.terran.TerranScienceFacility;
import ai.terran.TerranStarport;
import ai.terran.TerranSupplyDepot;

public class ConstructingManager {

	private static XVR xvr = XVR.getInstance();

	private static final int PROLONGATED_CONSTRUCTION_TIME = 350; // in fps

	private static HashMap<UnitTypes, Unit> _recentConstructionsInfo = new HashMap<>();
	private static HashMap<UnitTypes, MapPoint> _recentConstructionsPlaces = new HashMap<>();
	private static HashMap<Unit, UnitTypes> _recentConstructionsUnitToType = new HashMap<>();
	private static HashMap<Unit, Integer> _recentConstructionsTimes = new HashMap<>();

	private static int _recentConstructionsCounter = 0;
	private static int _actCounter = 0;
	private static int _lastCheckedForProlongated = -1;

	// ====================================

	public static void act() {
		_actCounter++;
		if (_actCounter >= 3) {
			_actCounter = 0;
		}

		// Store info about constructing given building for 3 acts, then
		// remove all data
		if (_recentConstructionsCounter++ >= 3) {
			resetInfoAboutConstructions();
		}

		// Check only every N frames
		boolean shouldBuildHQ = TerranCommandCenter.shouldBuild();
		boolean canBuildOtherThingThanHQ = !shouldBuildHQ || xvr.canAfford(550);

		if (_actCounter == 0 && (shouldBuildHQ && !xvr.canAfford(550))) {
			TerranCommandCenter.buildIfNecessary();
		} else if (_actCounter == 1 && canBuildOtherThingThanHQ) {
			TerranAcademy.buildIfNecessary();
			TerranBunker.buildIfNecessary();
			TerranFactory.buildIfNecessary();
			TerranBarracks.buildIfNecessary();
			TerranComsatStation.buildIfNecessary();
			TerranControlTower.buildIfNecessary();
			TerranEngineeringBay.buildIfNecessary();
			TerranRefinery.buildIfNecessary();
		} else if (canBuildOtherThingThanHQ) {
			TerranBarracks.buildIfNecessary();
			TerranBunker.buildIfNecessary();
			TerranMissileTurret.buildIfNecessary();
			TerranSupplyDepot.buildIfNecessary();
			TerranStarport.buildIfNecessary();
			TerranMachineShop.buildIfNecessary();
			TerranArmory.buildIfNecessary();
			TerranScienceFacility.buildIfNecessary();
		}

		// It can happen that damned worker will stuck somewhere (what a retard)
		if (xvr.getTimeSeconds() - _lastCheckedForProlongated >= 8) {
			checkForProlongatedConstructions();
			_lastCheckedForProlongated = xvr.getTimeSeconds();
		}
	}

	private static void checkForProlongatedConstructions() {
		int now = xvr.getFrames();
		for (Unit builder : _recentConstructionsTimes.keySet()) {
			if (!builder.isConstructing()) {
				continue;
			}

			if (now - _recentConstructionsTimes.get(builder) > PROLONGATED_CONSTRUCTION_TIME) {
				MapPoint buildTile = _recentConstructionsPlaces.get(builder);
				UnitTypes building = _recentConstructionsUnitToType.get(builder);

				// Issue new construction order
				Constructing.constructBuilding(xvr, building, buildTile);

				// Cancel previous construction by moving the unit
				UnitActions.moveTo(builder, xvr.getFirstBase());
			}
		}
	}

	public static boolean weAreBuilding(UnitTypes type) {
		if (_recentConstructionsInfo.containsKey(type)) {
			return true;
		}
		for (Unit unit : xvr.getBwapi().getMyUnits()) {
			if ((!unit.isCompleted() && unit.getTypeID() == type.ordinal())
					|| unit.getBuildTypeID() == type.ordinal()) {
				return true;
			}
		}
		return false;
	}

	private static void resetInfoAboutConstructions() {
		_recentConstructionsCounter = 0;
		_recentConstructionsInfo.clear();
	}

	protected static void addInfoAboutConstruction(UnitTypes building, Unit builder,
			MapPoint buildTile) {
		_recentConstructionsCounter = 0;
		_recentConstructionsInfo.put(building, builder);
		_recentConstructionsPlaces.put(building, buildTile);
		_recentConstructionsUnitToType.put(builder, building);
		_recentConstructionsTimes.put(builder, xvr.getFrames());
		ShouldBuildCache.cacheShouldBuildInfo(building, false);
	}

}

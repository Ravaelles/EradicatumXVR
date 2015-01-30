package ai.managers.constructing;

import java.util.HashMap;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;

public class ConstructingHelper {

	protected static XVR xvr = XVR.getInstance();

	// private static final int PROLONGATED_CONSTRUCTION_TIME = 350; // in fps

	protected static HashMap<UnitTypes, Unit> _recentConstructionsInfo = new HashMap<>();
	protected static HashMap<UnitTypes, MapPoint> _recentConstructionsPlaces = new HashMap<>();
	protected static HashMap<Unit, UnitTypes> _recentConstructionsUnitToType = new HashMap<>();
	protected static HashMap<Unit, Integer> _recentConstructionsTimes = new HashMap<>();

	protected static int _recentConstructionsCounter = 0;

	public static boolean weAreBuilding(UnitTypes type) {
		if (_recentConstructionsInfo.containsKey(type)) {
			return true;
		}
		for (Unit unit : ConstructingManager.xvr.getBwapi().getMyUnits()) {
			if ((!unit.isCompleted() && unit.getTypeID() == type.ordinal())
					|| unit.getBuildTypeID() == type.ordinal()) {
				return true;
			}
		}
		return false;
	}

	protected static void resetInfoAboutConstructions() {
		_recentConstructionsCounter = 0;
		_recentConstructionsInfo.clear();
	}

	protected static void addInfoAboutConstruction(UnitTypes building, Unit builder,
			MapPoint buildTile) {
		// if (building.getType().isBase()) {
		// System.out.println("BASE: " + builder);
		// }

		_recentConstructionsCounter = 0;
		_recentConstructionsInfo.put(building, builder);
		_recentConstructionsPlaces.put(building, buildTile);
		_recentConstructionsUnitToType.put(builder, building);
		_recentConstructionsTimes.put(builder, xvr.getFrames());
		ShouldBuildCache.cacheShouldBuildInfo(building, false);
	}

	public static HashMap<UnitTypes, MapPoint> get_recentConstructionsPlaces() {
		return _recentConstructionsPlaces;
	}

}

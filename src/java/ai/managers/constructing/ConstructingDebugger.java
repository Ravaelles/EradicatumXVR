package ai.managers.constructing;

import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.terran.TerranSupplyDepot;

public class ConstructingDebugger {

	public static void debug() {
		Constructing.debugConstruction = true;

		UnitTypes type = TerranSupplyDepot.getBuildingType();
		// UnitTypes type = TerranBarracks.getBuildingType();
		MapPoint near = xvr.getFirstBase().translate(96, 0);

		Constructing.getLegitTileToBuildNear(type, near, 5, 70);

		Constructing.debugConstruction = false;
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

}

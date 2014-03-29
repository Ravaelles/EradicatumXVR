package ai.handling.missions;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;

public class MissionProtectBase {

	public static final int NTH_MARINE = 1;

	@SuppressWarnings("unused")
	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static void actProtectBase(Unit unit, Unit base) {
		if (unit == null || base == null) {
			return;
		}

		// Define point where to protect base
		MapPoint protectPoint = definePointForBaseProtection(base);
		if (unit.distanceTo(protectPoint) > 3) {
			UnitActions.attackTo(unit, protectPoint);
			unit.setAiOrder("Protect the base");
		}
	}

	private static MapPoint definePointForBaseProtection(Unit base) {
		// Unit nearestBunker =
		// xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), base);
		// if (nearestBunker != null) {
		// return MapPointInstance.getPointBetween(base, nearestBunker, 95);
		// } else {
		// return base.translate(2, -1);
		return base;
		// }
	}

}

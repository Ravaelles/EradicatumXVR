package ai.managers.units.army.specialforces;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.missions.MissionProtectBase;

public class SpecialForcesManager {

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static boolean tryActingSpecialForceIfNeeded(Unit unit) {
		if (SpecialForces.isSpecialForce(unit)) {
			return act(unit);
		}
		return false;
	}

	// =========================================================

	public static boolean act(Unit unit) {
		MissionType mission = SpecialForces.getMissionForUnit(unit);

		if (mission == MissionType.PROTECT_MAIN_BASE) {
			MissionProtectBase.actProtectBase(unit, xvr.getFirstBase());
		}

		return false;
	}

	// =========================================================

}

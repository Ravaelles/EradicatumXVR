package ai.managers.units.army.specialforces;

import java.util.HashMap;

import jnibwapi.model.Unit;
import ai.utils.Units;

public class SpecialForces {

	private static HashMap<Unit, MissionType> specialForces = new HashMap<>();
	private static HashMap<MissionType, Units> missions = new HashMap<>();

	// =========================================================

	public static void assignMainBaseProtector(Unit unit) {
		newMision(MissionType.PROTECT_MAIN_BASE, unit);
	}

	private static void newMision(MissionType mission, Unit unit) {
		Units units = new Units(unit);

		specialForces.put(unit, mission);
		missions.put(mission, units);
	}

	// =========================================================

	public static boolean isMainBaseProtector(Unit unit) {
		return specialForces.get(unit) == MissionType.PROTECT_MAIN_BASE;
	}

	public static boolean hasMainBaseProtector() {
		return missions.containsKey(MissionType.PROTECT_MAIN_BASE);
	}

	public static boolean isSpecialForce(Unit unit) {
		return specialForces.containsKey(unit);
	}

	public static MissionType getMissionForUnit(Unit unit) {
		return specialForces.get(unit);
	}

}

package ai.managers.units.army.tanks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;

public class EnemyTanksManager {

	private static final double SAFE_DISTANCE_FROM_ENEMY_TANK = 12.7;
	private static final double ALWAYS_MOVE_TO_ENEMY_TANK_IF_CLOSER_THAN = 6;
	private static final double ALWAYS_ATTACK_ENEMY_TANK_IF_CLOSER_THAN = 1.1;
	private static final int MAXIMUM_TANKS_TO_ENGAGE_WITH_NORMAL_UNITS = 3;

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	private static HashMap<Integer, Unit> enemyTankDiscovered = new HashMap<>();
	private static HashMap<Unit, MapPoint> enemyTankPositions = new HashMap<>();

	// =========================================================

	public static boolean tryAvoidingEnemyTanks(Unit unit) {

		// Our own tanks should engage enemy tanks.
		if (unit.getType().isTank()) {
			return false;
		}

		Collection<Unit> allKnownEnemyTanks = getEnemyTanksThatAreDangerouslyClose(unit);

		for (Unit enemyTank : allKnownEnemyTanks) {
			double distance = enemyTank.distanceTo(unit);
			if (distance < ALWAYS_MOVE_TO_ENEMY_TANK_IF_CLOSER_THAN) {
				if (distance < ALWAYS_ATTACK_ENEMY_TANK_IF_CLOSER_THAN) {
					UnitActions.attackTo(unit, enemyTank);
				} else {
					UnitActions.moveTo(unit, enemyTank);
				}
			}
		}

		if (allKnownEnemyTanks.size() <= MAXIMUM_TANKS_TO_ENGAGE_WITH_NORMAL_UNITS) {
			return false;
		}

		for (Unit enemyTank : allKnownEnemyTanks) {
			UnitActions.moveAwayFrom(unit, enemyTank);
			unit.setAiOrder("Avoid siege tank");
			return true;
		}

		return false;
	}

	// =========================================================

	private static Collection<Unit> getEnemyTanksThatAreDangerouslyClose(Unit unit) {
		ArrayList<Unit> tanksInRange = new ArrayList<>();

		for (Unit enemyTank : getEnemyTanks()) {
			if (enemyTank.getType().isTankSieged()
					|| (!enemyTank.isSieged() && !enemyTank.isInterruptable())) {
				if (enemyTank.distanceTo(unit) < SAFE_DISTANCE_FROM_ENEMY_TANK) {
					tanksInRange.add(enemyTank);
				}
			}
		}

		return tanksInRange;
	}

	private static Collection<Unit> getEnemyTanks() {
		Collection<Unit> knownTanks = xvr.getEnemyUnitsOfType(
				UnitTypes.Terran_Siege_Tank_Siege_Mode, UnitTypes.Terran_Siege_Tank_Tank_Mode);
		// knownTanks.addAll(enemyTankPositions.values());
		return knownTanks;
	}

	public static void updateTankPosition(Unit enemyTank) {
		// enemyTankDiscovered.put(key, value)
	}
}

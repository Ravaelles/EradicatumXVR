package ai.managers.units.army.tanks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitActions;

public class EnemyTanksManager {

	private static final double SAFE_DISTANCE_FROM_ENEMY_TANK = 13.5;
	private static final double ALWAYS_MOVE_TO_ENEMY_TANK_IF_CLOSER_THAN = 8;
	private static final double ALWAYS_ATTACK_ENEMY_TANK_IF_CLOSER_THAN = 1.1;
	private static final int MAXIMUM_TANKS_TO_ENGAGE_WITH_NORMAL_UNITS = 3;

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	private static HashMap<Integer, Unit> enemyTanksDiscovered = new HashMap<>();
	private static HashMap<Integer, MapPointInstance> enemyTanksPositions = new HashMap<>();

	// =========================================================

	public static boolean tryAvoidingEnemyTanks(Unit unit) {

		// Our own tanks should engage enemy tanks.
		if (unit.getType().isTank()) {
			return false;
		}

		Collection<MapPoint> allKnownEnemyTanks = getEnemyTanksThatAreDangerouslyClose(unit);

		for (MapPoint enemyTank : allKnownEnemyTanks) {
			double distance = enemyTank.distanceTo(unit);
			if (distance < ALWAYS_MOVE_TO_ENEMY_TANK_IF_CLOSER_THAN) {
				if (allKnownEnemyTanks.size() <= MAXIMUM_TANKS_TO_ENGAGE_WITH_NORMAL_UNITS) {
					if (distance < ALWAYS_ATTACK_ENEMY_TANK_IF_CLOSER_THAN) {
						unit.setAiOrder("Attack tank");
						UnitActions.attackTo(unit, enemyTank);
						return true;
					} else {
						UnitActions.moveTo(unit, enemyTank);
						unit.setAiOrder("Engage enemy tank");
						return true;
					}
				}
			}
		}

		for (MapPoint enemyTank : allKnownEnemyTanks) {
			if (enemyTank.distanceTo(unit) < SAFE_DISTANCE_FROM_ENEMY_TANK) {
				UnitActions.moveAwayFrom(unit, enemyTank);
				unit.setAiOrder("Avoid siege tank");
				return true;
			}
		}

		return false;
	}

	// =========================================================

	private static Collection<MapPoint> getEnemyTanksThatAreDangerouslyClose(Unit unit) {
		ArrayList<MapPoint> tanksInRange = new ArrayList<>();

		for (Object enemyTank : getEnemyTanksKnownIncludingSpeculations()) {
			if (isEnemyTankDangerouslyClose(unit, enemyTank)) {
				MapPoint enemyTankPosition = (MapPoint) enemyTank;
				tanksInRange.add(enemyTankPosition);
			}
		}

		return tanksInRange;
	}

	private static boolean isEnemyTankDangerouslyClose(Unit unit, Object enemy) {

		// Real unit
		if (enemy instanceof Unit) {
			Unit enemyTank = (Unit) enemy;

			if (enemyTank.getType().isTankSieged()
					|| (!enemyTank.isSieged() && !enemyTank.isInterruptable())) {
				if (enemyTank.distanceTo(unit) < SAFE_DISTANCE_FROM_ENEMY_TANK) {
					return true;
				}
			}
		}

		// Speculated unit, with the last known position
		else {
			MapPoint enemyTank = (MapPoint) enemy;

			if (enemyTank.distanceTo(unit) < SAFE_DISTANCE_FROM_ENEMY_TANK) {
				return true;
			}
		}

		return false;
	}

	private static Collection<Object> getEnemyTanksKnownIncludingSpeculations() {
		ArrayList<Object> knownTanks = new ArrayList<>();

		// Add real tanks
		for (Unit tank : xvr.getEnemyUnitsOfType(UnitTypes.Terran_Siege_Tank_Siege_Mode,
				UnitTypes.Terran_Siege_Tank_Tank_Mode)) {
			knownTanks.add(tank);
		}

		// ArrayList<? extends MapPoint> knownTanks = (ArrayList<? extends
		// MapPoint>) xvr
		// .getEnemyUnitsOfType(UnitTypes.Terran_Siege_Tank_Siege_Mode,
		// UnitTypes.Terran_Siege_Tank_Tank_Mode);

		// knownTanks.addAll(enemyTanksPositions.values());
		for (MapPoint tankPosition : enemyTanksPositions.values()) {
			knownTanks.add(tankPosition);
		}

		return knownTanks;
	}

	public static void updateTankPosition(Unit enemyTank) {
		enemyTanksDiscovered.put(enemyTank.getID(), enemyTank);
		enemyTanksPositions.put(enemyTank.getID(), new MapPointInstance(enemyTank));
	}

	public static Collection<MapPointInstance> getSpeculatedTankPositions() {
		return enemyTanksPositions.values();
	}

	public static void unitDestroyed(int unitID) {
		enemyTanksDiscovered.remove(unitID);
		enemyTanksPositions.remove(unitID);
	}

}

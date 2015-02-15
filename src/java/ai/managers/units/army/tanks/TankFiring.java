package ai.managers.units.army.tanks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.army.TargetHandling;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.units.SelectUnits;
import ai.utils.RUtilities;

public class TankFiring {

	private static XVR xvr = XVR.getInstance();

	private static final double INNER_SPLASH_TILES = (double) 10 / 32;
	private static final double MEDIAN_SPLASH_TILES = (double) 25 / 32;
	private static final double OUTER_SPLASH_TILES = (double) 40 / 32;

	private static final int OPTION_BONUS_KILLING_ONE_ENEMY = 40;
	private static final int OPTION_PENALTY_WOUNDING_ONE_HP_OUR_UNIT = 10;
	private static final int OPTION_PENALTY_KILLING_ONE_OUR_UNIT = 100;

	private static final int MINIMUM_OPTION_VALUE = 30;

	// =========================================================

	public static void act() {
		for (Unit tank : SelectUnits.ourTanksSieged().list()) {
			if (tank.isSieged() && tank.getGroundWeaponCooldown() < 4) {
				act(tank);
			}
		}
	}

	private static void act(Unit tank) {
		if (!tank.isStartingAttack() && !tank.isInterruptable()) {
			MapPoint targetForSiegeTank = defineTargetForSiegeTank(tank);
			UnitActions.attackTo(tank, targetForSiegeTank);
			tank.setAiOrder("Aim");
		}
	}

	// =========================================================

	public static MapPoint defineTargetForSiegeTank(Unit unit) {
		Unit target = null;

		target = defineUnitToAttack(unit);
		if (target == null) {
			target = defineBuildingToAttack(unit);
		}

		return target;
	}

	private static Unit defineBuildingToAttack(Unit unit) {

		// Define list of all buildings that are in range of a shoot
		ArrayList<Unit> enemyBuildingsInRange = xvr.getUnitsInRadius(unit, 11,
				xvr.getEnemyBuildings());

		// Define building of highest priority (if possible) like Bunker, Photon
		// Cannon.
		Unit topPriorityBuilding = TargetHandling.findTopPriorityTargetIfPossible(
				enemyBuildingsInRange, true, false);

		Unit target = null;
		if (topPriorityBuilding != null) {
			target = topPriorityBuilding;
		} else {
			target = xvr.getUnitNearestFromList(unit, enemyBuildingsInRange);
		}

		return target;
	}

	private static Unit defineUnitToAttack(Unit unit) {

		// Define list of all units (not buildings) that are in range of a shoot
		ArrayList<Unit> enemyUnitsInRange = xvr.getUnitsInRadius(unit, 11,
				xvr.getEnemyUnitsVisible(true, false));
		if (enemyUnitsInRange.isEmpty()) {
			return null;
		}

		// Remove units that are closer than 2 tiles from Siege Tank
		enemyUnitsInRange.removeAll(xvr.getUnitsInRadius(unit, 2,
				xvr.getEnemyUnitsVisible(true, false)));

		// ===================================================
		// Define building of highest priority (if possible) like Bunker, Photon
		// Cannon.
		Unit importantEnemyUnit = TargetHandling.getImportantEnemyUnitTargetIfPossibleFor(unit,
				enemyUnitsInRange, true, false);

		Unit target = null;
		if (importantEnemyUnit != null) {
			target = importantEnemyUnit;
		} else {
			// target = xvr.getUnitNearestFromList(unit, enemyUnitsInRange);
			target = defineOptimalStandardTarget(unit, enemyUnitsInRange);
		}

		return target;
	}

	private static Unit defineOptimalStandardTarget(Unit unit, ArrayList<Unit> enemiesInRange) {
		Map<Unit, Double> bestTargets = defineBestTargets(unit, enemiesInRange);
		if (!bestTargets.isEmpty()) {
			Unit bestTarget = (Unit) RUtilities.getFirstMapElement(bestTargets);
			if (bestTargets.get(bestTarget) >= MINIMUM_OPTION_VALUE) {
				return bestTarget;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	private static Map<Unit, Double> defineBestTargets(Unit unit, ArrayList<Unit> enemiesInRange) {
		Map<Unit, Double> enemiesToScore = new HashMap<>();

		for (Unit enemy : enemiesInRange) {
			if (enemy.getType().isLarvaOrEgg()) {
				continue;
			}
			enemiesToScore.put(enemy, evaluateOptionShootAtUnit(enemy, enemiesInRange));
		}

		enemiesToScore = RUtilities.sortByValue(enemiesToScore, false);
		return enemiesToScore;
	}

	private static ArrayList<Unit> _unitsThatAreKilledIfThisOptionIsChosen;

	private static Double evaluateOptionShootAtUnit(Unit enemy, Collection<Unit> enemiesInRange) {
		double optionValue = 0;
		_unitsThatAreKilledIfThisOptionIsChosen = new ArrayList<>();

		// First, get the list of our units that will be affected by explosion
		// in this place
		ArrayList<Unit> ourUnitsInRange = xvr.getUnitsInRadius(enemy, OUTER_SPLASH_TILES, xvr
				.getBwapi().getMyUnits());

		// ==============
		// Define damage value

		int outerDamageValue = 17; // 70 * *25*% = 17
		int medianExtraDamageValue = outerDamageValue; // 70 * (25 + *25*)%
		int innerExtraDamageValue = outerDamageValue * 2; // 70 * (50 + *50*)%

		// ==============
		// OUTER DAMAGE
		optionValue += calculatePointsFromDamageToEnemy(outerDamageValue, enemiesInRange);
		optionValue -= calculatePointsPenaltyFromDamageToUs(outerDamageValue, ourUnitsInRange);

		// ==============
		// MEDIAN DAMAGE
		optionValue += calculatePointsFromDamageToEnemy(medianExtraDamageValue,
				xvr.getUnitsInRadius(enemy, MEDIAN_SPLASH_TILES, enemiesInRange));
		optionValue -= calculatePointsPenaltyFromDamageToUs(outerDamageValue,
				xvr.getUnitsInRadius(enemy, MEDIAN_SPLASH_TILES, ourUnitsInRange));

		// ==============
		// INNER DAMAGE
		optionValue += calculatePointsFromDamageToEnemy(innerExtraDamageValue,
				xvr.getUnitsInRadius(enemy, INNER_SPLASH_TILES, enemiesInRange));
		optionValue -= calculatePointsPenaltyFromDamageToUs(outerDamageValue,
				xvr.getUnitsInRadius(enemy, INNER_SPLASH_TILES, ourUnitsInRange));

		// System.out.println("Option (#" + optionValue + "#): shoot at " +
		// enemy.getName()
		// + ", there are enemies: " + enemiesInRange.size());

		return optionValue;
	}

	private static double calculatePointsFromDamageToEnemy(int damageValue,
			Collection<Unit> enemiesInRange) {
		int points = 0;
		for (Unit unit : enemiesInRange) {
			int totalLife = unit.getHP() + unit.getShields();
			int damageInflicted = Math.min(damageValue, totalLife);

			points += damageInflicted;

			// If by this shoot, you'll kill this unit, modify points for this
			// option
			if (damageValue >= totalLife && !_unitsThatAreKilledIfThisOptionIsChosen.contains(unit)) {
				_unitsThatAreKilledIfThisOptionIsChosen.add(unit);
				points += OPTION_BONUS_KILLING_ONE_ENEMY;
			}
		}
		return points;
	}

	private static double calculatePointsPenaltyFromDamageToUs(int damageValue,
			ArrayList<Unit> ourUnitsInRange) {
		int points = 0;
		for (Unit unit : ourUnitsInRange) {
			int totalLife = unit.getHP() + unit.getShields();
			int damageInflicted = Math.min(damageValue, totalLife);

			points += damageInflicted * OPTION_PENALTY_WOUNDING_ONE_HP_OUR_UNIT;

			// If by this shoot, you'll kill this unit, modify points for this
			// option
			if (damageValue >= totalLife && !_unitsThatAreKilledIfThisOptionIsChosen.contains(unit)) {
				_unitsThatAreKilledIfThisOptionIsChosen.add(unit);
				points += OPTION_PENALTY_KILLING_ONE_OUR_UNIT;
			}
		}
		return points;
	}

}

package ai.managers.units.enemy;

import jnibwapi.model.Unit;
import ai.handling.map.MapExploration;
import ai.managers.strategy.StrategyManager;

public class EnemyUnits {

	public static void newUnitDiscovered(Unit unit) {

		// Add info that we discovered enemy unit
		MapExploration.enemyUnitDiscovered(unit);

		// System.out.println("Unit discover: " + (unit != null ? unit.getName()
		// : "null"));

		// Disallow Vultures to roam on the map if enemy has tanks
		if (StrategyManager.CAN_VULTURES_ROAM) {
			if (unit.getGroundWeapon().getMaxRangeInTiles() > 1) {
				// if (unit.isTank()) {
				StrategyManager.CAN_VULTURES_ROAM = false;
			}
		}
	}

}

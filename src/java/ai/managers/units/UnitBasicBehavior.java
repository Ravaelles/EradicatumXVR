package ai.managers.units;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.handling.units.UnitActions;
import ai.managers.BuildingManager;
import ai.managers.StrategyManager;
import ai.terran.TerranSiegeTank;
import ai.terran.TerranVulture;
import ai.terran.TerranWraith;

public class UnitBasicBehavior {

	protected static void act(Unit unit) {
		UnitType unitType = unit.getType();
		if (unitType == null) {
			return;
		}

		// Flying unit
		if (unitType.isFlyer()) {

			if (unit.isHidden() ) {
				// TOP PRIORITY: Act when enemy detector or some AA building is
				// nearby: just run away, no matter what.
				if (UnitActions.runFromEnemyDetectorOrDefensiveBuildingIfNecessary(unit, true,
						true, true)) {
					return;
				}
			}
			else {
				if (UnitActions.runFromEnemyDetectorOrDefensiveBuildingIfNecessary(unit, false,
						true, true)) {
					return;
				}
			}
		}

		// ======================================
		// OVERRIDE COMMANDS FOR SPECIFIC UNITS

		// If unit is Building
		if (unitType.isBuilding()) {
			BuildingManager.act(unit);
		}

		// Vulture
		else if (unitType.isVulture()) {
			TerranVulture.act(unit);
			return;
		}

		// ======================================
		// STANDARD ARMY UNIT COMMANDS
		else {

			// If unit has personalized order
			if (unit.getCallForHelpMission() != null) {
				UnitManager.actWhenOnCallForHelpMission(unit);
			}

			// Standard action for unit
			else {

				// If we're ready to total attack
				if (StrategyManager.isAttackPending()) {
					UnitManager.actWhenMassiveAttackIsPending(unit);
				}

				// Standard situation
				else {
					UnitManager.actWhenNoMassiveAttack(unit);
				}
			}
		}

		// ======================================
		// SPECIFIC ACTIONS for units, but DON'T FULLY OVERRIDE standard
		// behavior

		// Tank
		if (unitType.isTank()) {
			TerranSiegeTank.act(unit);
		}

		// Wraith
		else if (unitType.isWraith()) {
			TerranWraith.act(unit);
		}
	}

}

package ai.handling.strength;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.map.MapExploration;

public class StrengthComparison {

	private static int ourRelativeStrength = -1;
	private static int ourSupply = -1;
	private static int enemySupply = -1;

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static void recalculateOurRelativeStrength() {
		int ourStrength = (int) StrengthComparison.evaluateOurForces();
		int enemyStrength = (int) StrengthComparison.evaluateKnownEnemyForces();

		int armyRelativeStrength = -1;
		if (enemyStrength > 0) {
			armyRelativeStrength = 100 * ourStrength / enemyStrength;
		} else {
		}

		ourRelativeStrength = armyRelativeStrength;
	}

	public static void recalculateSupply() {
		ourSupply = xvr.getSuppliesUsed();
		enemySupply = recalculateEnemySupply();
	}

	// =========================================================

	private static int recalculateEnemySupply() {
		int enemySupply = 0;

		for (Unit enemy : MapExploration.getEnemyUnitsDiscovered()) {
			UnitType type = enemy.getType();
			if (!type.isBuilding() && !type.isSpiderMine() && !type.isLarvaOrEgg()) {
				enemySupply += type.getSupplyRequired();

				// Probes are counted x2... dunno why. Maybe it's my bug, maybe
				// it's not.
				if (type.isWorker()) {
					enemySupply--;
				}
			}
		}

		return enemySupply;
	}

	public static double evaluateKnownEnemyForces() {
		double total = 0;

		for (Unit enemy : MapExploration.getEnemyUnitsDiscovered()) {
			UnitType type = enemy.getType();
			if (!type.isBuilding() && !type.isSpiderMine() && !type.isLarvaOrEgg()) {
				total += StrengthRatio.calculateUnitStrengthAsSingleValue(enemy, true);
			}
		}

		return total;
	}

	public static double evaluateOurForces() {
		double total = 0;

		for (Unit enemy : XVR.getInstance().getUnitsArmy()) {
			UnitType type = enemy.getType();
			if (!type.isBuilding() && !type.isSpiderMine() && !type.isLarvaOrEgg()) {
				total += StrengthRatio.calculateUnitStrengthAsSingleValue(enemy, false);
			}
		}

		return total;
	}

	public static int getOurRelativeStrength() {
		return ourRelativeStrength;
	}

	public static int getOurSupply() {
		return ourSupply;
	}

	public static int getEnemySupply() {
		return enemySupply;
	}

}

package ai.handling.enemy;

import ai.core.XVR;
import ai.managers.units.army.ArmyCreationManager;
import ai.strategies.TerranInfantryRush;
import ai.strategies.TerranOffensiveBunker;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;

public class AdaptStrategyToEnemy {

	private static XVR xvr = XVR.getInstance();

	public static void setEnemyIsProtoss() {
		if (!xvr.getENEMY().getName().contains("Churchill")) {
			TerranOffensiveBunker.activateStrategy();
		}
		// TerranInfantryRush.activateStrategy();

		// =========================================================

		if (TerranOffensiveBunker.isStrategyActive()) {
			TerranOffensiveBunker.applyStrategy();
		} else if (TerranInfantryRush.isStrategyActive()) {
			TerranInfantryRush.applyStrategy();
		} else {

			// BUNKER
			TerranBunker.MAX_STACK = 3;
			TerranBunker.GLOBAL_MAX_BUNKERS = 3;

			// BARRACKS
			TerranBarracks.enemyIsProtoss();
		}
	}

	public static void setEnemyIsZerg() {

		// BUNKER
		TerranBunker.MAX_STACK = 3;
		TerranBunker.GLOBAL_MAX_BUNKERS = 3;

		// BARRACKS
		TerranBarracks.enemyIsZerg();
	}

	public static void setEnemyIsTerran() {
		if (true || xvr.getENEMY().getName().contains("Krasimir")) {
			TerranOffensiveBunker.activateStrategy();
			TerranInfantryRush.activateStrategy();
		} else {
			TerranOffensiveBunker.disableStrategy();
			TerranInfantryRush.disableStrategy();
		}

		// =========================================================

		if (TerranOffensiveBunker.isStrategyActive()) {
			TerranOffensiveBunker.applyStrategy();
		} else if (TerranInfantryRush.isStrategyActive()) {
			TerranInfantryRush.applyStrategy();
		} else {

			// BUNKER
			TerranBunker.MAX_STACK = 1;
			TerranBunker.GLOBAL_MAX_BUNKERS = 1;

			// BARRACKS
			TerranBarracks.enemyIsTerran();
			TerranBarracks.MAX_BARRACKS = 1;

			// UNITS
			ArmyCreationManager.MINIMUM_MARINES = 3;
			ArmyCreationManager.MAXIMUM_MARINES = 3;

			// EXPLORER
			// WorkerManager.EXPLORER_INDEX = 4;
		}
	}

	// =========================================================

}

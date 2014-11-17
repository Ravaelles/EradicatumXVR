package ai.strategies;

import ai.managers.units.army.ArmyCreationManager;
import ai.managers.units.workers.WorkerManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;

public class TerranInfantryRush {

	public static void applyStrategy() {
		isStrategyActive = true;

		// BUNKER
		TerranBunker.MAX_STACK = 0;
		TerranBunker.GLOBAL_MAX_BUNKERS = 0;

		// BARRACKS
		TerranBarracks.enemyIsTerran();
		TerranBarracks.MAX_BARRACKS = 2;

		// UNITS
		ArmyCreationManager.MINIMUM_MARINES = 15;
		ArmyCreationManager.MAXIMUM_MARINES = 15;
		ArmyCreationManager.MINIMUM_MARINES = 15;
		ArmyCreationManager.MAXIMUM_MARINES = 15;

		// EXPLORER
		WorkerManager.EXPLORER_INDEX = 4;
	}

	// =========================================================

	protected static boolean isStrategyActive = false;

	public static boolean isStrategyActive() {
		return isStrategyActive;
	}

}

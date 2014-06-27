package ai.strategies;

import ai.core.XVR;
import ai.managers.units.army.ArmyCreationManager;
import ai.managers.units.workers.WorkerManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;

public class TerranInfantryRush {

	private static XVR xvr = XVR.getInstance();

	private static boolean isStrategyActive = false;

	// =========================================================

	public static void applyStrategy() {

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

	public static boolean isStrategyActive() {
		return isStrategyActive;
	}

	public static void activateStrategy() {
		isStrategyActive = true;
	}

	public static void disableStrategy() {
		isStrategyActive = false;
	}

}

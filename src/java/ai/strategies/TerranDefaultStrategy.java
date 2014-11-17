package ai.strategies;

import ai.managers.units.army.ArmyCreationManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;

public class TerranDefaultStrategy {

	public static void applyStrategy_againstProtoss() {
		isStrategyActive = true;

		// BUNKER
		TerranBunker.MAX_STACK = 3;
		TerranBunker.GLOBAL_MAX_BUNKERS = 3;

		// BARRACKS
		TerranBarracks.enemyIsProtoss();
	}

	public static void applyStrategy_againstTerran() {
		isStrategyActive = true;

		// BUNKER
		TerranBunker.MAX_STACK = 1;
		TerranBunker.GLOBAL_MAX_BUNKERS = 1;

		// BARRACKS
		TerranBarracks.enemyIsTerran();
		TerranBarracks.MAX_BARRACKS = 1;

		// UNITS
		ArmyCreationManager.MINIMUM_MARINES = 3;
		ArmyCreationManager.MAXIMUM_MARINES = 3;
	}

	public static void applyStrategy_againstZerg() {
		isStrategyActive = true;

	}

	// =========================================================

	protected static boolean isStrategyActive = false;

	public static boolean isStrategyActive() {
		return isStrategyActive;
	}

}

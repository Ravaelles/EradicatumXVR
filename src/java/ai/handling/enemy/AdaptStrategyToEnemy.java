package ai.handling.enemy;

import ai.managers.units.army.ArmyCreationManager;
import ai.managers.units.workers.WorkerManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;

public class AdaptStrategyToEnemy {

	public static void setEnemyIsProtoss() {

		// BUNKER
		TerranBunker.MAX_STACK = 3;
		TerranBunker.GLOBAL_MAX_BUNKERS = 3;

		// BARRACKS
		TerranBarracks.enemyIsProtoss();
	}

	public static void setEnemyIsZerg() {

		// BUNKER
		TerranBunker.MAX_STACK = 3;
		TerranBunker.GLOBAL_MAX_BUNKERS = 3;

		// BARRACKS
		TerranBarracks.enemyIsZerg();
	}

	public static void setEnemyIsTerran() {

		// BUNKER
		TerranBunker.MAX_STACK = 1;
		TerranBunker.GLOBAL_MAX_BUNKERS = 1;

		// BARRACKS
		TerranBarracks.enemyIsTerran();
		TerranBarracks.MAX_BARRACKS = 1;

		// UNITS
		ArmyCreationManager.MINIMUM_MARINES = 4;
		ArmyCreationManager.MAXIMUM_MARINES = 4;

		// EXPLORER
		WorkerManager.EXPLORER_INDEX--;
	}

	// =========================================================

}

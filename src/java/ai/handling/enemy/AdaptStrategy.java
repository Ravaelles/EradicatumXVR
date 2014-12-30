package ai.handling.enemy;

import ai.core.XVR;
import ai.managers.economy.TechnologyManager;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.ArmyCreationManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranFactory;
import ai.terran.TerranVulture;

public class AdaptStrategy {

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	private static boolean _adapted = false;

	// =========================================================

	public static void adaptToOpponent() {
		if (!_adapted) {
			adaptToRace();
			adaptToBot();
			_adapted = true;
		}
	}

	// =========================================================
	// Hi-level logic

	private static void adaptToBot() {
		String opponentName = xvr.getBwapi().getEnemies().get(0).getName();
		if (opponentName != null) {
			opponentName = opponentName.toLowerCase();
		}

		// opponentName = "tomas vajda";
		// opponentName = "david churchill";

		System.out.println("Opponent name: " + opponentName);
		if (opponentName == null) {
			return;
		}

		// XIMP - Tomas Vajda - Carrier Push
		if (opponentName.contains("vajda")) {
			System.out.println("Special strategy for: Tomas Vajda");

			TerranBunker.GLOBAL_MAX_BUNKERS = 0;
			TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING = 0;
			TerranBarracks.DONT_USE_INFANTRY = true;
			TerranFactory.FORCE_GOLIATHS_INSTEAD_VULTURES = true;
			TerranVulture.CRITICALLY_FEW_VULTURES = 1;
			TechnologyManager.DISABLE_MINES = true;
			StrategyManager.FORCE_CRAZY_ATTACK = true;
		}

		// UAlbertaBot - David Churchill - Zealot rush
		else if (opponentName.contains("churchill")) {
			System.out.println("Special strategy for: Dave Churchill");

			TerranBunker.GLOBAL_MAX_BUNKERS = 2;
			TerranVulture.CRITICALLY_FEW_VULTURES = 6;
		}
	}

	private static void adaptToRace() {
		if (xvr.isEnemyProtoss()) {
			AdaptStrategy.adaptRace_protoss();
		} else if (xvr.isEnemyTerran()) {
			AdaptStrategy.adaptRace_zerg();
		} else if (xvr.isEnemyTerran()) {
			AdaptStrategy.adaptRace_terran();
		}
	}

	// =========================================================
	// Lo-level logic

	private static void adaptRace_protoss() {

		// BUNKER
		// TerranBunker.MAX_STACK = 3;
		// TerranBunker.GLOBAL_MAX_BUNKERS = 2;

		// BARRACKS
		TerranBarracks.enemyIsProtoss();
	}

	private static void adaptRace_zerg() {

		// BUNKER
		TerranBunker.GLOBAL_MAX_BUNKERS = 2;

		// BARRACKS
		TerranBarracks.enemyIsZerg();
	}

	private static void adaptRace_terran() {

		// BUNKER
		TerranBunker.GLOBAL_MAX_BUNKERS = 1;

		// BARRACKS
		TerranBarracks.enemyIsTerran();
		TerranBarracks.MAX_BARRACKS = 1;

		// UNITS
		ArmyCreationManager.MINIMUM_MARINES = 4;
		ArmyCreationManager.MAXIMUM_MARINES = 4;
	}

}

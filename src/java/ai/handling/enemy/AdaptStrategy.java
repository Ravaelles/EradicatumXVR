package ai.handling.enemy;

import ai.core.XVR;
import ai.managers.economy.TechnologyManager;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.ArmyCreationManager;
import ai.managers.units.workers.WorkerManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranComsatStation;
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

		// opponentName = "krasimir";
		// opponentName = "tomas vajda";
		// opponentName = "david churchill";
		// opponentName = "florian richoux";

		System.out.println("Opponent name: " + opponentName);
		if (opponentName == null) {
			return;
		}

		// XIMP - Tomas Vajda - Carrier Push
		if (opponentName.contains("vajda") || opponentName.contains("krasimir")) {
			System.out.println("Special strategy for: Tomas Vajda");

			TerranBunker.GLOBAL_MAX_BUNKERS = 0;
			TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING = 0;
			TerranBarracks.DONT_USE_INFANTRY = true;
			TerranCommandCenter.EXPAND_ONLY_IF_TANKS_MORE_THAN = 20;
			TerranFactory.FORCE_GOLIATHS_INSTEAD_VULTURES = true;
			TerranFactory.ONLY_TANKS = true;
			TerranVulture.CRITICALLY_FEW_VULTURES = 1;
			TerranVulture.DISABLE_VULTURES = true;
			TechnologyManager.DISABLE_MINES = true;
			StrategyManager.FORCE_CRAZY_ATTACK = true;
			StrategyManager.MIN_BATTLE_UNITS_TO_ATTACK = 0;

			WorkerManager.EXPLORER_INDEX = 7;
		}

		// UAlbertaBot - David Churchill - Zealot rush
		else if (opponentName.contains("churchill")) {
			System.out.println("Special strategy for: Dave Churchill");

			TerranBunker.GLOBAL_MAX_BUNKERS = 2;
			TerranVulture.CRITICALLY_FEW_VULTURES = 6;
		}

		// AIUR - Florian Richoux - Quick DT
		else if (opponentName.contains("florian")) {
			System.out.println("Special strategy for: Florian Richoux");
			TerranComsatStation.MODE_ASAP = true;

			TerranBarracks.CRITICALLY_FEW_INFANTRY = 16;
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
		TerranBunker.GLOBAL_MAX_BUNKERS = 1;

		// BARRACKS
		TerranBarracks.CRITICALLY_FEW_INFANTRY = 14;
		TerranBarracks.RATIO_MARINES_PERCENT = 55;
		TerranBarracks.RATIO_FIREBATS_PERCENT = 25;
		TerranBarracks.RATIO_MEDICS_PERCENT = 20;

		// VULTURES
		TerranVulture.CRITICALLY_FEW_VULTURES = 8;
	}

	private static void adaptRace_zerg() {

		// BUNKER
		TerranBunker.GLOBAL_MAX_BUNKERS = 2;

		// BARRACKS
		TerranBarracks.CRITICALLY_FEW_INFANTRY = 6;
	}

	private static void adaptRace_terran() {
		StrategyManager.MIN_BATTLE_UNITS_TO_ATTACK = 18;

		// BUNKER
		TerranBunker.GLOBAL_MAX_BUNKERS = 1;

		// BARRACKS
		TerranBarracks.MAX_BARRACKS = 1;

		// UNITS
		ArmyCreationManager.MINIMUM_MARINES = 4;
		ArmyCreationManager.MAXIMUM_MARINES = 4;
	}

}

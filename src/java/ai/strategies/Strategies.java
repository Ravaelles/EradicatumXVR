package ai.strategies;

import ai.core.XVR;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;

public class Strategies {

	// TOB - Terran Offensive Bunker - build offensive bunker early

	private static XVR xvr = XVR.getInstance();

	private static boolean isTwoPlayersMap;
	private static boolean useTerranOffensiveBunker = false;
	private static boolean useTerranInfantryRush = false;

	// =========================================================

	private static void preStrategyChosen() {

		// Global variables
		isTwoPlayersMap = xvr.getMap().getStartLocations().size() <= 2;

		// =========================================================
		// Local variables

		// =========================================================
		// Decision variables
		// useTerranOffensiveBunker = isTwoPlayersMap;
		useTerranOffensiveBunker = true;
		useTerranInfantryRush = false;
	}

	private static void postStrategyChosen() {

		// Apply specific strategies if needed
		boolean someStrategyApplied = false;
		if (useTerranOffensiveBunker) {
			TerranOffensiveBunker.applyStrategy();
			someStrategyApplied = true;
			System.out.println("Strategy: Offensive Bunker");
		}
		if (useTerranInfantryRush) {
			TerranInfantryRush.applyStrategy();
			someStrategyApplied = true;
			System.out.println("Strategy: Infantry Rush");
		}

		// If no strategy was applied, apply general approach
		if (!someStrategyApplied) {
			if (XVR.isEnemyProtoss()) {
				TerranDefaultStrategy.applyStrategy_againstProtoss();
			} else if (XVR.isEnemyTerran()) {
				TerranDefaultStrategy.applyStrategy_againstTerran();
			} else if (XVR.isEnemyZerg()) {
				TerranDefaultStrategy.applyStrategy_againstZerg();
			}
			System.out.println("Strategy: default");
		}
	}

	// =========================================================

	public static void setEnemyIsProtoss() {
		preStrategyChosen();

		// =========================================================

		if (xvr.getENEMY().getName().contains("Churchill")) {
			useTerranOffensiveBunker = false;
		}

		// =========================================================

		postStrategyChosen();
	}

	public static void setEnemyIsZerg() {
		preStrategyChosen();

		// =========================================================

		useTerranOffensiveBunker = false;

		// =========================================================

		// BUNKER
		TerranBunker.MAX_STACK = 3;
		TerranBunker.GLOBAL_MAX_BUNKERS = 3;

		// BARRACKS
		TerranBarracks.enemyIsZerg();

		// =========================================================

		postStrategyChosen();
	}

	public static void setEnemyIsTerran() {
		preStrategyChosen();

		// =========================================================

		if (xvr.getENEMY().getName().contains("Krasimir")) {
			useTerranOffensiveBunker = true;
			useTerranInfantryRush = true;
		} else {
			useTerranOffensiveBunker = false;
			useTerranInfantryRush = false;
		}

		// =========================================================

		postStrategyChosen();
	}

}

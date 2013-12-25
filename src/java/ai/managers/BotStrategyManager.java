package ai.managers;

public class BotStrategyManager {

	private static boolean expandWithBunkers = false;
	private static boolean expandWithBarracks = true;

	// =============

	/** Build initially forge, then some cannons, only then some gateways. */
	public static boolean isExpandWithBunkers() {
		return expandWithBunkers;
	}

	public static void setExpandWithCannons(boolean expandWithCannons) {
		BotStrategyManager.expandWithBunkers = expandWithCannons;
		BotStrategyManager.expandWithBarracks = false;
	}

	/** Build initially gateways and make zealot push. */
	public static boolean isExpandWithBarracks() {
		return expandWithBarracks;
	}

	public static void setExpandWithGateways(boolean expandWithGateways) {
		BotStrategyManager.expandWithBarracks = expandWithGateways;
		BotStrategyManager.expandWithBunkers = false;
	}
	
}

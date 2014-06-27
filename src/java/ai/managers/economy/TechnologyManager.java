package ai.managers.economy;

import jnibwapi.model.Unit;
import jnibwapi.types.TechType.TechTypes;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType.UpgradeTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.strategies.TerranOffensiveBunker;
import ai.terran.TerranAcademy;
import ai.terran.TerranArmory;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranMachineShop;
import ai.terran.TerranSiegeTank;

public class TechnologyManager {

	public static final TechTypes TANK_SIEGE_MODE = TechTypes.Tank_Siege_Mode;
	public static final TechTypes SPIDER_MINES = TechTypes.Spider_Mines;
	public static final TechTypes CLOAKING_FIELD = TechTypes.Cloaking_Field;
	public static final TechTypes STIMPACKS = TechTypes.Stim_Packs;
	public static final TechTypes DEFENSIVE_MATRIX = TechTypes.Defensive_Matrix;
	public static final UpgradeTypes U238_SHELLS = UpgradeTypes.U_238_Shells;
	public static final UpgradeTypes ION_THRUSTERS = UpgradeTypes.Ion_Thrusters;
	public static final UpgradeTypes VEHICLE_PLATING = UpgradeTypes.Terran_Vehicle_Plating;

	private static final boolean PRIORITY_FOR_SPIDER_MINES_OVER_SIEGE = true;

	private static XVR xvr = XVR.getInstance();

	// private static HashMap<UpgradeTypes, Boolean> knownTechs = new
	// HashMap<UpgradeTypes, Boolean>();

	// =========================================================

	public static void act() {
		UpgradeTypes upgrade;
		TechTypes technology;

		int marines = UnitCounter.getNumberOfUnits(UnitTypes.Terran_Marine);
		int vultures = UnitCounter.getNumberOfUnits(UnitTypes.Terran_Vulture);
		// int tanks = TerranSiegeTank.getNumberOfUnits();
		// int infantry = UnitCounter.getNumberOfInfantryUnits();

		// ======================================================
		// TOP PRIORITY
		// Technologies that are crucial and we don't need to have second base
		// in order to upgrade them
		boolean isPossibleSiegeResearch = isResearchPossible(TANK_SIEGE_MODE);
		boolean isPossibleSpiderResearch = isResearchPossible(SPIDER_MINES);
		boolean forceSiegeResearch = TerranOffensiveBunker.isStrategyActive();

		// Spider Mines
		technology = SPIDER_MINES;
		boolean spiderMinesResearchBonus = (PRIORITY_FOR_SPIDER_MINES_OVER_SIEGE ? (xvr.canAfford(
				170, 100)) : false);
		if (isPossibleSpiderResearch
				&& (!PRIORITY_FOR_SPIDER_MINES_OVER_SIEGE || !isPossibleSiegeResearch)
				&& (vultures >= 1 || spiderMinesResearchBonus)) {
			if (!forceSiegeResearch) {
				tryToResearch(TerranMachineShop.getOneNotBusy(), technology);
			}
		}

		// Tank Siege Mode
		technology = TANK_SIEGE_MODE;
		if (isPossibleSiegeResearch
				&& (PRIORITY_FOR_SPIDER_MINES_OVER_SIEGE && !isPossibleSpiderResearch || TerranSiegeTank
						.getNumberOfUnits() >= 2)) {
			tryToResearch(TerranMachineShop.getOneNotBusy(), technology);
		} else if (forceSiegeResearch && isPossibleSiegeResearch) {
			tryToResearch(TerranMachineShop.getOneNotBusy(), technology);
		}

		// U-238 Shells
		upgrade = U238_SHELLS;
		if (!isPossibleSiegeResearch && vultures > 2 && marines >= 6) {
			tryToUpgrade(TerranAcademy.getOneNotBusy(), upgrade);
		}

		// U-238 Shells
		upgrade = UpgradeTypes.U_238_Shells;
		if (!isPossibleSiegeResearch && marines >= 8 && isUpgradePossible(upgrade)) {
			tryToUpgrade(TerranAcademy.getOneNotBusy(), upgrade);
		}

		// // Stim Packs
		// technology = STIMPACKS;
		// if (!isPossibleSiegeResearch && marines >= 9 &&
		// isResearchPossible(technology)) {
		// tryToResearch(TerranAcademy.getOneNotBusy(), technology);
		// }

		// ======================================================
		// LOWER PRIORITY
		// To research technologies below we must have second base built.
		if (TerranCommandCenter.getNumberOfUnits() <= 1 || isPossibleSiegeResearch
				|| isPossibleSpiderResearch) {
			return;
		}

		// Ion Thrusters (Vulture extra speed)
		upgrade = ION_THRUSTERS;
		if (isUpgradePossible(upgrade) && vultures >= 6 && xvr.canAfford(200, 200)) {
			tryToUpgrade(TerranMachineShop.getOneNotBusy(), upgrade);
		}

		// Vehicle Armour
		upgrade = VEHICLE_PLATING;
		if (vultures > 4 && TerranArmory.getNumberOfUnitsCompleted() > 0
				&& isUpgradePossible(upgrade) && xvr.canAfford(250, 150)) {
			tryToUpgrade(TerranArmory.getOneNotBusy(), upgrade);
		}

		// Vehicle Weapons
		// upgrade = UpgradeTypes.Terran_Vehicle_Weapons;
		// if (TerranArmory.getNumberOfUnitsCompleted() > 0 &&
		// isUpgradePossible(upgrade)) {
		// tryToUpgrade(TerranArmory.getOneNotBusy(), upgrade);
		// }

		// // Cloaking field
		// technology = CLOAKING_FIELD;
		// if (isResearchPossible(technology)) {
		// tryToResearch(TerranControlTower.getOneNotBusy(), technology);
		// }
	}

	// =========================================================

	@SuppressWarnings("unused")
	private static int getTechLevelOf(UpgradeTypes technology) {
		return xvr.SELF.upgradeLevel(technology.getID());
	}

	private static boolean isUpgradePossible(UpgradeTypes upgrade) {
		return isNotUpgraded(upgrade) && canUpgrade(upgrade);
	}

	private static boolean isNotUpgraded(UpgradeTypes tech) {
		return !xvr.SELF.hasResearched(tech.ordinal());
	}

	private static boolean canUpgrade(UpgradeTypes tech) {
		return xvr.getBwapi().canUpgrade(tech.ordinal());
	}

	private static boolean isResearchPossible(TechTypes technology) {
		return isNotResearched(technology) && canResearch(technology);
	}

	private static boolean isNotResearched(TechTypes tech) {
		return !xvr.SELF.hasResearched(tech.ordinal());
	}

	private static boolean canResearch(TechTypes tech) {
		return xvr.getBwapi().canResearch(tech.ordinal());
	}

	private static void tryToUpgrade(Unit building, UpgradeTypes upgrade) {
		if (building != null) {
			// Debug.message(xvr, "Researching " + upgrade.toString());
			xvr.getBwapi().upgrade(building.getID(), upgrade.ordinal());
			// if (!building.isBuildingNotBusy()) {
			// knownTechs.put(upgrade, true);
			// }
		}
	}

	private static void tryToResearch(Unit building, TechTypes technology) {
		if (building != null) {
			// Debug.message(xvr, "Researching " + technology.toString());
			xvr.getBwapi().research(building.getID(), technology.ordinal());
			// if (!building.isBuildingNotBusy()) {
			// knownTechs.put(upgrade, true);
			// }
		}
	}

	public static boolean isResearched(TechTypes tech) {
		return !isNotResearched(tech);
	}

	public static boolean isSiegeModeResearched() {
		return isResearched(TANK_SIEGE_MODE);
	}

	public static boolean isSiegeModeResearchPossible() {
		return isResearchPossible(TANK_SIEGE_MODE);
	}

	public static boolean isSpiderMinesResearched() {
		return isResearched(SPIDER_MINES);
	}

	public static boolean isStimpacksResearched() {
		return isResearched(STIMPACKS);
	}

	public static boolean isWraithCloakingFieldResearched() {
		return isResearched(SPIDER_MINES);
	}

}

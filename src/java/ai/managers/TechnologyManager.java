package ai.managers;

import jnibwapi.model.Unit;
import jnibwapi.types.TechType.TechTypes;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType.UpgradeTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.units.UnitManager;
import ai.terran.TerranAcademy;
import ai.terran.TerranArmory;
import ai.terran.TerranControlTower;
import ai.terran.TerranMachineShop;
import ai.terran.TerranSiegeTank;

public class TechnologyManager {

	public static final TechTypes TANK_SIEGE_MODE = TechTypes.Tank_Siege_Mode;
	public static final TechTypes SPIDER_MINES = TechTypes.Spider_Mines;
	public static final TechTypes CLOAKING_FIELD = TechTypes.Cloaking_Field;

	private static XVR xvr = XVR.getInstance();

	// private static HashMap<UpgradeTypes, Boolean> knownTechs = new
	// HashMap<UpgradeTypes, Boolean>();

	public static void act() {
		UpgradeTypes upgrade;
		TechTypes technology;

		int marines = UnitCounter.getNumberOfUnits(UnitTypes.Terran_Marine);
		int vultures = UnitCounter.getNumberOfUnits(UnitTypes.Terran_Vulture);
		int tanks = TerranSiegeTank.getNumberOfUnits();
		// int infantry = UnitCounter.getNumberOfInfantryUnits();

		// ======================================================
		// TOP PRIORITY
		// Technologies that are crucial and we don't need to have second base
		// in order to upgrade them

		// Tank Siege Mode
		technology = TANK_SIEGE_MODE;
		if (isResearchPossible(technology)) {
			tryToResearch(TerranMachineShop.getOneNotBusy(), technology);
		}

		// Spider Mines
		technology = SPIDER_MINES;
		if (vultures >= 1 && isResearchPossible(technology) && !isResearchPossible(TANK_SIEGE_MODE)) {
			tryToResearch(TerranMachineShop.getOneNotBusy(), technology);
		}

		// U-238 Shells
		upgrade = UpgradeTypes.U_238_Shells;
		if (marines >= 8 && isUpgradePossible(upgrade)) {
			tryToUpgrade(TerranAcademy.getOneNotBusy(), upgrade);
		}

		// ======================================================
		// LOWER PRIORITY
		// To research technologies below we must have second base built.
		if (UnitCounter.getNumberOfUnits(UnitManager.BASE) <= 1) {
			return;
		}

		// Ion Thrusters
		upgrade = UpgradeTypes.Ion_Thrusters;
		if (vultures >= 4 && isUpgradePossible(upgrade)) {
			tryToUpgrade(TerranMachineShop.getOneNotBusy(), upgrade);
		}

		// Vehicle Weapons
		upgrade = UpgradeTypes.Terran_Vehicle_Weapons;
		if (TerranArmory.getNumberOfUnitsCompleted() > 0 && tanks >= 5
				&& isUpgradePossible(upgrade)) {
			tryToUpgrade(TerranArmory.getOneNotBusy(), upgrade);
		}

		// Cloaking field
		technology = CLOAKING_FIELD;
		if (isResearchPossible(technology)) {
			tryToResearch(TerranControlTower.getOneNotBusy(), technology);
		}
	}

	@SuppressWarnings("unused")
	private static int getTechLevelOf(UpgradeTypes technology) {
		return XVR.SELF.upgradeLevel(technology.getID());
	}

	private static boolean isUpgradePossible(UpgradeTypes upgrade) {
		return isNotUpgraded(upgrade) && canUpgrade(upgrade);
	}

	private static boolean isNotUpgraded(UpgradeTypes tech) {
		return !XVR.SELF.hasResearched(tech.ordinal());
	}

	private static boolean canUpgrade(UpgradeTypes tech) {
		return xvr.getBwapi().canUpgrade(tech.ordinal());
	}

	private static boolean isResearchPossible(TechTypes technology) {
		return isNotResearched(technology) && canResearch(technology);
	}

	private static boolean isNotResearched(TechTypes tech) {
		return !XVR.SELF.hasResearched(tech.ordinal());
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

	public static boolean isWraithCloakingFieldResearched() {
		return isResearched(SPIDER_MINES);
	}

}

package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.enemy.TerranOffensiveBunker;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.economy.TechnologyManager;
import ai.managers.units.army.ArmyCreationManager;

public class TerranFactory {

	private static UnitTypes TANK_TANK_MODE = UnitTypes.Terran_Siege_Tank_Tank_Mode;
	public static UnitTypes VULTURE = UnitTypes.Terran_Vulture;
	public static UnitTypes GOLIATH = UnitTypes.Terran_Goliath;

	public static boolean FORCE_FACTORY_BEFORE_SECOND_BASE = false;

	private static final int MINIMUM_VULTURES = 3;
	public static final int MINIMUM_TANKS = 2;
	private static final int MINIMUM_GOLIATHS_EARLY = 2;
	private static final int MINIMUM_GOLIATHS_LATER = 6;

	private static final int tanksPercentage = 30;
	private static final int vulturesPercentage = 40;
	private static final int goliathsPercentage = 30;

	private static final UnitTypes buildingType = UnitTypes.Terran_Factory;
	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static void act(Unit facility) {
		if (facility == null) {
			return;
		}

		int[] buildingQueueDetails = Constructing.shouldBuildAnyBuilding();
		int freeMinerals = xvr.getMinerals();
		int freeGas = xvr.getGas();

		if (facility.getAddOnID() == -1 && TerranMachineShop.shouldBuild()) {
			return;
		}

		if (buildingQueueDetails != null) {
			freeMinerals -= buildingQueueDetails[0];
			freeGas -= buildingQueueDetails[1];
		}

		if (TerranControlTower.getNumberOfUnits() >= 1 && UnitCounter.getNumberOfShipUnits() <= 1) {
			freeGas -= 150;
		}

		if (buildingQueueDetails == null || (freeMinerals >= 125 && freeGas >= 25)) {
			if (facility.getTrainingQueueSize() == 0) {
				xvr.buildUnit(facility, defineUnitToBuild(freeMinerals, freeGas));
			}
		}
	}

	public static boolean shouldBuild() {
		int factories = UnitCounter.getNumberOfUnits(buildingType);
		int battleUnits = UnitCounter.getNumberOfBattleUnits();

		if (!xvr.canAfford(0, 10)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		// =========================================================

		// STRATEGY: Offensive Bunker
		if (TerranOffensiveBunker.isStrategyActive()) {
			if (TerranBarracks.getNumberOfUnitsCompleted() > 0) {
				if (factories == 0 && xvr.canAfford(200, 100)) {
					return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				}

				if (xvr.canAfford(450, 270)) {
					return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				}

				int tanks = TerranSiegeTank.getNumberOfUnits();
				if (factories == 0
						|| (factories < tanks + 1 || xvr.canAfford(240, 170 + factories * 20))) {
					return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				}
			}

			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		// =========================================================

		if (factories <= 2
				&& (xvr.canAfford(250, 200) || TerranCommandCenter.getNumberOfUnits() > 1)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (battleUnits <= 4 && battleUnits < ArmyCreationManager.MINIMUM_MARINES) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		if (TerranBunker.GLOBAL_MAX_BUNKERS >= 2 && xvr.getTimeSeconds() < 500) {
			if (!xvr.canAfford(250) && TerranBunker.getNumberOfUnits() < TerranBunker.MAX_STACK) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			}
		}

		if (factories >= 1 && !xvr.canAfford(550) && xvr.getTimeSeconds() < 800
				&& TerranCommandCenter.shouldBuild()) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		if (UnitCounter.getNumberOfUnits(TerranBarracks.getBuildingType()) >= 2) {
			boolean weAreConstructing = Constructing.weAreBuilding(buildingType);

			if (factories == 0 && !weAreConstructing) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			}

			int machineShops = TerranMachineShop.getNumberOfUnits();
			if (factories == 1 && machineShops >= 1 && !weAreConstructing
					&& (battleUnits >= 18 || xvr.canAfford(280, 200))) {
				return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			}
		}

		// If all buildings are busy, build new one.
		if (factories >= 2 && xvr.canAfford(300, 100) && areAllBuildingsBusy()) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	// =========================================================

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			Constructing.construct(xvr, buildingType);
		}
	}

	private static boolean areAllBuildingsBusy() {
		return getOneNotBusy() == null;
	}

	public static Unit getOneNotBusy() {
		for (Unit unit : xvr.getUnitsOfType(buildingType)) {
			if (unit.isCompleted() && unit.isBuildingNotBusy()) {
				return unit;
			}
		}
		return null;
	}

	public static UnitTypes getBuildingType() {
		return buildingType;
	}

	public static ArrayList<Unit> getAllObjects() {
		return xvr.getUnitsOfTypeCompleted(buildingType);
	}

	// ==========================
	// Unit creating

	private static UnitTypes defineUnitToBuild(int freeMinerals, int freeGas) {
		boolean tanksAllowed = (freeMinerals >= 150 && freeGas >= 100)
				&& UnitCounter.weHaveBuildingFinished(TerranMachineShop.getBuildingType());
		boolean goliathsAllowed = (freeMinerals >= 125 && freeGas >= 50)
				&& UnitCounter.weHaveBuildingFinished(TerranArmory.getBuildingType());

		// ==================

		int vultures = UnitCounter.getNumberOfUnits(VULTURE);
		int tanks = TerranSiegeTank.getNumberOfUnits();
		int goliaths = UnitCounter.getNumberOfUnits(GOLIATH);

		boolean forceTanks = XVR.isEnemyTerran();
		boolean notEnoughVultures = vultures < MINIMUM_VULTURES;
		boolean notEnoughTanks = vultures < MINIMUM_TANKS;
		boolean notEnoughGoliaths = xvr.getTimeSeconds() < 800 ? goliaths < MINIMUM_GOLIATHS_EARLY
				: goliaths < MINIMUM_GOLIATHS_LATER;

		// ==================
		// If very little units, below critical limit

		// Force researching Siege Mode if we have at least one tank
		if (TerranOffensiveBunker.isStrategyActive()) {
			if (xvr.canAfford(150, 100)) {
				return TANK_TANK_MODE;
			}

			if (tanks >= 1 && !TechnologyManager.isSiegeModeResearched()
					&& !xvr.canAfford(300, 250)) {
				return null;
			}
		}

		// TANK
		if (tanksAllowed && (notEnoughTanks || forceTanks)) {
			return TANK_TANK_MODE;
		}

		// GOLIATH
		if (freeGas >= 50 && goliathsAllowed && notEnoughGoliaths) {
			return GOLIATH;
		}

		// VULTURE
		if (notEnoughVultures && (!forceTanks || (tanks >= 5 && !xvr.canAfford(0, 100)))) {
			return VULTURE;
		}

		if (TerranOffensiveBunker.isStrategyActive()) {
			if (xvr.canAfford(250)) {
				return VULTURE;
			} else {
				return null;
			}
		}

		// =================
		// Standard production, based on units percentage in our army

		int totalRatio = vulturesPercentage + (tanksAllowed ? tanksPercentage : 0)
				+ (goliathsAllowed ? goliathsPercentage : 0);
		int totalVehicles = UnitCounter.getNumberOfVehicleUnits();

		// TANK
		if (freeGas >= 150 && notEnoughPercentOf(tanks, totalVehicles, tanksPercentage, totalRatio)) {
			return TANK_TANK_MODE;
		}

		// GOLIATH
		if (freeGas >= 50
				&& notEnoughPercentOf(goliathsPercentage, totalVehicles, goliathsPercentage,
						totalRatio)) {
			return GOLIATH;
		}

		return VULTURE;

		// // TANK
		// if (tanksAllowed) {
		// if (tanks >= 2) {
		// if (notEnoughGoliaths) {
		// return GOLIATH;
		// } else if (notEnoughVultures) {
		// return VULTURE;
		// }
		// } else {
		// return TANK_TANK_MODE;
		// }
		// }
		//
		// // GOLIATH
		// if (goliathsAllowed) {
		// if (notEnoughGoliaths) {
		// return GOLIATH;
		// }
		// }
		//
		// // VULTURE
		// if (notEnoughVultures || !tanksAllowed) {
		// return VULTURE;
		// }
		//
		// if (xvr.canAfford(800)) {
		// return VULTURE;
		// }
		// return null;
	}

	private static boolean notEnoughPercentOf(int vehiclesOfThisType, int totalVehicles,
			int minPercentInArmy, int totalOfPercentage) {
		double percentOfVehicles = (double) vehiclesOfThisType / totalVehicles;
		double minPercentOfVehicles = (double) minPercentInArmy / totalOfPercentage;
		if (percentOfVehicles < minPercentOfVehicles) {
			return true;
		} else {
			return false;
		}
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(buildingType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(buildingType);
	}

}

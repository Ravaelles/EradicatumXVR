package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.economy.TechnologyManager;

public class TerranFactory {

	private static UnitTypes TANK_TANK_MODE = UnitTypes.Terran_Siege_Tank_Tank_Mode;
	public static UnitTypes VULTURE = UnitTypes.Terran_Vulture;
	public static UnitTypes GOLIATH = UnitTypes.Terran_Goliath;

	public static boolean FORCE_GOLIATHS_INSTEAD_VULTURES = false;
	public static boolean FORCE_FACTORY_BEFORE_SECOND_BASE = false;

	private static final int MAX_FACTORIES = 5;

	public static final int MINIMUM_TANKS = 2;
	private static final int MINIMUM_GOLIATHS_EARLY = 2;
	private static final int MINIMUM_GOLIATHS_LATER = 6;

	private static final int tanksPercentage = 20;
	private static final int vulturesPercentage = 55;
	private static final int goliathsPercentage = 30;

	private static final UnitTypes buildingType = UnitTypes.Terran_Factory;
	private static XVR xvr = XVR.getInstance();
	public static boolean ONLY_TANKS = false;

	// =========================================================

	public static void act(Unit facility) {
		if (facility == null) {
			return;
		}

		int vultures = UnitCounter.getNumberOfUnits(VULTURE);
		boolean isCriticallyFewVultures = vultures < TerranVulture.CRITICALLY_FEW_VULTURES;

		int[] buildingQueueDetails = Constructing.shouldBuildAnyBuilding();
		int freeMinerals = xvr.getMinerals();
		int freeGas = xvr.getGas();

		if (!facility.hasAddOn() && TerranMachineShop.shouldBuild() && !TerranFactory.ONLY_TANKS) {
			return;
		}

		// if (!isCriticallyFewVultures && !TerranFactory.ONLY_TANKS) {
		// if (buildingQueueDetails != null) {
		// freeMinerals -= buildingQueueDetails[0];
		// freeGas -= buildingQueueDetails[1];
		// }
		//
		// // if (TerranControlTower.getNumberOfUnits() >= 1
		// // && UnitCounter.getNumberOfShipUnits() <= 1) {
		// // freeGas -= 150;
		// // }
		// }

		// boolean isEnoughFreeResources = (freeMinerals >= 125 && freeGas >=
		// 25);
		boolean isEnoughFreeResources = freeMinerals >= 75;
		boolean isLotOfResourcesFree = freeMinerals >= 500;
		if (buildingQueueDetails == null || isEnoughFreeResources || isCriticallyFewVultures || isLotOfResourcesFree) {
			if (facility.getTrainingQueueSize() == 0 || facility.getRemainingTrainTime() <= 5) {
				xvr.buildUnit(facility, defineUnitToBuild(freeMinerals, freeGas));
			}
		}
	}

	public static boolean shouldBuild() {
		int factories = UnitCounter.getNumberOfUnits(buildingType);
		int battleUnits = UnitCounter.getNumberOfBattleUnits();

		if (!xvr.canAfford(200, 100)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		if (!xvr.canAfford(75) || battleUnits < 4) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		// =========================================================

		if (TerranFactory.ONLY_TANKS && factories >= 1 && getOneNotBusy() == null && !xvr.canAfford(340, 190)
				&& TerranSiegeTank.getNumberOfUnits() < 1) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		}

		// =========================================================
		// Begin EASY-WAY

		if (factories < 2 && (battleUnits >= 3 || xvr.canAfford(300, 100)) && xvr.canAfford(0, 1)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (factories < MAX_FACTORIES && xvr.canAfford(200, 100)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		// if (factories >= 3 && TerranCommandCenter.getNumberOfUnits() <= 1 ) {
		// return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		// }

		// End EASY-WAY
		// =========================================================

		if (factories <= 2 && (xvr.canAfford(250) || TerranCommandCenter.getNumberOfUnits() > 1)) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		if (UnitCounter.getNumberOfVehicleUnits() <= 4) {
			return ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		}

		// if (battleUnits <= 4 && battleUnits <
		// ArmyCreationManager.MINIMUM_MARINES) {
		// return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		// }

		// if (factories >= 1 && !xvr.canAfford(550) && xvr.getTimeSeconds() <
		// 800
		// && TerranCommandCenter.shouldBuild()) {
		// return ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		// }

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
			Constructing.construct(buildingType);
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
		int tanks = TerranSiegeTank.getNumberOfUnits();
		int goliaths = UnitCounter.getNumberOfUnits(GOLIATH);

		// =========================================================
		// Special mode, mostly tanks

		if (ONLY_TANKS) {
			if (tanks < 7 || tanks < goliaths) {
				return TANK_TANK_MODE;
			} else {
				return GOLIATH;
			}
		}

		// =========================================================

		boolean tanksAllowed = (freeMinerals >= 125 && freeGas >= 50)
				&& UnitCounter.weHaveBuildingFinished(TerranMachineShop.getBuildingType())
				&& TechnologyManager.isSiegeModeResearched();
		boolean goliathsAllowed = (freeMinerals >= 125 && freeGas >= 50)
				&& UnitCounter.weHaveBuildingFinished(TerranArmory.getBuildingType());

		// ==================

		int vultures = UnitCounter.getNumberOfUnits(VULTURE);
		int vulturesCompleted = UnitCounter.getNumberOfUnitsCompleted(VULTURE);

		boolean notEnoughVultures = vultures < TerranVulture.CRITICALLY_FEW_VULTURES
				|| (vulturesCompleted + 1 <= tanks);
		boolean notEnoughTanks = vultures < MINIMUM_TANKS;
		boolean notEnoughGoliaths = false;

		if (xvr.getTimeSeconds() < 750) {
			notEnoughGoliaths = goliaths < MINIMUM_GOLIATHS_EARLY;
		} else {
			notEnoughGoliaths = goliaths < MINIMUM_GOLIATHS_LATER || (goliaths - 1) < vultures;
		}

		// ==================
		// If very little units, below critical limit

		// VULTURE
		if (!TerranVulture.DISABLE_VULTURES && notEnoughVultures) {
			return VULTURE;
		}

		// TANK
		if (freeGas >= 150 && tanksAllowed && notEnoughTanks) {
			return TANK_TANK_MODE;
		}

		// GOLIATH
		boolean preferGoliathsOverVultures = xvr.getTimeSeconds() > 700 && goliaths < vultures;
		boolean goliathsIfFewOfTanks = (freeGas >= 200 && tanks < 6);
		boolean goliathsIfLotOfTanks = (freeGas >= 50 && tanks >= 6);
		if (FORCE_GOLIATHS_INSTEAD_VULTURES
				|| ((goliathsIfFewOfTanks || goliathsIfLotOfTanks) && goliathsAllowed && (notEnoughGoliaths || preferGoliathsOverVultures))) {
			if (UnitCounter.getNumberOfUnits(TerranArmory.getBuildingType()) == 0) {
				TerranArmory.buildIfNecessary();
			}
			return GOLIATH;
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
		if (freeGas >= 50 && notEnoughPercentOf(goliathsPercentage, totalVehicles, goliathsPercentage, totalRatio)) {
			return GOLIATH;
		}

		if (!TerranVulture.DISABLE_VULTURES) {
			return VULTURE;
		} else {
			return null;
		}

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

	private static boolean notEnoughPercentOf(int vehiclesOfThisType, int totalVehicles, int minPercentInArmy,
			int totalOfPercentage) {
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

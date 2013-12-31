package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;

public class TerranFactory {

	private static UnitTypes TANK_TANK_MODE = UnitTypes.Terran_Siege_Tank_Tank_Mode;
	public static UnitTypes VULTURE = UnitTypes.Terran_Vulture;
	public static UnitTypes GOLIATH = UnitTypes.Terran_Goliath;

	private static final int MINIMUM_VULTURES = 3;
	private static final int MINIMUM_TANKS = 2;
	private static final int MINIMUM_GOLIATHS_EARLY = 2;
	private static final int MINIMUM_GOLIATHS_LATER = 6;

	private static final int tanksPercentage = 30;
	private static final int vulturesPercentage = 40;
	private static final int goliathsPercentage = 30;

	private static final UnitTypes buildingType = UnitTypes.Terran_Factory;
	private static XVR xvr = XVR.getInstance();

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			Constructing.construct(xvr, buildingType);
		}
	}

	public static boolean shouldBuild() {
		int factories = UnitCounter.getNumberOfUnits(buildingType);

		if (UnitCounter.getNumberOfUnits(TerranBarracks.getBuildingType()) >= 2) {
			boolean buildNext = factories == 0
					|| (factories == 1 && !Constructing.weAreBuilding(buildingType));
			if (buildNext
					&& (UnitCounter.getNumberOfBattleUnits() >= (4 * factories) || factories == 0)) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		// If all buildings are busy, build new one.
		if (factories >= 2 && xvr.canAfford(300, 100) && areAllBuildingsBusy()) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			return true;
		}

		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		return false;
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

		if (buildingQueueDetails == null || (freeMinerals >= 125 && freeGas >= 25)) {
			if (facility.getTrainingQueueSize() == 0) {
				xvr.buildUnit(facility, defineUnitToBuild(freeMinerals, freeGas));
			}
		}
	}

	private static UnitTypes defineUnitToBuild(int freeMinerals, int freeGas) {
		boolean tanksAllowed = (freeMinerals >= 125 && freeGas >= 50)
				&& UnitCounter.weHaveBuildingFinished(TerranMachineShop.getBuildingType());
		boolean goliathsAllowed = (freeMinerals >= 125 && freeGas >= 50)
				&& UnitCounter.weHaveBuildingFinished(TerranArmory.getBuildingType());

		// ==================

		int vultures = UnitCounter.getNumberOfUnits(VULTURE);
		int tanks = TerranSiegeTank.getNumberOfUnits();
		int goliaths = UnitCounter.getNumberOfUnits(GOLIATH);

		boolean notEnoughVultures = vultures < MINIMUM_VULTURES;
		boolean notEnoughTanks = vultures < MINIMUM_TANKS;
		boolean notEnoughGoliaths = xvr.getTimeSeconds() < 800 ? goliaths < MINIMUM_GOLIATHS_EARLY
				: goliaths < MINIMUM_GOLIATHS_LATER;

		// ==================
		// If very little units, below critical limit

		// TANK
		if (tanksAllowed && notEnoughTanks) {
			return TANK_TANK_MODE;
		}

		// GOLIATH
		if (goliathsAllowed && notEnoughGoliaths) {
			return TANK_TANK_MODE;
		}

		// VULTURE
		if (notEnoughVultures) {
			return VULTURE;
		}

		// =================
		// Standard production, based on units percentage in our army

		int totalRatio = vulturesPercentage + (tanksAllowed ? tanksPercentage : 0)
				+ (goliathsAllowed ? goliathsPercentage : 0);
		int totalVehicles = UnitCounter.getNumberOfVehicleUnits();

		// TANK
		if (notEnoughPercentOf(tanks, totalVehicles, tanksPercentage, totalRatio)) {
			return TANK_TANK_MODE;
		}

		// GOLIATH
		if (notEnoughPercentOf(goliathsPercentage, totalVehicles, goliathsPercentage, totalRatio)) {
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

package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.units.UnitManager;

public class TerranBarracks {

	public static UnitTypes MARINE = UnitTypes.Terran_Marine;
	public static UnitTypes FIREBAT = UnitTypes.Terran_Firebat;
	public static UnitTypes MEDIC = UnitTypes.Terran_Medic;
	public static UnitTypes GHOST = UnitTypes.Terran_Ghost;

	private static final UnitTypes buildingType = UnitTypes.Terran_Barracks;
	private static XVR xvr = XVR.getInstance();

	private static boolean isPlanAntiAirActive = false;

	public static int MIN_UNITS_FOR_DIFF_BUILDING = 20;
	public static int MIN_MEDICS = 3;

	public static boolean LIMIT_MARINES = false;

	private static int marinesBuildRatio = 65;
	private static int firebatBuildRatio = 0;
	private static int medicBuildRatio = 19;

	// private static int highTemplarBuildRatio = 19;

	// private static final int MINIMAL_HIGH_TEMPLARS = 2;
	// private static final int MAX_ZEALOTS = 5;

	public static boolean shouldBuild() {
		int barracks = UnitCounter.getNumberOfUnits(buildingType);
		int bases = UnitCounter.getNumberOfUnitsCompleted(UnitManager.BASE);

		if (TerranCommandCenter.shouldBuild() && barracks >= (2 * bases)) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			return false;
		}

		if (TerranSupplyDepot.getNumberOfUnits() > 0 && barracks < 2) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			return true;
		}

		// // ### Version ### Expansion with cannons
		// if (BotStrategyManager.isExpandWithBunkers()) {
		// int bunkers =
		// UnitCounter.getNumberOfUnitsCompleted(TerranBunker.getBuildingType());
		// if ((bunkers >= TerranBunker.MAX_STACK || xvr.canAfford(300)) &&
		// barracks <= 2
		// && xvr.canAfford(150)) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		// }

		// // ### Version ### Expansion with gateways
		// if (BotStrategyManager.isExpandWithBarracks()) {
		// if (barracks <= 2 && (isMajorityOfBarracksTrainingUnits()) &&
		// xvr.canAfford(134)) {
		// if (barracks < 2) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		// } else {
		// if (!UnitCounter.weHaveBuilding(TerranAcademy.getBuildingType())) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		// return false;
		// }
		// }
		// }

		if (bases <= 1) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			return false;
		}

		// if (barracks >= 3 && xvr.canAfford(140)) {
		// if (isMajorityOfBarracksTrainingUnits()) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		// }
		//
		// // 3 barracks or more
		// if (barracks >= 3 && (barracks <= 5 || xvr.canAfford(520))) {
		// if (isMajorityOfBarracksTrainingUnits()) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		// }
		// if (barracks >= 2 && bases >= 2
		// && UnitCounter.weHaveBuilding(UnitTypes.Protoss_Observatory)
		// && UnitCounter.weHaveBuilding(UnitTypes.Protoss_Citadel_of_Adun)) {
		// int HQs = UnitCounter.getNumberOfUnits(UnitManager.BASE);
		// if ((double) barracks / HQs <= 2 && xvr.canAfford(560)) {
		// if (isMajorityOfBarracksTrainingUnits()) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		// }
		// }
		//
		// if (xvr.canAfford(1500)) {
		// if (isMajorityOfBarracksTrainingUnits()) {
		// ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
		// return true;
		// }
		// }

		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		return false;
	}

	// private static boolean isMajorityOfBarracksTrainingUnits() {
	// ArrayList<Unit> allObjects = xvr.getUnitsOfType(buildingType);
	// int all = allObjects.size();
	// int busy = 0;
	// for (Unit gateway : allObjects) {
	// if (gateway.isTraining()) {
	// busy++;
	// }
	// }
	//
	// double threshold = (Math.min(0.8, 0.7 + all * 0.05));
	// return all <= 2 || ((double) busy / all) >= threshold;
	// }

	public static ArrayList<Unit> getAllObjects() {
		return xvr.getUnitsOfTypeCompleted(buildingType);
	}

	public static void enemyIsProtoss() {
	}

	public static void enemyIsTerran() {
		medicBuildRatio /= 7;
	}

	public static void enemyIsZerg() {
	}

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			Constructing.construct(xvr, buildingType);
		}
		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
	}

	public static void act(Unit barracks) {
		int[] buildingQueueDetails = Constructing.shouldBuildAnyBuilding();
		int freeMinerals = xvr.getMinerals();
		int freeGas = xvr.getGas();
		if (buildingQueueDetails != null) {
			freeMinerals -= buildingQueueDetails[0];
			freeGas -= buildingQueueDetails[1];
		}

		boolean shouldAlwaysBuild = xvr.canAfford(100)
				&& UnitCounter.getNumberOfBattleUnits() <= MIN_UNITS_FOR_DIFF_BUILDING;
		if (shouldAlwaysBuild || buildingQueueDetails == null || freeMinerals >= 100) {
			if (barracks.getTrainingQueueSize() == 0) {
				xvr.buildUnit(barracks, defineUnitToBuild(freeMinerals, freeGas));
			}
		}
	}

	private static UnitTypes defineUnitToBuild(int freeMinerals, int freeGas) {
		int marines = UnitCounter.getNumberOfUnits(MARINE);

		// If we don't have Observatory build than disallow production of units
		// which cost lot of gas.
		int forceFreeGas = 0;
		int firebats = UnitCounter.getNumberOfUnits(FIREBAT);

		boolean weHaveAcademy = UnitCounter.weHaveBuilding(TerranAcademy.getBuildingType());
		boolean firebatAllowed = weHaveAcademy
				&& (freeMinerals >= 50 && (freeGas - forceFreeGas) >= 25);
		boolean medicAllowed = weHaveAcademy
				&& (freeMinerals >= 50 && (freeGas - forceFreeGas) >= 25);
		boolean ghostAllowed = (UnitCounter.weHaveBuildingFinished(UnitTypes.Terran_Covert_Ops))
				&& (freeMinerals >= 50 && (freeGas - forceFreeGas) >= 25);

		// Calculate unit ratios
		double totalRatio = marinesBuildRatio + (firebatAllowed ? firebatBuildRatio : 0)
				+ (medicAllowed ? medicBuildRatio : 0);
		double totalInfantry = UnitCounter.getNumberOfInfantryUnits() + 1;

		// ===========================================================
		UnitTypes typeToBuild = MARINE;

		// FIREBATS
		// Don't build firebats at all against Terran
		if (!XVR.isEnemyTerran()) {
			if (firebatAllowed) {
				int minFirebats = XVR.isEnemyProtoss() ? 3 : 1;
				if (firebats < minFirebats && xvr.canAfford(50, 25)) {
					return FIREBAT;
				}

				double firebatPercent = (double) firebats / totalInfantry;
				if (firebatPercent < firebatBuildRatio / totalRatio) {
					return FIREBAT;
				}
			}
		}

		// MEDICS
		if (weHaveAcademy) {
			int medics = UnitCounter.getNumberOfUnits(MEDIC);
			int ghosts = UnitCounter.getNumberOfUnits(GHOST);

			if (medics <= 1) {
				return MEDIC;
			}

			// Build some HIGH Templars if there'are none.
			if (ghostAllowed
					&& ((medics >= 5 || medicBuildRatio < 10) && ghosts < 2 || freeGas > 1000)) {
				return GHOST;
			}

			if (xvr.getTimeSeconds() >= 270) {
				double medicPercent = (double) medics / totalInfantry;
				if (medics < MIN_MEDICS || medicPercent < medicBuildRatio / totalRatio) {
					return MEDIC;
				}
			} else {
				int marinesMinusBunkers = marines - TerranBunker.MAX_STACK * 3;
				if (medics < marinesMinusBunkers / 3) {
					return MEDIC;
				}
			}
		}

		// MARINES
		// if (BotStrategyManager.isExpandWithBunkers()) {
		// if (marines >= 3 && !xvr.canAfford(1000)) {
		// return null;
		// }
		// }
		// if (marines >= 8 + firebats) {
		// return null;
		// }

		double marinePercent = marines / totalInfantry;
		if (marinePercent < marinesBuildRatio / totalRatio || LIMIT_MARINES) {
			return MARINE;
		}

		return typeToBuild;
	}

	public static UnitTypes getBuildingType() {
		return buildingType;
	}

	public static void changePlanToAntiAir() {
		if (isPlanAntiAirActive) {
			return;
		}

		isPlanAntiAirActive = true;
		marinesBuildRatio = 10;
		firebatBuildRatio = 70;
		medicBuildRatio = 10;
	}

	public static boolean isPlanAntiAirActive() {
		return isPlanAntiAirActive;
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(buildingType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(buildingType);
	}

}

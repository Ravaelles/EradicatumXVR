package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.TechnologyManager;
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

	public static int MIN_UNITS_FOR_DIFF_BUILDING = TerranBunker.MAX_STACK * 4;
	public static int MIN_MEDICS = 2;

	public static boolean LIMIT_MARINES = false;

	private static int marinesBuildRatio = 65;
	private static int firebatBuildRatio = 0;
	private static int medicBuildRatio = 19;

	// =========================================================

	public static boolean shouldBuild() {
		int barracks = UnitCounter.getNumberOfUnits(buildingType);
		int bases = UnitCounter.getNumberOfUnitsCompleted(UnitManager.BASE);

		boolean enoughBarracks = barracks >= 2;

		// =========================================================
		// ANTI-ZERGLING RUSH

		// If enemy is Zerg make sure you build one barracks, one bunker.
		// Normally it would be: 2 x Barracks, only then bunker.
		if (XVR.isEnemyZerg()) {
			if (barracks == 0 && !Constructing.weAreBuilding(buildingType)) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}

			if (barracks >= 1 && TerranBunker.getNumberOfUnits() == 0) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
				return false;
			}

			if (barracks == 1 && xvr.canAfford(200)) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
				return false;
			}
		}

		// =========================================================

		if (TerranCommandCenter.shouldBuild() && barracks >= (2 * bases)) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			return false;
		}

		if ((TerranSupplyDepot.getNumberOfUnits() > 0 || xvr.canAfford(142)) && !enoughBarracks) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			return true;
		}

		if (bases <= 1) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
			return false;
		}

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

		// If we have very few tanks, always leave cash for one.
		if (TerranMachineShop.getNumberOfUnitsCompleted() > 0
				&& TerranSiegeTank.getNumberOfUnits() < TerranFactory.MINIMUM_TANKS
				&& TerranFactory.getOneNotBusy() != null) {
			if (!xvr.canAfford(200) && UnitCounter.getNumberOfInfantryUnits() >= 8) {
				return;
			}
		}

		int[] buildingQueueDetails = Constructing.shouldBuildAnyBuilding();
		int freeMinerals = xvr.getMinerals();
		int freeGas = xvr.getGas();
		if (buildingQueueDetails != null) {
			freeMinerals -= buildingQueueDetails[0];
			freeGas -= buildingQueueDetails[1];
		}

		// =====================================================
		// Check if shouldn't spare resources for Siege Research
		boolean shouldResearchSiege = TerranMachineShop.getNumberOfUnitsCompleted() > 0
				&& TechnologyManager.isSiegeModeResearchPossible();
		if (shouldResearchSiege) {
			freeMinerals -= 150;
			freeGas -= 150;
		}

		boolean shouldSpareGasForOtherUnits = false;
		if (TerranControlTower.getNumberOfUnits() >= 1 && UnitCounter.getNumberOfShipUnits() <= 1) {
			freeGas -= 150;
		}

		// =====================================================
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
		int medics = UnitCounter.getNumberOfUnits(MEDIC);

		if (medics >= MIN_MEDICS && freeMinerals >= 700) {
			return MARINE;
		}

		// If we don't have Observatory build than disallow production of units
		// which cost lot of gas.
		int forceFreeGas = 0;

		int tanks = TerranSiegeTank.getNumberOfUnits();
		if (tanks > 0) {
			forceFreeGas += 105;
		}

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

		if (marines < 8) {
			return MARINE;
		}

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
				if (medics < marinesMinusBunkers / 4) {
					return MEDIC;
				}
			}
		}

		// MARINES
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

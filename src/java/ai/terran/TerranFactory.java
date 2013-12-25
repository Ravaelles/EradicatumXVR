package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;

public class TerranFactory {

	public static UnitTypes TANK = UnitTypes.Terran_Siege_Tank_Tank_Mode;
	public static UnitTypes VULTURE = UnitTypes.Terran_Vulture;
	public static UnitTypes GOLIATH = UnitTypes.Terran_Goliath;

	private static final int MINIMUM_VULTURES = 6;
	private static final int MINIMUM_GOLIATHS = 2;

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
					&& (UnitCounter.getNumberOfBattleUnits() >= (9 + 4 * factories) || factories == 0)) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		return false;
	}

	public static Unit getOneNotBusy() {
		for (Unit unit : xvr.getUnitsOfType(buildingType)) {
			if (unit.isBuildingNotBusy()) {
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
		// && (freeMinerals >= 125 && freeGas >= 50)
		boolean goliathsAllowed = (freeMinerals >= 125 && freeGas >= 50)
				&& UnitCounter.weHaveBuildingFinished(TerranArmory.getBuildingType());

		// ==================

		int vultures = UnitCounter.getNumberOfUnits(VULTURE);
		int tanks = UnitCounter.getNumberOfUnits(TANK);
		int goliaths = UnitCounter.getNumberOfUnits(GOLIATH);

		boolean notEnoughVultures = vultures < MINIMUM_VULTURES;
		boolean notEnoughGoliaths = goliaths < MINIMUM_GOLIATHS;

		// ==================

		// TANK
		if (tanksAllowed) {
			if (tanks >= 2 && notEnoughVultures) {
				return VULTURE;
			} else {
				return TANK;
			}
		}

		// VULTURE
		if (notEnoughVultures || !tanksAllowed) {
			return VULTURE;
		}

		// GOLIATH
		if (goliathsAllowed) {
			// int totalInfantry = UnitCounter.getNumberOfInfantryUnits() + 1;
			// int totalReavers = UnitCounter.getNumberOfUnits(REAVER);
			// if ((double) totalReavers / totalInfantry <=
			// REAVERS_TO_INFANTRY_RATIO
			// || totalReavers < MINIMUM_REAVERS) {
			// return REAVER;
			// }
			if (notEnoughGoliaths) {
				return GOLIATH;
			}
		}

		return null;
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(buildingType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(buildingType);
	}

}

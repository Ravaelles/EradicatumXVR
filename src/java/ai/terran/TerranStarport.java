package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.economy.TechnologyManager;

public class TerranStarport {

	public static UnitTypes BATTLECRUISER = UnitTypes.Terran_Battlecruiser;
	public static UnitTypes DROPSHIP = UnitTypes.Terran_Dropship;
	public static UnitTypes VALKYRIE = UnitTypes.Terran_Valkyrie;
	public static UnitTypes WRAITH = UnitTypes.Terran_Wraith;
	public static UnitTypes SCIENCE_VESSEL = UnitTypes.Terran_Science_Vessel;

	private static final int MIN_VESSELS = 1;
	// private static final int MINIMUM_BATTLECRUISERS = 2;
	// private static final int MINIMUM_WRAITHS = 2;
	// private static final int MINIMUM_VALKYRIES = 3;
	// private static final int VALKYRIES_PER_OTHER_AIR_UNIT = 2;

	private static final UnitTypes buildingType = UnitTypes.Terran_Starport;
	private static XVR xvr = XVR.getInstance();

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			Constructing.construct(xvr, buildingType);
		}
	}

	public static boolean shouldBuild() {
		int starports = UnitCounter.getNumberOfUnits(buildingType);

		if (TerranFactory.getNumberOfUnitsCompleted() >= 2 && starports == 0
				&& TerranSiegeTank.getNumberOfUnits() >= 5
				&& !TechnologyManager.isSiegeModeResearchPossible()) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			return true;
		}

		if (starports == 1 && xvr.canAfford(600, 400)) {
			ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
			return true;
		}

		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		return false;
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
		return xvr.getUnitsOfType(buildingType);
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
		if (buildingQueueDetails != null) {
			freeMinerals -= buildingQueueDetails[0];
			freeGas -= buildingQueueDetails[1];
		}

		if (buildingQueueDetails == null || (freeMinerals >= 200 && freeGas >= 400)) {
			if (facility.getTrainingQueueSize() == 0) {
				xvr.buildUnit(facility, defineUnitToBuild(freeMinerals, freeGas));
			}
		}
	}

	// =========================================================

	private static UnitTypes defineUnitToBuild(int freeMinerals, int freeGas) {
		// boolean arbiterAllowed =
		// UnitCounter.weHaveBuilding(UnitTypes.Protoss_Arbiter_Tribunal);
		boolean valkyrieAllowed = UnitCounter.weHaveBuilding(UnitTypes.Terran_Control_Tower);
		boolean scienceVesselAllowed = UnitCounter
				.weHaveBuilding(UnitTypes.Terran_Science_Facility);

		// // BATTLECRUISER
		// if (arbiterAllowed && xvr.countUnitsOfType(BATTLECRUISER) <
		// MINIMUM_BATTLECRUISERS) {
		// return BATTLECRUISER;
		// }

		// SCIENCE VESSEL
		if (scienceVesselAllowed && TerranScienceVessel.getNumberOfUnits() < MIN_VESSELS) {
			return SCIENCE_VESSEL;
		}

		// VALKYRIE
		if (valkyrieAllowed
				&& UnitCounter.getNumberOfUnitsCompleted(UnitTypes.Terran_Valkyrie) <= 3) {
			return VALKYRIE;
		}

		// WRAITH
		// if (UnitCounter.getNumberOfUnits(WRAITH) < MINIMUM_WRAITHS) {
		return WRAITH;
		// }

		// // VALKYRIE
		// if (UnitCounter.getNumberOfUnits(VALKYRIE) < MINIMUM_VALKYRIES
		// || (UnitCounter.countAirUnitsNonValkyrie() *
		// VALKYRIES_PER_OTHER_AIR_UNIT < UnitCounter
		// .getNumberOfUnits(VALKYRIE))) {
		// return VALKYRIE;
		// }

		// return null;
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(buildingType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(buildingType);
	}

}

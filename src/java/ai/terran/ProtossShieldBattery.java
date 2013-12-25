package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.units.UnitManager;

public class ProtossShieldBattery {

	private static final UnitTypes buildingType = UnitTypes.Protoss_Shield_Battery;
	private static XVR xvr = XVR.getInstance();

	// public static void act(Unit battery) {
	// if (battery.getEnergy() >= 10) {
	// }
	// }

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			Constructing.construct(xvr, buildingType);
		}
	}

	public static boolean shouldBuild() {

		// First battery
		if (!UnitCounter.weHaveBuilding(buildingType)
				&& !Constructing.weAreBuilding(buildingType)) {
			if (UnitCounter.getNumberOfBattleUnits() >= TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		// Second
		else if (UnitCounter.getNumberOfUnits(UnitManager.BASE) == 2
				&& UnitCounter.getNumberOfUnits(buildingType) == 1
				&& !Constructing.weAreBuilding(buildingType)) {
			if (xvr.canAfford(200)
					&& UnitCounter.getNumberOfBattleUnits() >= TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING * 2) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		// Third
		else if (UnitCounter.getNumberOfUnits(buildingType) == 2
				&& !Constructing.weAreBuilding(buildingType)) {
			if (xvr.canAfford(200)
					&& UnitCounter.getNumberOfBattleUnits() >= TerranBarracks.MIN_UNITS_FOR_DIFF_BUILDING * 3) {
				ShouldBuildCache.cacheShouldBuildInfo(buildingType, true);
				return true;
			}
		}

		ShouldBuildCache.cacheShouldBuildInfo(buildingType, false);
		return false;
	}

	public static Unit getOneWithEnergy() {
		for (Unit unit : xvr.getUnitsOfType(buildingType)) {
			if (unit.isCompleted() && unit.getEnergy() >= 20
					&& !unit.isUnpowered()) {
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

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(buildingType);
	}
	
	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(buildingType);
	}
	
}

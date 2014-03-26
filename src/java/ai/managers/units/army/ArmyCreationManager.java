package ai.managers.units.army;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.economy.TechnologyManager;
import ai.managers.units.UnitManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranFactory;
import ai.terran.TerranMachineShop;
import ai.terran.TerranStarport;

public class ArmyCreationManager {

	private static XVR xvr = XVR.getInstance();

	private static final int MINIMUM_UNITS = 15;
	private static final int MINIMUM_MARINES = 7;

	public static void act() {
		if (weShouldBuildBattleUnits()) {
			boolean shouldResearchSiege = TerranMachineShop.getNumberOfUnitsCompleted() > 0
					&& TechnologyManager.isSiegeModeResearchPossible() && !xvr.canAfford(300, 200);

			// =========================================================

			// STARPORTS
			ArrayList<Unit> starports = TerranStarport.getAllObjects();
			if (!starports.isEmpty()) {
				for (Unit stargate : starports) {
					TerranStarport.act(stargate);
				}
			}

			// FACTORIES
			ArrayList<Unit> factories = TerranFactory.getAllObjects();
			if (!factories.isEmpty() && !shouldResearchSiege) {
				for (Unit factory : factories) {
					TerranFactory.act(factory);
				}
			}

			int infantry = UnitCounter.getNumberOfInfantryUnits();
			boolean fewInfantry = infantry <= MINIMUM_MARINES;
			boolean weShouldBuildInfantryUnits = factories.isEmpty() || xvr.canAfford(100)
					|| fewInfantry;

			// If we have factories and have free spot, don't build infantry.
			if (weShouldBuildInfantryUnits && !factories.isEmpty() && !xvr.canAfford(200)) {
				// TerranFactory.getOneNotBusy() != null &&
				weShouldBuildInfantryUnits = false;
			}

			// BARRACKS
			if (weShouldBuildInfantryUnits) {
				boolean noFactories = TerranFactory.getNumberOfUnitsCompleted() == 0;
				boolean haveFreeFactorySpots = TerranFactory.getOneNotBusy() != null;
				boolean shouldBuildInfantry = fewInfantry || noFactories
						|| (!haveFreeFactorySpots && fewInfantry);
				if (shouldBuildInfantry) {
					ArrayList<Unit> barracks = TerranBarracks.getAllObjects();
					if (!barracks.isEmpty()) {
						for (Unit barrack : barracks) {
							TerranBarracks.act(barrack);
						}
					}
				}
			}
			// }
		}
	}

	public static boolean isCriticallyFewInfantry() {
		return UnitCounter.getNumberOfUnits(UnitTypes.Terran_Marine) <= MINIMUM_MARINES;
	}

	public static boolean weShouldBuildBattleUnits() {
		if (xvr.getTimeSeconds() >= 800 && TerranCommandCenter.getNumberOfUnits() <= 1
				&& !xvr.canAfford(560)) {
			return false;
		}

		int battleUnits = UnitCounter.getNumberOfBattleUnits();

		if (xvr.getTimeSeconds() >= 350 && TerranFactory.getNumberOfUnits() == 0
				&& battleUnits >= 10 && !xvr.canAfford(560)) {
			return false;
		}

		if (isCriticallyFewInfantry()) {
			return true;
		}

		int bases = UnitCounter.getNumberOfUnits(UnitManager.BASE);

		if (battleUnits <= MINIMUM_UNITS) {
			return true;
		}
		if (bases == 1
				&& (TerranCommandCenter.shouldBuild() || Constructing
						.weAreBuilding(UnitManager.BASE)) && !xvr.canAfford(550)) {
			return false;
		}

		// if (!xvr.canAfford(125)) {
		// return false;
		// }
		if (TerranBunker.shouldBuild() && !xvr.canAfford(200)) {
			return false;
		}

		return true;
	}

}

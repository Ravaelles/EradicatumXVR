package ai.managers;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.units.UnitManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranFactory;
import ai.terran.TerranStarport;

public class ArmyCreationManager {

	private static XVR xvr = XVR.getInstance();

	private static final int MINIMUM_INFANTRY = 15;

	public static void act() {
		if (weShouldBuildBattleUnits()) {

			// FACTORIES
			ArrayList<Unit> factories = TerranFactory.getAllObjects();
			if (!factories.isEmpty()) {
				for (Unit factory : factories) {
					TerranFactory.act(factory);
				}
			}

			// BARRACKS
			boolean noFactories = TerranFactory.getNumberOfUnitsCompleted() == 0;
			boolean fewInfantry = UnitCounter.getNumberOfInfantryUnits() <= MINIMUM_INFANTRY;
			boolean haveFreeFactorySpots = TerranFactory.getOneNotBusy() != null;
			boolean shouldBuildInfantry = noFactories || (!haveFreeFactorySpots && fewInfantry);
			if (shouldBuildInfantry) {
				ArrayList<Unit> barracks = TerranBarracks.getAllObjects();
				if (!barracks.isEmpty()) {
					for (Unit barrack : barracks) {
						TerranBarracks.act(barrack);
					}
				}
			}

			// STARPORTS
			ArrayList<Unit> starports = TerranStarport.getAllObjects();
			if (!starports.isEmpty()) {
				for (Unit stargate : starports) {
					TerranStarport.act(stargate);
				}
			}
		}
	}

	public static boolean weShouldBuildBattleUnits() {
		int battleUnits = UnitCounter.getNumberOfBattleUnits();
		int bases = UnitCounter.getNumberOfUnits(UnitManager.BASE);

		if (battleUnits <= MINIMUM_INFANTRY
				|| (battleUnits < StrategyManager.getMinBattleUnits() + 2)) {
			return true;
		}
		if (bases == 1
				&& (TerranCommandCenter.shouldBuild() || Constructing
						.weAreBuilding(UnitManager.BASE)) && !xvr.canAfford(525)) {
			return false;
		}

		// if (!xvr.canAfford(125)) {
		// return false;
		// }
		if (TerranBunker.shouldBuild() && !xvr.canAfford(250)) {
			return false;
		}

		return true;
	}

}

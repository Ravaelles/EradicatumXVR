package ai.managers;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.units.UnitManager;
import ai.terran.TerranFactory;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranStarport;

public class ArmyCreationManager {

	private static XVR xvr = XVR.getInstance();

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
			ArrayList<Unit> barracks = TerranBarracks.getAllObjects();
			if (!barracks.isEmpty()) {
				for (Unit barrack : barracks) {
					TerranBarracks.act(barrack);
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

		if (bases == 1
				&& (TerranCommandCenter.shouldBuild() || Constructing.weAreBuilding(UnitManager.BASE))
				&& !xvr.canAfford(525)) {
			return false;
		}

		if (battleUnits <= 6 || (battleUnits < StrategyManager.getMinBattleUnits() + 2)) {
			return true;
		}
		if (!xvr.canAfford(125)) {
			return false;
		}
		if (TerranBunker.shouldBuild() && !xvr.canAfford(250)) {
			return false;
		}

		return true;
	}

}

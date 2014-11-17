package ai._start;

import jnibwapi.JNIBWAPI;
import jnibwapi.model.Player;
import jnibwapi.types.RaceType.RaceTypes;
import jnibwapi.types.UnitDamages;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.terran.TerranCommandCenter;

public class BotStart {

	// private JNIBWAPI bwapi;
	// private XVR xvr;

	public static void start(JNIBWAPI bwapi, XVR xvr) {
		// bwapi = bwapi;
		// xvr = xvr;

		// Game settings
		bwapi.enableUserInput();
		bwapi.setGameSpeed(XVR.GAME_SPEED);
		bwapi.loadMapData(true);

		MapExploration.disableChokePointsNearFirstBase();

		// ========================================

		xvr.SELF = bwapi.getSelf();
		xvr.SELF_ID = bwapi.getSelf().getID();

		xvr.NEUTRAL = bwapi.getNeutralPlayer();

		Player enemy = bwapi.getEnemies().get(0);
		xvr.setENEMY(enemy);
		xvr.ENEMY_ID = enemy.getID();

		// ========================================

		// Creates map where values of attacks of all unit types are stored.
		UnitDamages.rememberUnitDamageValues();

		// Removes some of initial choke points e.g. those on the edge of the
		// map.
		MapExploration.processInitialChokePoints();

		// ========================================

		// Enemy -> Protoss
		if (enemy.getRaceID() == RaceTypes.Protoss.getID()) {
			XVR.setEnemyRace("Protoss");
		}
		// ENEMY -> Terran
		else if (enemy.getRaceID() == RaceTypes.Terran.getID()) {
			XVR.setEnemyRace("Terran");
		}
		// ENEMY -> Zerg
		else if (enemy.getRaceID() == RaceTypes.Zerg.getID()) {
			XVR.setEnemyRace("Zerg");
		}

		// ==========
		// HotFix
		TerranCommandCenter.initialMineralGathering();
	}

}

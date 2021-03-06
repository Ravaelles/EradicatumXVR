package ai.handling.other;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitCommandType.UnitCommandTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitActions;
import ai.terran.TerranComsatStation;

public class NukeHandling {

	public static MapPoint nuclearDetectionPoint = null;

	private static XVR xvr = XVR.getInstance();

	public static void nukeDetected(int x, int y) {
		// ######
		// ###### X AND Y ARE USUALLY JUST MISSILE SILOS !!!!!!
		// ######

		nuclearDetectionPoint = new MapPointInstance(x, y);
		MapPoint probableGhostLocation = null;

		// Only ghost can release nuke so get all enemy ghosts known
		ArrayList<Unit> enemyGhostsKnown = new ArrayList<Unit>();
		Unit motherfucker = null;
		for (Unit unit : xvr.getBwapi().getEnemyUnits()) {
			if (unit.getType().isGhost()) {
				enemyGhostsKnown.add(unit);
				if (unit.getLastCommandID() == UnitCommandTypes.Use_Tech_Position.ordinal()) {
					motherfucker = unit;
					System.out.println("## MOTHERFUCKER FOUND");
					break;
				}
			}
		}

		// Tough situation: we don't know of any enemy ghost; try to scan x,y...
		// =/
		if (enemyGhostsKnown.isEmpty() && motherfucker == null) {
			TerranComsatStation.tryToScanPoint(nuclearDetectionPoint);
			System.out.println("## HOPELESS NUKE CASE!");
		} else {
			if (motherfucker != null) {
				probableGhostLocation = new MapPointInstance(motherfucker.getX(),
						motherfucker.getY());
			} else {
				Unit someGhost = enemyGhostsKnown.get(0);
				System.out.println("## TRYING TO GUESS THAT THIS IS: " + someGhost);
				probableGhostLocation = new MapPointInstance(someGhost.getX(), someGhost.getY());
			}
			TerranComsatStation.tryToScanPoint(nuclearDetectionPoint);
		}

		// Send all units from given radius to fight the bastard!
		if (probableGhostLocation != null) {
			ArrayList<Unit> armyUnitsNearby = xvr.getArmyUnitsInRadius(probableGhostLocation, 40,
					true);
			System.out.println("## ATTACKING NUKE PLACE WITH: " + armyUnitsNearby.size()
					+ " SOLDIERS!");
			for (Unit unit : armyUnitsNearby) {
				UnitActions.attackTo(unit, probableGhostLocation);
			}
		} else {
			System.out.println("## GHOST POSITION UNKNOWN");
		}
	}

}

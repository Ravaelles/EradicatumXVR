package ai.handling.enemy;

import jnibwapi.model.BaseLocation;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.terran.TerranBunker;
import ai.utils.RUtilities;

public class TerranOffensiveBunker {

	private static XVR xvr = XVR.getInstance();

	private static MapPoint _offensivePoint = null;
	private static BaseLocation secondEnemyBase = null;

	// =========================================================

	public static MapPoint getRendezvousOffensive() {
		MapPoint enemyWhereabout = defineEnemyWhereabout();

		// if (enemyWhereabout != null) {
		// System.out.println("enemyWhereabout = " +
		// enemyWhereabout.toStringLocation());
		// }

		if (enemyWhereabout == null) {
			return null;
		} else if (_offensivePoint == null) {
			MapPoint enemyLocation = enemyWhereabout;
			secondEnemyBase = EnemyBases.getNearestBaseLocationForEnemy(enemyLocation);
			// if (secondEnemyBase != null) {

			// Build at first base choke point
			_offensivePoint = MapExploration.getImportantChokePointNear(enemyLocation);
			_offensivePoint = MapPointInstance.getPointBetween(_offensivePoint, secondEnemyBase, 7);

			// _offensivePoint =
			// MapExploration.getImportantChokePointNear(secondEnemyBase);
			// if (chokePoint != null) {
			// MapPoint point =

			// _offensivePoint =
			// MapPointInstance.getPointBetween(secondEnemyBase,
			// xvr.getFirstBase(), 11);

			// Constructing.getLegitTileToBuildNear(xvr.getRandomWorker(),
			// chokePoint, nearestFreeBaseLocation, 0, 10);
			// }
			// }
			// }
		}

		return _offensivePoint;
	}

	private static MapPoint defineEnemyWhereabout() {
		if (MapExploration.getEnemyBuildingsDiscovered().isEmpty()) {
			if (MapExploration.getCalculatedEnemyBaseLocation() != null) {
				return MapExploration.getCalculatedEnemyBaseLocation();
			}
		} else {
			return (MapPoint) RUtilities.getRandomElement(MapExploration
					.getEnemyBuildingsDiscovered());
		}
		return null;
	}

	// private static MapPoint getImportantPointNearOurBase() {
	// return MapExploration.getImportantChokePointNear(xvr.getFirstBase());
	// }

	// =========================================================

	public static boolean isStrategyActive() {
		return true;
	}

	public static MapPoint getTerranOffensiveBunkerPosition() {
		MapPoint offensivePoint = TerranOffensiveBunker.getRendezvousOffensive();
		if (offensivePoint != null) {
			return TerranBunker.findTileAtBase(TerranOffensiveBunker.getRendezvousOffensive());
		} else {
			return null;
		}
	}

	public static MapPoint getOffensivePoint() {
		return _offensivePoint;
	}

	public static BaseLocation getSecondEnemyBase() {
		return secondEnemyBase;
	}

}

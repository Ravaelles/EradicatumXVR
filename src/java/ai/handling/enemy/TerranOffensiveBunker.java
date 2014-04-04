package ai.handling.enemy;

import jnibwapi.model.BaseLocation;
import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.terran.TerranBunker;

public class TerranOffensiveBunker {

	private static XVR xvr = XVR.getInstance();

	private static MapPoint _offensivePoint = null;

	// =========================================================

	public static MapPoint getRendezvousOffensive() {
		if (MapExploration.getEnemyBuildingsDiscovered().isEmpty()) {
			return null;
		}
		if (_offensivePoint == null) {
			Unit enemyLocation = MapExploration.getNearestEnemyBuilding();
			BaseLocation secondEnemyBase = EnemyBases.getNearestBaseLocationForEnemy(enemyLocation);
			if (secondEnemyBase != null) {
				_offensivePoint = MapExploration.getImportantChokePointNear(secondEnemyBase);
				// if (chokePoint != null) {
				// MapPoint point =

				// _offensivePoint =
				// MapPointInstance.getPointBetween(secondEnemyBase,
				// xvr.getFirstBase(), 11);

				// Constructing.getLegitTileToBuildNear(xvr.getRandomWorker(),
				// chokePoint, nearestFreeBaseLocation, 0, 10);
				// }
			}
		}

		return _offensivePoint;
	}

	private static MapPoint getImportantPointNearOurBase() {
		return MapExploration.getImportantChokePointNear(xvr.getFirstBase());
	}

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

}

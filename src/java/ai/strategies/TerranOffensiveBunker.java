package ai.strategies;

import jnibwapi.model.BaseLocation;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.enemy.EnemyBases;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.managers.constructing.Constructing;
import ai.managers.units.army.ArmyCreationManager;
import ai.managers.units.workers.WorkerManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.utils.RUtilities;

public class TerranOffensiveBunker {

	private static XVR xvr = XVR.getInstance();

	protected static boolean isStrategyActive = false;

	private static MapPoint _offensivePoint = null;
	private static BaseLocation secondEnemyBase = null;

	// =========================================================

	public static void applyStrategy() {
		isStrategyActive = true;

		// BUNKER
		TerranBunker.MAX_STACK = 2;
		TerranBunker.GLOBAL_MAX_BUNKERS = 2;

		// BARRACKS
		TerranBarracks.enemyIsTerran();
		TerranBarracks.MAX_BARRACKS = 1;

		// UNITS
		ArmyCreationManager.MINIMUM_MARINES = 11;
		ArmyCreationManager.MAXIMUM_MARINES = 11;
		// ArmyCreationManager.MINIMUM_MARINES = 4;
		// ArmyCreationManager.MAXIMUM_MARINES = 4;

		// EXPLORER
		WorkerManager.EXPLORER_INDEX = 3;
	}

	// =========================================================

	public static MapPoint getRendezvousOffensive() {
		MapPoint enemyWhereabout = getEnemyWhereabout();

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

			// =========================================================
			// THIS WORKS FOR DEFAULT SC TERRAN AI

			// Higher this value, closer to the enemy base bunker will be built
			int offensivenessBonusFromTime = (int) Math.max(0,
					30 - (60 - xvr.getTimeSeconds()) / 1.5);
			int bunkerPositionOffensivenessRatio = 40 + offensivenessBonusFromTime;

			System.out.println("time bonus: " + offensivenessBonusFromTime);
			if (bunkerPositionOffensivenessRatio > 83) {
				bunkerPositionOffensivenessRatio = 83;
			}

			// if (true || xvr.getENEMY().getName().contains("Krystev")) {
			// bunkerPositionOffensivenessRatio = 60;
			// }

			// Build at first base choke point
			MapPoint firstBaseChokePoint = MapExploration.getImportantChokePointNear(enemyLocation);
			_offensivePoint = MapPointInstance.getPointBetween(firstBaseChokePoint,
					enemyWhereabout, bunkerPositionOffensivenessRatio);

			// =========================================================
			// THIS WORKS FOR DEFAULT SC TERRAN AI

			// // Build at first base choke point
			// _offensivePoint =
			// MapExploration.getImportantChokePointNear(enemyLocation);
			// _offensivePoint =
			// MapPointInstance.getPointBetween(_offensivePoint,
			// secondEnemyBase, 7);

			// =========================================================

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

	public static MapPoint getEnemyWhereabout() {
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

	public static MapPoint getTerranOffensiveBunkerPosition() {
		MapPoint offensivePoint = TerranOffensiveBunker.getRendezvousOffensive();
		if (offensivePoint != null) {
			return TerranBunker.findTileAtBase(TerranOffensiveBunker.getRendezvousOffensive());
		} else {
			return null;
		}
	}

	public static MapPoint getTerranSecondOffensiveBunkerPosition() {
		MapPoint firstBunker = getTerranOffensiveBunkerPosition();
		if (firstBunker != null) {
			MapPoint apprxPoint = MapPointInstance.getPointBetween(firstBunker,
					getEnemyWhereabout(), 80);
			// MapPoint apprxPoint =
			// MapPointInstance.getPointBetween(firstBunker,
			// getEnemyWhereabout(), 80);
			return Constructing.getLegitTileToBuildNear(UnitTypes.Terran_Bunker, apprxPoint, 0, 15);
		} else {
			return null;
		}
	}

	// private static MapPoint getImportantPointNearOurBase() {
	// return MapExploration.getImportantChokePointNear(xvr.getFirstBase());
	// }

	// =========================================================

	public static boolean isStrategyActive() {
		return isStrategyActive;
	}

	// public static void activateStrategy() {
	// isStrategyActive = true;
	// }
	//
	// public static void disableStrategy() {
	// isStrategyActive = false;
	// }

	public static MapPoint getOffensivePoint() {
		return _offensivePoint;
	}

	public static BaseLocation getSecondEnemyBase() {
		return secondEnemyBase;
	}

	public static MapPoint getImportantChokeNearEnemyMainBase() {
		MapPoint enemyWhereabout = getEnemyWhereabout();
		if (enemyWhereabout != null) {
			return MapExploration.getImportantChokePointNear(enemyWhereabout);
		}
		return null;
	}

}

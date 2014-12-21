package ai.managers.units.army;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.specialforces.SpecialForces;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.terran.TerranBunker;
import ai.terran.TerranSiegeTank;

public class BunkerManager {

	private static final int MIN_DIST_TO_LEAVE_BUNKER = 5;

	private static XVR xvr = XVR.getInstance();

	// =========================================================

	public static boolean tryLoadingIntoBunkersIfPossible(Unit unit) {
		if (!unit.getType().isTerranInfantry()) {
			return false;
		}

		if (TerranBunker.getNumberOfUnitsCompleted() == 0) {
			return false;
		}

		if (SpecialForces.isMainBaseProtector(unit)) {
			return false;
		}

		boolean isUnitLoaded = unit.isLoaded();
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		double enemyIsNearThreshold = nearestEnemy != null ? Math.max(3.7, nearestEnemy.getType()
				.getGroundWeapon().getMaxRangeInTiles() + 2.5) : 3;
		boolean enemyIsNearby = nearestEnemy != null
				&& nearestEnemy.distanceTo(unit) <= enemyIsNearThreshold;

		// if (StrategyManager.isAnyAttackFormPending() && !unit.isIdle() &&
		// !enemyIsNearby) {
		// if (isUnitLoaded || isBunkerTooFarFromCombat(unit,
		// unit.getBunkerThatsIsLoadedInto())) {
		// unit.unload();
		// }
		// return false;
		// }
		// if (!enemyIsNearby) {
		// if (isUnitInsideBunker) {
		// unit.unload();
		// }
		// return false;
		// }

		// If unit should be inside bunker, try to load it inside.
		if (!isUnitLoaded) {
			if (!enemyIsNearby && StrategyManager.isAnyAttackFormPending() && !unit.isIdle()) {
				return false;
			}
			// enemyIsNearby &&
			if (loadIntoBunkerNearbyIfPossible(unit)) {
				// unit.setIsRunningFromEnemyNow();
				return true;
			}
		}

		// Unit is inside a bunker
		else {
			if (shouldUnitUnloadFromBunker(unit)) {
				unit.unload();
				return false;
			}
		}

		return false;
	}

	public static boolean loadIntoBunkerNearbyIfPossible(Unit unit) {
		final int MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT = 28;

		if (unit.getType().isMedic()) {
			return false;
		}

		if (shouldUnitUnloadFromBunker(unit)) {
			return false;
		}

		// Define what is the nearest bunker to this unit
		Unit nearestBunker = xvr.getUnitOfTypeNearestTo(UnitTypes.Terran_Bunker, unit);
		if (nearestBunker == null) {
			return false;
		}
		double distToBunker = nearestBunker.distanceTo(unit);

		// If the distance to bunker is small, try to load into it if possible
		if (distToBunker <= MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT) {

			// // If this bunker is free, load into it.
			// if (nearestBunker.getNumLoadedUnits() < 4) {
			// UnitActions.loadUnitInto(unit, nearestBunker);
			// return;
			// }
			//
			// // Bunker is full, try to find other bunkers in radius that may
			// have
			// // space inside
			// else {
			//
			// // Get the list of bunkers that are near unit.
			// ArrayList<Unit> bunkersNearby = xvr.getUnitsOfGivenTypeInRadius(
			// UnitTypes.Terran_Bunker, MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT,
			// unit, true);
			// for (Unit bunker : bunkersNearby) {
			// if (bunker.getNumLoadedUnits() < 4) {
			// UnitActions.loadUnitInto(unit, bunker);
			// return;
			// }
			// }
			// }

			// If this bunker is free, load into it.
			if (nearestBunker.getNumLoadedUnits() < 4 && nearestBunker.isCompleted()) {
				UnitActions.loadUnitInto(unit, nearestBunker);
				return true;
			}

			// Bunker is full, try to find other bunkers in radius that may have
			// space inside
			else {

				// Define place around which we will look for a bunker. If we
				// look for a bunker nearest to the nearest enemy building, we
				// will make sure always the most distant bunkers are full.
				Unit nearestEnemyBuilding = MapExploration.getNearestEnemyBuilding();
				MapPoint bunkersNearestTo = nearestEnemyBuilding != null ? nearestEnemyBuilding
						: unit;

				// Get the list of bunkers that are near.
				ArrayList<Unit> bunkersNearby = xvr.getUnitsInRadius(bunkersNearestTo, 300,
						xvr.getUnitsOfType(UnitTypes.Terran_Bunker));
				for (Unit bunker : bunkersNearby) {
					if (bunker.getNumLoadedUnits() < 4 && nearestBunker.isCompleted()
							&& bunker.distanceTo(unit) <= MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT) {
						UnitActions.loadUnitInto(unit, bunker);
						return true;
					}
				}
			}
		}
		return false;
	}

	// =========================================================

	private static boolean shouldUnitUnloadFromBunker(Unit unit) {
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		double enemyIsNearThreshold = nearestEnemy != null ? Math.max(3.7, nearestEnemy.getType()
				.getGroundWeapon().getMaxRangeInTiles() + 2.5) : 3;
		boolean enemyIsNearby = nearestEnemy != null
				&& nearestEnemy.distanceTo(unit) <= enemyIsNearThreshold;

		if (enemyIsNearby || unit.isWounded() || xvr.getTimeSeconds() < 330) {
			return false;
		}

		// If there's an attack pending, consider getting out of the bunker.
		if (StrategyManager.isAnyAttackFormPending() || TerranSiegeTank.getNumberOfUnits() >= 2) {
			// Unit bunker = unit.getBunkerThatsIsLoadedInto();
			// if (bunker != null && bunker.getNumLoadedUnits() > 1) {
			MapPoint safePoint = ArmyRendezvousManager.getDefensivePoint(unit);
			if (safePoint != null && safePoint.distanceTo(unit) >= MIN_DIST_TO_LEAVE_BUNKER) {
				return true;
			}
			// }
		}

		return true;
	}

	public static boolean isBunkerTooFarFromCombat(Unit unit, Unit bunker) {
		if (unit == null || bunker == null) {
			return false;
		}

		return bunker.distanceTo(unit) >= 7;
	}

}

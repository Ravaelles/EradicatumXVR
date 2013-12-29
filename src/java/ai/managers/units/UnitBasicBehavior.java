package ai.managers.units;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.managers.StrategyManager;
import ai.terran.TerranBunker;
import ai.terran.TerranMedic;
import ai.terran.TerranSiegeTank;
import ai.terran.TerranVulture;
import ai.terran.TerranWraith;

public class UnitBasicBehavior {

	private static XVR xvr = XVR.getInstance();

	protected static void act(Unit unit) {
		UnitType unitType = unit.getType();
		if (unitType == null) {
			return;
		}

		// ======================================
		// OVERRIDE COMMANDS FOR SPECIFIC UNITS

		// Vulture
		if (unitType.isVulture()) {
			TerranVulture.act(unit);
			return;
		}

		// Medic
		else if (unitType.isMedic()) {
			TerranMedic.act(unit);
			return;
		}

		// ======================================
		// STANDARD ARMY UNIT COMMANDS
		else {

			// If unit has personalized order
			if (unit.getCallForHelpMission() != null) {
				UnitManager.actWhenOnCallForHelpMission(unit);
			}

			// Standard action for unit
			else {

				// If we're ready to total attack
				if (StrategyManager.isAttackPending()) {
					UnitManager.actWhenMassiveAttackIsPending(unit);
				}

				// Standard situation
				else {
					UnitManager.actWhenNoMassiveAttack(unit);
				}
			}
		}

		// ======================================
		// SPECIFIC ACTIONS for units, but DON'T FULLY OVERRIDE standard
		// behavior

		// Tank
		if (unitType.isTank()) {
			TerranSiegeTank.act(unit);
		}

		// Wraith
		else if (unitType.isWraith()) {
			TerranWraith.act(unit);
		}
	}

	public static boolean runFromCloseOpponentsIfNecessary(Unit unit) {
		UnitType type = unit.getType();
		if (unit.isRunningFromEnemy()) {
			return true;
		}

		// Don't interrupt when just starting an attack
		if ((unit.isStartingAttack() && !unit.isWounded()) || unit.isLoaded()
				|| unit.getType().isFirebat()) {
			return false;
		}

		// =============================================
		// Define nearest enemy (threat)
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);

		// If there's dangerous enemy nearby and he's close, try to move away.
		boolean unitHasMovedItsAss = false;
		if (nearestEnemy != null && !nearestEnemy.isWorker()) {
			if (unit.isStartingAttack() && nearestEnemy.distanceTo(unit) >= 2) {
				return false;
			}

			int ourShootRange = type.getGroundWeapon().getMaxRangeInTiles();
			boolean isEnemyVeryClose = nearestEnemy.distanceTo(unit) <= ourShootRange - 1;
			if (isEnemyVeryClose) {

				// ===================================
				// SPECIAL UNITS
				// Sieged tanks have to unsiege first
				if (type.isTank() && unit.isSieged()) {
					unit.unsiege();
					return true;
				}

				// ===================================
				// Define attack range
				int enemyShootRange = nearestEnemy.getType().getGroundWeapon().getMaxRangeInTiles();
				boolean weHaveBiggerRangeThanEnemy = ourShootRange > enemyShootRange;

				// Check if we have bigger shoot range than the enemy. If not,
				// it doesn't make any sense to run away from him.
				if (weHaveBiggerRangeThanEnemy) {
					if (type.isTerranInfantry()
							&& UnitBasicBehavior.tryLoadingIntoBunkersIfPossible(unit)) {
						unit.setIsRunningFromEnemyNow();
						return true;
					} else {
						UnitActions.moveAwayFromUnit(unit, nearestEnemy);
						unit.setIsRunningFromEnemyNow();
						return true;
					}
				}
			}
		}

		return unitHasMovedItsAss;
	}

	public static boolean tryLoadingIntoBunkersIfPossible(Unit unit) {
		if (TerranBunker.getNumberOfUnitsCompleted() == 0) {
			return false;
		}

		boolean isUnitInsideBunker = unit.isLoaded();
		boolean enemyIsNearby = xvr.getNearestEnemyInRadius(unit, 11, true, true) != null;

		// If unit should be inside bunker, try to load it inside.
		if (!isUnitInsideBunker) {
			if (!enemyIsNearby && StrategyManager.isAnyAttackFormPending()) {
				return false;
			}
			if (loadIntoBunkerNearbyIfPossible(unit)) {
				unit.setIsRunningFromEnemyNow();
				return true;
			}
		}

		// Unit is inside a bunker
		else {
			if (!enemyIsNearby) {
				Unit bunker = unit.getBunkerThatsIsLoadedInto();
				if (bunker != null && bunker.getNumLoadedUnits() > 1
						&& StrategyManager.isAnyAttackFormPending()) {
					unit.unload();
					return false;
				}
			}
		}

		return false;
	}

	protected static boolean loadIntoBunkerNearbyIfPossible(Unit unit) {
		final int MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT = 28;

		if (unit.getType().isMedic()) {
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
			if (nearestBunker.getNumLoadedUnits() < 1) {
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
					if (bunker.getNumLoadedUnits() < 4
							&& bunker.distanceTo(unit) <= MAX_DIST_TO_BUNKER_TO_LOAD_INTO_IT) {
						UnitActions.loadUnitInto(unit, bunker);
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean tryRunningFromCloseDefensiveBuilding(Unit unit) {
		if (xvr.isEnemyDefensiveGroundBuildingNear(unit)) {
			UnitActions.moveToSafePlace(unit);
			unit.setIsRunningFromEnemyNow();
			return true;
		} else {
			return false;
		}
	}

}

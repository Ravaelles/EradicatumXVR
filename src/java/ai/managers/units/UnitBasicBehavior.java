package ai.managers.units;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.WeaponType;
import ai.core.XVR;
import ai.handling.army.StrengthRatio;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.managers.StrategyManager;
import ai.managers.TechnologyManager;
import ai.terran.TerranBunker;
import ai.terran.TerranMedic;
import ai.terran.TerranSiegeTank;
import ai.terran.TerranVulture;
import ai.utils.RUtilities;

public class UnitBasicBehavior {

	private static XVR xvr = XVR.getInstance();

	private static final double SAFE_DIST_FROM_ENEMY = 1.9;

	// =============================================

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
	}

	public static boolean runFromCloseOpponentsIfNecessary(Unit unit) {
		UnitType type = unit.getType();
		boolean hasPrettyGoodChances = unit.getStrengthRatio() > 1.6 || unit.getStrengthRatio() < 0;

		double distanceBonusIfWounded = (unit.isWounded() ? 0.95 : 0);
		double criticallyCloseDistance = 3 + distanceBonusIfWounded + (type.isTank() ? 1.5 : 0)
				- (type.isMedic() ? 1 : 0);
		boolean safeFromEnemyShootRange = UnitManager.isUnitSafeFromEnemiesShootRange(unit,
				xvr.getEnemyUnitsInRadius(11, unit));

		// If unit can be attacked by enemy distant attack and chances aren't
		// great, pull back.
		if (!safeFromEnemyShootRange && !hasPrettyGoodChances) {
			unit.setIsRunningFromEnemyNow();
			UnitActions.moveToSafePlace(unit);
			return true;
		}

		// Define nearest enemy (threat)
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		double distToEnemy = unit.distanceTo(nearestEnemy);
		if (distToEnemy < 0) {
			return false;
		}

		// If near units are retreating and enemy is nearby, also run.
		if (distToEnemy <= 6
				&& UnitManager.areVeryCloseUnitsReatreting(unit,
						xvr.getUnitsInRadius(unit, 4, xvr.getUnitsArmy()))) {
			unit.setIsRunningFromEnemyNow();
			UnitActions.moveToSafePlace(unit);
			return true;
		}

		if (distToEnemy <= 1.5 && !nearestEnemy.getType().isZergling()) {
			unit.setIsRunningFromEnemyNow();
			UnitActions.moveAwayFromUnit(unit, nearestEnemy);
			return true;
		}

		boolean isEnemyCriticallyClose = distToEnemy < criticallyCloseDistance
				- (nearestEnemy.getType().isZergling() ? 1 : 0);

		// ==============================================

		if (unit.isRunningFromEnemy() && (!hasPrettyGoodChances && isEnemyCriticallyClose)) {
			unit.setIsRunningFromEnemyNow();
			// UnitActions.moveAwayFromNearestEnemy(unit);
			UnitActions.moveToSafePlace(unit);
			return true;
		}

		// Don't interrupt when just starting an attack
		if (unit.isLoaded() || (unit.getType().isFirebat() && unit.getHP() > 25)) {
			return false;
		}

		// =============================================

		// CHECK if to RUN, but include STRENGTH RATIO
		if (distToEnemy >= 0 && distToEnemy <= criticallyCloseDistance && !hasPrettyGoodChances) {
			unit.setIsRunningFromEnemyNow();
			// UnitActions.moveAwayFromNearestEnemy(unit);
			UnitActions.moveToSafePlace(unit);
			return true;
		}

		// RUN if ENEMY very CLOSE
		if (distToEnemy >= 0 && distToEnemy <= 2.5 + distanceBonusIfWounded) {
			unit.setIsRunningFromEnemyNow();
			// UnitActions.moveAwayFromNearestEnemy(unit);
			UnitActions.moveToSafePlace(unit);
			return true;
		}

		// if ((!unit.isWounded() || (unit.getGroundWeaponCooldown() > 0 &&
		// !isEnemyCriticallyClose))
		// && !unit.isUnderAttack()
		// && xvr.countUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(),
		// 3.5, unit,
		// true) > 0) {
		// return false;
		// }

		// If there's dangerous enemy nearby and he's close, try to move away.
		boolean unitHasMovedItsAss = false;
		if (nearestEnemy != null && !nearestEnemy.isWorker()) {
			// if (unit.isStartingAttack() && nearestEnemy.distanceTo(unit) >=
			// SAFE_DIST_FROM_ENEMY) {
			// return false;
			// }

			int ourShootRange = type.getGroundWeapon().getMaxRangeInTiles();
			boolean isEnemyVeryClose = distToEnemy <= SAFE_DIST_FROM_ENEMY;

			// If enemy is close, run!
			if (isEnemyVeryClose) {

				// ===================================
				// SPECIAL UNITS
				// Sieged tanks have to unsiege first
				if (type.isTank() && unit.isSieged() && unit.getStrengthRatio() < 1.7) {
					unit.unsiege();
					return true;
				}
			}

			// Enemy isn't critically close
			else {

				// ===================================
				// Define attack range
				int enemyShootRange = nearestEnemy.getType().getGroundWeapon().getMaxRangeInTiles();
				boolean weHaveBiggerRangeThanEnemy = ourShootRange > enemyShootRange;

				// Check if we have bigger shoot range than the enemy. If not,
				// it doesn't make any sense to run away from him just because
				// he's near.
				if (weHaveBiggerRangeThanEnemy && unit.getGroundWeaponCooldown() > 0) {
					// UnitActions.moveAwayFromUnit(unit, nearestEnemy);
					UnitActions.moveToSafePlace(unit);
					unit.setIsRunningFromEnemyNow();
					return true;
				}
			}
		}

		return unitHasMovedItsAss;
	}

	public static boolean tryLoadingIntoBunkersIfPossible(Unit unit) {
		if (!unit.getType().isTerranInfantry()) {
			return false;
		}

		if (TerranBunker.getNumberOfUnitsCompleted() == 0) {
			return false;
		}

		boolean isUnitInsideBunker = unit.isLoaded();
		// boolean enemyIsNearby = xvr.getNearestEnemyInRadius(unit, 12, true,
		// true) != null;
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		if (nearestEnemy == null) {
			if (isUnitInsideBunker) {
				unit.unload();
			}
			return false;
		}

		// Calculate max safe distance to the enemy, so we can shoot at him
		double enemyIsNearThreshold = Math.max(3.7, nearestEnemy.getType().getGroundWeapon()
				.getMaxRangeInTiles() + 2.5);
		boolean enemyIsNearby = nearestEnemy != null
				&& nearestEnemy.distanceTo(unit) <= enemyIsNearThreshold;

		if (!enemyIsNearby) {
			if (isUnitInsideBunker) {
				unit.unload();
			}
			return false;
		}

		// If unit should be inside bunker, try to load it inside.
		if (!isUnitInsideBunker) {
			if (!enemyIsNearby && StrategyManager.isAnyAttackFormPending()) {
				if (isUnitInsideBunker) {
					unit.unload();
				}
				return false;
			}
			// enemyIsNearby &&
			if (unit.getStrengthRatio() < 1.7 && loadIntoBunkerNearbyIfPossible(unit)) {
				// unit.setIsRunningFromEnemyNow();
				return true;
			}
		}

		// Unit is inside a bunker
		else {
			if (!enemyIsNearby) {
				Unit bunker = unit.getBunkerThatsIsLoadedInto();
				if (bunker != null && bunker.getNumLoadedUnits() > 1
						&& StrategyManager.isAnyAttackFormPending()) {
					if (isUnitInsideBunker) {
						unit.unload();
					}
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
		Unit defensiveBuilding = xvr.getEnemyDefensiveGroundBuildingNear(unit);
		if (defensiveBuilding != null) {
			if (unit.getType().isTank()) {
				if (unit.distanceTo(defensiveBuilding) <= 10.8) {
					unit.siege();
				}
				return false;
			} else {
				UnitActions.moveAwayFromUnit(unit, defensiveBuilding);
				unit.setIsRunningFromEnemyNow();
				return true;
			}
		} else {
			return false;
		}
	}

	protected static boolean tryAvoidingSeriousSpellEffectsIfNecessary(Unit unit) {
		if (unit.isUnderStorm() || unit.isUnderDisruptionWeb()) {
			if (unit.isMoving()) {
				return true;
			}
			UnitActions.moveTo(unit, unit.getX() + 5 * 32 * (-1 * RUtilities.rand(0, 1)),
					unit.getY() + 5 * 32 * (-1 * RUtilities.rand(0, 1)));
			return true;
		}
		return false;
	}

	protected static boolean tryAvoidingActivatedSpiderMines(Unit unit) {
		if (unit.getType().isFlyer()) {
			return false;
		}

		Unit activatedMine = null;

		// Check if there's any activted mine nearby and if so, get the fuck out
		// of here.
		for (Unit spiderMine : xvr.getUnitsOfGivenTypeInRadius(
				UnitTypes.Terran_Vulture_Spider_Mine, 4, unit, true)) {
			if (spiderMine.isMoving() || spiderMine.isAttacking()) {
				activatedMine = spiderMine;
				break;
			}
		}

		// Move away from activated mine
		if (activatedMine != null) {
			UnitActions.moveAwayFromUnit(unit, activatedMine);
			unit.setIsRunningFromEnemyNow();
			unit.setAiOrder("MINE !!!");
			return true;
		}

		return false;
	}

	protected static boolean tryRunningIfSeriouslyWounded(Unit unit) {
		double ratio = 0.4;
		if (xvr.getTimeSeconds() < 330) {
			ratio = 0.62;
		}

		if (unit.getHP() <= unit.getMaxHP() * ratio) {
			// // If there are tanks nearby, DON'T RUN. Rather die first!
			// if
			// (xvr.countUnitsEnemyOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode,
			// 15,
			// unit) > 0) {
			// return;
			// }

			// // If there are tanks nearby, DON'T RUN. Rather die first!
			// if (unit.distanceTo(xvr.getFirstBase()) < 17) {
			// return;
			// }

			// if (StrategyManager.isAttackPending()) {
			// return;
			// }

			boolean lowLife = UnitActions.actWhenLowHitPointsOrShields(unit, false);
			if (lowLife) {
				unit.setAiOrder("Almost dead");
			}

			if (unit.isRepairable()) {
				RepairAndSons.issueTicketToRepairIfHasnt(unit);

				Unit repairer = RepairAndSons.getRepairerForUnit(unit);
				if (repairer != null) {
					if (repairer.isConstructing() || !repairer.isExists()
							|| (!repairer.isRepairing() && !repairer.isMoving())) {
						RepairAndSons.removeTicketFor(unit, repairer);
						RepairAndSons.issueTicketToRepairIfHasnt(unit);
					}
					if (repairer.distanceTo(unit) >= 1.3) {
						UnitActions.moveTo(unit, repairer);
						unit.setAiOrder("Go to repair");
					} else {
						Unit nearEnemy = xvr.getNearestEnemyInRadius(unit, 11, true, true);
						if (nearEnemy != null) {
							WeaponType groundWeapon = nearEnemy.getType().getGroundWeapon();
							if (groundWeapon != null
									&& unit.distanceTo(nearEnemy) <= groundWeapon
											.getMaxRangeInTiles() + 1.9) {
								unit.setAiOrder("Almost dead - Run");
								UnitActions.moveAwayFromNearestEnemy(unit);
							}
						}
					}
				}

			}
			return lowLife;
		}
		return false;
	}

	protected static void avoidHiddenUnitsIfNecessary(Unit unit) {
		Unit hiddenEnemyUnitNearby = MapExploration.getHiddenEnemyUnitNearbyTo(unit);
		if (hiddenEnemyUnitNearby != null && unit.isDetected()
				&& !hiddenEnemyUnitNearby.isDetected()) {
			UnitActions.moveAwayFromUnit(unit, hiddenEnemyUnitNearby);
			unit.setAiOrder("Avoid hidden unit");
		}
	}

	public static void tryUsingStimpacksIfNeeded(Unit unit) {
		if (unit.getType().canUseStimpacks() && TechnologyManager.isStimpacksResearched()
				&& !unit.isStimmed()) {
			if (!unit.isWounded() && xvr.countUnitsEnemyInRadius(unit, 8) >= 2) {
				UnitActions.useTech(unit, TechnologyManager.STIMPACKS);
			}
		}
	}

	protected static boolean tryRetreatingIfChancesNotFavorable(Unit unit) {

		// If no base isn't existing, screw this.
		Unit firstBase = xvr.getFirstBase();
		if (firstBase == null) {
			return false;
		}

		// ============================================
		// Some top level situations when don't try retreating

		// If no enemy is critically close, don't retreat
		if (xvr.getNearestEnemyDistance(unit, true, false) <= 2) {
			return false;
		}

		// Don't interrupt unit that has just started shooting.
		if (unit.isStartingAttack()) {
			return false;
		}

		// MEDICS can run only if INJURED
		if (unit.getType().isMedic() && !unit.isWounded()) {
			return false;
		}

		// If unit isn't attacking or is very close to the critical first base,
		// don't retreat.
		// if ((!unit.isAttacking() && !unit.isWorker())
		// || xvr.getDistanceSimple(unit, firstBase) <= 15) {
		// return;
		// }

		// ============================================
		// Now is a block of situations where we shouldn't allow a retreat.

		// If there's our first base nearby
		// if (xvr.getDistanceBetween(
		// xvr.getUnitNearestFromList(unit, TerranCommandCenter.getBases()),
		// unit) <= 10) {
		// return;
		// }

		// If there's OUR BUNKER nearby, we should be here at all costs, because
		// if we lose this position, then every other battle will be far tougher
		// than fighting here, near the bunker.
		if (!unit.isWounded()
				&& unit.getGroundWeaponCooldown() > 0
				&& xvr.countUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(), 3.5, unit,
						true) > 0) {
			// if () {
			return false;
			// }
		}

		// ===============================================
		// If all is fine, we can CALCULATE CHANCES TO WIN
		// and if we wouldn't win, then go where it's safe
		// and by doing this we may encounter some help.
		if (!StrengthRatio.isStrengthRatioFavorableFor(unit)) {
			// UnitActions.moveToSafePlace(unit);
			UnitActions.moveAwayFromNearestEnemy(unit);
			return true;
		}

		return false;
	}

}

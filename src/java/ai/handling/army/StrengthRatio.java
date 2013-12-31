package ai.handling.army;

import java.util.ArrayList;
import java.util.Iterator;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import ai.core.Painter;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitCounter;
import ai.managers.StrategyManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranSiegeTank;

public class StrengthRatio {

	private static XVR xvr = XVR.getInstance();

	public static final int STRENGTH_RATIO_FULLY_SAFE = -69;
	public static final int STRENGTH_RATIO_VERY_BAD = 0;
	private static final int BATTLE_RADIUS_ENEMIES = 12;
	private static final int BATTLE_RADIUS_ALLIES = 11;
	private static final double CRITICAL_RATIO_THRESHOLD = 0.7;
	private static final double RATIO_PENALTY_FOR_CLOSE_CHOKE_POINT = 0.3;
	private static final double FAVORABLE_RATIO_THRESHOLD = 1.7;
	private static final double ENEMY_RANGE_WEAPON_STRENGTH_BONUS = 1.5;
	private static final int RANGE_BONUS_IF_ENEMY_DEF_BUILDING_NEAR = 6;
	private static final int DEFENSIVE_BUILDING_ATTACK_BONUS = 24;
	// private static final int MAX_TILES_FROM_TANK = 7;
	// private static final double RATIO_PENALTY_FOR_NO_TANK_NEARBY = 1;

	private static boolean changePlanToBuildAntiAirUnits = false;

	private static int _rangeBonus = 0;
	private static int _enemyDefensiveBuildings = 0;

	private static ArrayList<Unit> _ourUnits;
	private static ArrayList<Unit> _enemyUnits;

	// private static double _distToNearestTank = -1;

	// =====================================================

	/**
	 * Calculates ground advantage for given unit, based on nearby enemies and
	 * allied units. Value less than 1 means we would probably loss the fight.
	 * For convenience, use other static methods of this class that return
	 * boolean type.
	 */
	protected static double calculateStrengthRatioFor(Unit unit) {
		_rangeBonus = 0;
		UnitType type = unit.getType();
		double ratio = 0.01;
		boolean isTank = type.isTank();
		boolean isWorker = type.isWorker();
		// boolean isInfantry = type.isTerranInfantry();
		boolean noTanksYet = TerranSiegeTank.getNumberOfUnitsCompleted() == 0;
		boolean canAttackLonely = isWorker || type.isVulture() || type.isGhost();

		// ===================================================
		// SPECIAL unit actions

		if (isWorker && unit.isRepairing() && unit.getHP() > 18) {
			return 10;
		}

		if (!noTanksYet && !canAttackLonely && StrategyManager.isAnyAttackFormPending()) {
			MapPoint properPlace = unit.getProperPlaceToBe();
			if (properPlace != null
					&& properPlace.distanceTo(unit) > StrategyManager
							.getAllowedDistanceFromSafePoint()) {
				return STRENGTH_RATIO_VERY_BAD;
			}
		}

		// ===================================================

		// If there's at least one building like cannon, sunken colony, bunker,
		// then increase range of units search and look again for enemy units.
		if (xvr.getEnemyDefensiveGroundBuildingNear(unit.getX(), unit.getY(), BATTLE_RADIUS_ENEMIES
				+ RANGE_BONUS_IF_ENEMY_DEF_BUILDING_NEAR) != null) {
			_rangeBonus += RANGE_BONUS_IF_ENEMY_DEF_BUILDING_NEAR;
		}

		// Define enemy units nearby and our units nearby
		ArrayList<Unit> enemyUnits = getEnemiesNear(unit);
		_enemyUnits = enemyUnits;

		// ==================================================
		// Check if unit is quite safe

		// If there's no enemy units
		if (canAttackLonely && enemyUnits.isEmpty() && unit.getGroundWeaponCooldown() == 0) {
			return STRENGTH_RATIO_FULLY_SAFE;
		}

		// If no enemy can shoot at this unit right now, it's still safe
		boolean isUnitSafeFromEnemyShootRange = isUnitSafeFromEnemyShootRange(unit);
		if (isUnitSafeFromEnemyShootRange && unit.getGroundWeaponCooldown() == 0) {
			return STRENGTH_RATIO_FULLY_SAFE;
		}
		// ==================================================

		ArrayList<Unit> ourUnits = getOurUnitsNear(unit);
		int ourUnitsGroupSize = ourUnits.size();
		_ourUnits = ourUnits;

		if (isTank && ourUnitsGroupSize <= 2) {
			_ourUnits.remove(unit);
			if (weHaveOnlyMedics(unit)) {
				return STRENGTH_RATIO_VERY_BAD;
			}
			_ourUnits.add(unit);
		}

		if (weHaveOnlyMedics(unit)) {
			return STRENGTH_RATIO_VERY_BAD;
		}

		if (isEnemyAirUnitNearbyThatCanShootGround(unit) && isCriticalSituationVersusAirUnits(unit)) {
			return STRENGTH_RATIO_VERY_BAD;
		}

		// ==================================
		// Disallow attacking lonely
		boolean ourGroupToSmall = ourUnitsGroupSize <= 3;
		if (!noTanksYet && !canAttackLonely && ourGroupToSmall) {
			return STRENGTH_RATIO_VERY_BAD;
		}

		if (!noTanksYet && !canAttackLonely) {
			boolean shouldGoBackNearBunker = shouldGoBackNearBunker(unit);
			if (shouldGoBackNearBunker && ourUnitsGroupSize <= 4) {
				return STRENGTH_RATIO_VERY_BAD;
			}
		}

		// ==================================
		// Calculate hit points and ground attack values of units nearby

		double ourHitPoints = calculateHitPointsOf(ourUnits);
		double enemyHitPoints = calculateHitPointsOf(enemyUnits);

		double ourAttack = calculateTotalAttackOf(ourUnits, false);
		double enemyAttack = calculateTotalAttackOf(enemyUnits, true);

		// ==================================
		// Calculate "strength" for us and for the enemy, being correlated
		// values of hit points and attack values
		double ourStrength = ourHitPoints / (enemyAttack + 0.1);
		double enemyStrength = enemyHitPoints / (ourAttack + 0.1);

		// Final strength value for us is ratio (comparison) of our and enemy
		// calculated strengths
		// Its range is (0.0; Infinity).
		// Ratio 1.0 means forces are perfectly equal (according to this metric)
		// Ratio < 1.0 means
		ratio += ourStrength / enemyStrength;

		// Include info about choke points near
		if (StrategyManager.isAnyAttackFormPending()) {
			if (isCriticallyCloseToChokePoint(unit)) {
				ratio -= RATIO_PENALTY_FOR_CLOSE_CHOKE_POINT;
			}
		}

		// System.out.println("\n========= RATIO: " + ratio);
		// System.out.println("WE: " + ourStrength);
		// for (Unit unit : ourUnits) {
		// System.out.println("   " + unit.getName() + " -> "
		// + unit.getGroundAttackNormalized());
		// }
		// System.out.println("ENEMY: " + enemyStrength);
		// for (Unit unit : enemyUnits) {
		// System.out.println("   " + unit.getName() + " -> "
		// + unit.getGroundAttackNormalized());
		// }

		// ========================================
		// Coordinate troops

		// Ensure we're attacking in large groups
		if (ourUnitsGroupSize >= 6 && ratio < 0.8) {
			StrategyManager.waitForMoreUnits();
		}
		if (ourUnitsGroupSize >= UnitCounter.getNumberOfBattleUnitsCompleted() * 0.4
				&& ratio < FAVORABLE_RATIO_THRESHOLD) {
			StrategyManager.reduceAllowedDistanceFromSafePoint();
		}
		if (_enemyDefensiveBuildings * 8 >= ourUnitsGroupSize && ourUnitsGroupSize <= 25) {
			return STRENGTH_RATIO_VERY_BAD;
		}

		// Disallow small groups of units too attack later in the game
		if (!isWorker && !canAttackLonely && ourUnitsGroupSize <= 5 && xvr.getTimeSeconds() > 310) {
			return STRENGTH_RATIO_VERY_BAD;
		}

		// ========================================

		_rangeBonus = 0;
		return ratio;
	}

	private static boolean weHaveOnlyMedics(Unit unit) {
		for (Unit ourUnit : _ourUnits) {
			if (!ourUnit.getType().isMedic()) {
				return false;
			}
		}
		return xvr.getDistanceBetween(xvr.getFirstBase(), unit) >= 24;
	}

	private static boolean isUnitSafeFromEnemyShootRange(Unit unit) {
		// int ourRange = unit.getType().getGroundWeapon().getMinRangeInTiles();

		for (Unit enemy : _enemyUnits) {
			int enemyRange = enemy.getType().getGroundWeapon().getMinRangeInTiles();
			if (unit.distanceTo(enemy) < 1.5 + enemyRange) {
				return false;
			}
		}

		return true;
	}

	private static boolean shouldGoBackNearBunker(Unit unit) {
		Unit nearestBunker = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), unit);
		if (nearestBunker != null) {
			double distanceToBunker = nearestBunker.distanceTo(unit);
			if (distanceToBunker >= 5 && distanceToBunker <= 11) {
				Unit nearestTank = xvr.getNearestTankTo(unit);
				if (nearestTank == null || nearestTank.distanceTo(unit) >= 9) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isEnemyAirUnitNearbyThatCanShootGround(Unit unit) {
		for (Unit enemy : _enemyUnits) {
			UnitType type = enemy.getType();
			if (type.isFlyer()
					&& type.canGroundAttack()
					&& enemy.distanceTo(unit) <= enemy.getType().getGroundWeapon()
							.getMinRangeInTiles() + 2) {
				return true;
			}
		}

		return false;
	}

	// private static boolean isTooFarFromTank(Unit unit) {
	// Unit nearestTank = null;
	//
	// if (TerranSiegeTank.getNumberOfUnitsCompleted() == 0) {
	// return false;
	// }
	//
	// UnitType type = unit.getType();
	// boolean shouldBeAlwaysNearTank = !type.isFlyer() && !type.isWorker() &&
	// !type.isTank()
	// && !type.isVulture();
	// _distToNearestTank = -1;
	// if (shouldBeAlwaysNearTank) {
	// nearestTank = xvr.getNearestTankTo(unit);
	// if (nearestTank != null) {
	// _distToNearestTank = nearestTank.distanceTo(unit);
	// }
	//
	// if (_distToNearestTank > MAX_TILES_FROM_TANK) {
	// return true;
	// }
	// }
	// return false;
	// }

	private static boolean isCriticallyCloseToChokePoint(Unit unit) {
		ChokePoint nearestChoke = MapExploration.getNearestChokePointFor(unit);
		double distToNearestChoke = xvr.getDistanceBetween(nearestChoke, unit);
		if (distToNearestChoke > -0.1 && distToNearestChoke <= 9) {
			return true;
		}
		return false;
	}

	private static boolean isCriticalSituationVersusAirUnits(Unit unit) {
		int antiAirUnits = 0;
		for (Unit ourUnit : _ourUnits) {
			if (ourUnit.getType().canAirAttack()) {
				antiAirUnits++;
			}
		}

		return antiAirUnits == 0;
	}

	private static double calculateTotalAttackOf(ArrayList<Unit> units, boolean forEnemy) {
		int total = 0;
		int seconds = xvr.getTimeSeconds();
		int defensiveBuildings = 0;

		// int vultures = 0;
		// int dragoons = 0;
		for (Unit unit : units) {
			boolean isInfantry = unit.getType().isTerranInfantry();
			double attackValue = unit.getGroundAttackNormalized();
			UnitType type = unit.getType();
			if (type.isWorker()) {
				continue;
			}

			if (forEnemy && type.getGroundWeapon().getMaxRangeInTiles() >= 3) {
				attackValue *= ENEMY_RANGE_WEAPON_STRENGTH_BONUS;
			}

			// If we calculate attack for our units, decisively lower attack of
			// units that are retreating
			if (!forEnemy && isInfantry && unit.isRunningFromEnemy()) {
				attackValue /= 3;
			}

			if (forEnemy) {
				total += attackValue;

				if (type.isVulture()) {
					// vultures++;
					total -= attackValue * 1.4;
				}
				if (type.isHydralisk()) {
					total -= attackValue * 0.4;
				}
				if (type.isFirebat()) {
					total += attackValue * 0.4;
				}
				if (type.isDragoon()) {
					// dragoons++;
					total += attackValue * 0.4;
				}

				// Handle defensive buildings
				if (unit.isDefensiveGroundBuilding()) {
					total += DEFENSIVE_BUILDING_ATTACK_BONUS;
					if (unit.isCompleted()) {
						defensiveBuildings++;

						if (seconds >= 550) {
							if (type.isBunker()) {
								total += 40;
							}
						} else {
							total -= DEFENSIVE_BUILDING_ATTACK_BONUS;
						}
					}
					if (!TerranBarracks.LIMIT_MARINES && !type.isBunker() && seconds < 550) {
						total -= DEFENSIVE_BUILDING_ATTACK_BONUS;
						total -= attackValue * 0.7;
					} else {
						total -= DEFENSIVE_BUILDING_ATTACK_BONUS;
						total -= attackValue;
					}
				}

				// Carriers
				if (unit.getType().isInterceptor()) {
					total -= attackValue * 0.75;
				}
			} else {
				total += attackValue;
			}
		}

		if (defensiveBuildings >= 2 && _ourUnits.size() < 7 && XVR.isEnemyProtoss()) {
			StrategyManager.waitForMoreUnits();
			total = 99999;
		}
		if (defensiveBuildings > 0 && defensiveBuildings <= 7 && _ourUnits.size() >= 7) {
			if (!TerranBarracks.LIMIT_MARINES) {
				total /= 2;
			} else {
				total = 0;
			}
		}

		if (forEnemy) {
			if (StrategyManager.getMinBattleUnits() <= StrategyManager.INITIAL_MIN_UNITS
					&& defensiveBuildings >= 2 && TerranSiegeTank.getNumberOfUnitsCompleted() <= 1) {
				StrategyManager.waitForMoreUnits();
				// TerranBarracks.LIMIT_MARINES = true;
				// Debug.message(xvr, "Dont build zealots mode enabled");
			}

			_enemyDefensiveBuildings = defensiveBuildings;
		}

		return total;
	}

	private static double calculateHitPointsOf(ArrayList<Unit> units) {
		int total = 0;
		for (Unit unit : units) {
			UnitType type = unit.getType();

			if (unit.isCompleted() && (!type.isBuilding() || unit.isDefensiveGroundBuilding())) {
				total += unit.getHP() + unit.getShields();
				if (type.isMedic()) {
					total += 60;
				}
			}
		}
		return total;
	}

	private static ArrayList<Unit> getEnemiesNear(Unit ourUnit) {
		boolean isTank = ourUnit.getType().isTank();

		ArrayList<Unit> unitsInRadius = xvr.getUnitsInRadius(ourUnit, BATTLE_RADIUS_ENEMIES
				+ _rangeBonus, xvr.getEnemyArmyUnitsIncludingDefensiveBuildings());
		for (Iterator<Unit> iterator = unitsInRadius.iterator(); iterator.hasNext();) {
			Unit unit = (Unit) iterator.next();
			if (unit.getType().isBuilding()
					&& (!unit.isDefensiveGroundBuilding() || !unit.isCompleted() || (unit
							.isDefensiveGroundBuilding() && isTank))) {
				iterator.remove();
			}
		}
		return unitsInRadius;
	}

	private static ArrayList<Unit> getOurUnitsNear(Unit ourUnit) {
		ArrayList<Unit> unitsInRadius = xvr.getUnitsInRadius(ourUnit, BATTLE_RADIUS_ALLIES,
				xvr.getArmyUnitsIncludingDefensiveBuildings());

		for (Iterator<Unit> iterator = unitsInRadius.iterator(); iterator.hasNext();) {
			Unit unit = (Unit) iterator.next();
			if (unit.getHP() <= 10 || unit.getType().isSpiderMine()) {
				iterator.remove();
			} else if (unit.isDefensiveGroundBuilding()) {
				if (xvr.getDistanceBetween(unit, ourUnit) >= 3) {
					iterator.remove();
				}
			}
		}
		return unitsInRadius;
		// return xvr.getUnitsInRadius(ourUnit, BATTLE_RADIUS_ALLIES,
		// xvr.getArmyUnitsIncludingDefensiveBuildings());
	}

	// ==================================

	public static void checkIfBuildMoreAntiAirUnits() {
		if (!changePlanToBuildAntiAirUnits) {
			boolean changeOfPlans = false;

			if (countEnemyAirUnits() > 5) {
				Painter.message(xvr, "Start building Anti-Air units");
				changeOfPlans = true;
			}

			if (changeOfPlans) {
				changePlanToBuildAntiAirUnits = true;
				TerranBarracks.changePlanToAntiAir();
			}
		}
	}

	private static int countEnemyAirUnits() {
		int counter = 0;
		for (Unit enemy : MapExploration.getEnemyUnitsDiscovered()) {
			if (enemy.getType().isFlyer() && enemy.getAirWeaponCooldown() > 0) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 * Will this unit (and its companions nearby) win the fight with nearby
	 * enemies, with huge probability. This situation is typical and strong
	 * go-for-it, as it means we are decisively stronger than the enemy.
	 */
	public static boolean isStrengthRatioFavorableFor(Unit unit) {
		double strengthRatio = calculateStrengthRatioFor(unit);
		unit.setStrengthRatio(strengthRatio);
		StrategyManager.reduceSlightlyAllowedDistanceFromSafePoint();

		if (strengthRatio < 0) {
			return true;
		}
		return !(strengthRatio < FAVORABLE_RATIO_THRESHOLD);
	}

	/**
	 * Will this unit (and its companions nearby) lose the fight with nearby
	 * enemies almost certainly. This situation is a sure-loss.
	 */
	public static boolean isStrengthRatioCriticalFor(Unit unit) {
		double strengthRatio = calculateStrengthRatioFor(unit);
		unit.setStrengthRatio(strengthRatio);

		if (strengthRatio < 0) {
			return false;
		}
		return strengthRatio < CRITICAL_RATIO_THRESHOLD;
	}

	public static void recalculateFor(Unit unit) {
		unit.setStrengthRatio(calculateStrengthRatioFor(unit));
	}

}
package ai.terran;

import java.util.ArrayList;
import java.util.Collection;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.economy.TechnologyManager;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitImportantActions;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.utils.RUtilities;

public class TerranVulture {

	public static int CRITICALLY_FEW_VULTURES = 2;
	public static final int SAFE_DISTANCE_FROM_ENEMY_DEFENSIVE_BUILDING = 12;
	private static final double SAFE_DISTANCE_FROM_ENEMY = 3.85;
	private static final int MINIMUM_VULTURES_TO_DO_SCOUTING = 5;

	// =========================================================

	private static UnitTypes unitType = UnitTypes.Terran_Vulture;
	private static Unit explorerVulture = null;
	private static XVR xvr = XVR.getInstance();
	public static boolean DISABLE_VULTURES = false;

	// =========================================================

	public static boolean act(Unit unit) {
		if (!StrategyManager.isGlobalAttackInProgress()
				&& UnitImportantActions.tryAvoidingEnemyUnitsThatCanShoot(unit)) {
			return true;
		}

		// =========================================================

		if (unit.isBeingRepaired()) {
			UnitActions.holdPosition(unit);
			return true;
		}

		// =========================================================
		// Use mines if possible

		if (tryPlantingMines(unit)) {
			return true;
		}

		// =========================================================

		// If there's no organized fighting going on
		if (canActIndividually(unit)) {
			actIndividually(unit);
		}

		// =========================================================

		// int alliedUnitsNearby = xvr.countUnitsInRadius(unit, 10, true);
		// boolean shouldConsiderRunningAway =
		// !StrategyManager.isAnyAttackFormPending();

		// // =========================
		// // Look out for enemy defensive buildings.
		// if (ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(unit))
		// {
		// return true;
		// }
		//
		// // Avoid enemy tanks in Siege Mode
		// if (EnemyTanksManager.tryAvoidingEnemyTanks(unit)) {
		// return true;
		// }
		//
		// // Don't interrupt unit on march
		// if (unit.isStartingAttack()) {
		// return true;
		// }
		//
		// if (RunManager.runFromCloseOpponentsIfNecessary(unit,
		// SAFE_DISTANCE_FROM_ENEMY)) {
		// unit.setAiOrder("Run from enemy");
		// return true;
		// }
		//
		// // Disallow fighting when overwhelmed.
		// if (ArmyUnitBasicBehavior.tryRetreatingIfChancesNotFavorable(unit)) {
		// unit.setAiOrder("Would lose");
		// return true;
		// }

		// Scout bases near the enemy
		// if (handleExplorerVulture(unit)) {
		// return false;
		// }

		// ======== DEFINE NEXT MOVE =============================

		// If it's quite late in the game, we can start harrassing the enemy
		// if (xvr.getTimeSeconds() > 350) {
		// actOffensively(unit);
		// } else {
		// actDefensively(unit);
		// }

		// if (!StrengthEvaluator.isStrengthRatioFavorableFor(unit)) {
		// UnitActions.moveToSafePlace(unit);
		// }
		//
		// UnitActions.actWhenLowHitPointsOrShields(unit, false);

		return false;
	}

	// =========================================================

	// private static void actDefensively(Unit unit) {
	// MapPoint genericRendezvous =
	// ArmyRendezvousManager.getDefensivePoint(unit);
	// if (genericRendezvous != null && genericRendezvous.distanceTo(unit) >
	// 6.5) {
	// UnitActions.attackTo(unit, genericRendezvous);
	// unit.setAiOrder("Go entrench");
	// } else {
	// UnitActions.holdPosition(unit);
	// unit.setAiOrder("Entrench");
	// }
	// }
	//
	// private static void actOffensively(Unit unit) {
	// // if (!StrategyManager.isAnyAttackFormPending()) {
	// // actIndividually(unit);
	// // } else {
	// // FrontLineManager.actOffensively(unit,
	// // FrontLineManager.MODE_FRONT_GUARD);
	// // }
	//
	// // FrontLineManager.actOffensively(unit,
	// // FrontLineManager.MODE_FRONT_GUARD);
	// }

	private static void actIndividually(Unit unit) {
		MapPoint target = null;

		target = MapExploration.getNearestEnemyBase();
		if (target == null) {
			target = MapExploration.getNearestEnemyBuilding();
		}

		if (target != null && target.distanceTo(unit) > 3) {
			UnitActions.attackTo(unit, target);
		} else {
			MapPoint defensivePoint = ArmyRendezvousManager.getDefensivePoint(unit);
			UnitActions.attackTo(unit, defensivePoint);
		}
	}

	// private static void actIndividuallyOld(Unit unit) {
	//
	// // Get base locations near enemy, or buildings and try to go there.
	// MapPoint pointToHarass = defineNeighborhoodToHarass(unit);
	// if (pointToHarass != null) {
	//
	// ArrayList<MapPoint> pointForHarassmentNearEnemy = new ArrayList<>();
	// //
	// pointForHarassmentNearEnemy.addAll(MapExploration.getEnemyBasesDiscovered().values());
	// //
	// pointForHarassmentNearEnemy.addAll(MapExploration.getEnemyBuildingsDiscovered());
	// pointForHarassmentNearEnemy.addAll(MapExploration.getBaseLocationsNear(pointToHarass,
	// 37));
	// //
	// pointForHarassmentNearEnemy.addAll(MapExploration.getChokePointsNear(pointToHarass,
	// // 30));
	//
	// MapPoint goTo = null;
	// if (!pointForHarassmentNearEnemy.isEmpty()) {
	//
	// // Randomly choose one of them.
	// goTo = (MapPoint)
	// RUtilities.getRandomListElement(pointForHarassmentNearEnemy);
	// }
	//
	// else {
	// goTo = MapExploration.getNearestUnknownPointFor(unit.getX(), unit.getY(),
	// true);
	// if (goTo != null
	// && xvr.getBwapi().getMap()
	// .isConnected(unit, goTo.getX() / 32, goTo.getY() / 32)) {
	// }
	// }
	//
	// MapPoint safePoint = ArmyRendezvousManager.getDefensivePoint(unit);
	//
	// if (safePoint == null
	// || (safePoint.distanceTo(unit) < 25 && xvr.countUnitsInRadius(unit, 10,
	// true) > 0)) {
	// UnitActions.attackTo(unit, goTo);
	// unit.setAiOrder("Harass");
	// } else if (safePoint != null) {
	// UnitActions.attackTo(unit, safePoint);
	// unit.setAiOrder("Back");
	// }
	// } else {
	// UnitActions.spreadOutRandomly(unit);
	// unit.setAiOrder("Spread/harass");
	// }
	// }

	// =========================================================

	public static boolean canActIndividually(Unit unit) {
		return StrategyManager.CAN_VULTURES_ROAM && !StrategyManager.isGlobalAttackInProgress()
				&& unit.getHP() >= 40;
	}

	// =========================================================

	// =========================================================

	private static boolean handleExplorerVulture(Unit unit) {

		// Choose random Vulture to become Vulture Explorer
		if (explorerVulture == null) {
			if (UnitCounter.getNumberOfUnits(TerranVulture.getUnitType()) >= MINIMUM_VULTURES_TO_DO_SCOUTING) {
				explorerVulture = xvr.getUnitsOfType(TerranVulture.getUnitType()).iterator().next();
			}
		}

		if (explorerVulture != null && explorerVulture.equals(unit)) {
			if (unit.isIdle() && !unit.isAttacking() && !unit.isMoving()) {
				MapPoint point = getRandomBaseLocationNearEnemyMainBase();
				// System.out.println("BASE FOR VULTURE EXPLORER: " +
				// point.toStringLocation());
				if (point != null) {
					UnitActions.attackTo(explorerVulture, point);
					return true;
				}
			}
		}

		return false;
	}

	private static MapPoint getRandomBaseLocationNearEnemyMainBase() {
		if (!MapExploration.getEnemyBasesDiscovered().isEmpty()) {
			Unit enemyBase = MapExploration.getEnemyBasesDiscovered().values().iterator().next();
			return (MapPoint) RUtilities.getRandomElement(MapExploration.getBaseLocationsNear(
					enemyBase, 50));

		}

		return null;
	}

	// =========================================================

	private static boolean tryPlantingMines(Unit unit) {
		if (unit.getSpiderMineCount() > 0) {

			// Make sure mine will be safely far from our units
			boolean isSafePlaceForOurUnits = isSafelyFarFromOurUnits(unit);
			// boolean isSafelyFarFromBuildings =
			// isSafelyFarFromBuildings(unit);

			if (isSafePlaceForOurUnits) {
				boolean isPlaceInterestingChoiceForMine = isQuiteNearBunker(unit)
						|| isQuiteNearChokePoint(unit) || isQuiteNearEnemy(unit);
				boolean isNoEnemyNear = xvr.countUnitsEnemyInRadius(unit, 14) == 0;
				if (isNoEnemyNear && isPlaceInterestingChoiceForMine
						&& minesArentStackedTooMuchNear(unit)) {
					placeSpiderMine(unit, unit);
					return true;
				}

				boolean noMinesInRegion = isNoMinesInRegion(unit);
				boolean isFarFromMainbase = isFarFromMainbase(unit);
				if (noMinesInRegion && isFarFromMainbase) {
					placeSpiderMine(unit, unit);
					return true;
				}
			}
		}
		return false;
	}

	// =========================================================

	private static boolean isFarFromMainbase(MapPoint point) {
		return point.distanceTo(xvr.getFirstBase()) > 26;
	}

	private static boolean isNoMinesInRegion(Unit unit) {
		return xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Vulture_Spider_Mine, 2, unit,
				true) == 0
				&& xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Vulture_Spider_Mine, 8, unit,
						true) <= 1 || unit.getSpiderMineCount() == 3;
	}

	private static boolean isQuiteNearEnemy(Unit unit) {
		Unit nearEnemyUnitOrBuilding = xvr.getUnitNearestFromList(unit, xvr.getEnemyUnitsVisible());
		if (nearEnemyUnitOrBuilding != null) {
			if (nearEnemyUnitOrBuilding.distanceTo(unit) <= 11) {
				return true;
			}
		}
		return false;
	}

	private static boolean isQuiteNearBunker(Unit unit) {
		return xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Bunker, 18, unit, true) > 0;
	}

	private static boolean isQuiteNearChokePoint(Unit unit) {
		ChokePoint choke = MapExploration.getNearestChokePointFor(unit);
		return unit.distanceToChokePoint(choke) <= 1.05 || unit.getSpiderMineCount() == 3;
	}

	private static void placeSpiderMine(Unit vulture, MapPoint place) {
		UnitActions.useTech(vulture, TechnologyManager.SPIDER_MINES, place);
	}

	private static boolean minesArentStackedTooMuchNear(Unit unit) {
		return xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Vulture_Spider_Mine, 3, unit,
				true) <= 1;
	}

	private static boolean isSafelyFarFromOurUnits(Unit unit) {
		Unit nearestUnit = xvr.getUnitNearestFromList(unit, getUnitsInDangerOfMine(), true, false);
		boolean isSafelyFarFromUnits = true;
		if (nearestUnit != null) {

			double distanceToUnit = nearestUnit.distanceTo(unit);
			if (nearestUnit.getType().isBunker()) {
				if (distanceToUnit <= 6) {
					isSafelyFarFromUnits = false;
				}
			} else {
				if (distanceToUnit <= 7) {
					isSafelyFarFromUnits = false;
				}
			}
		}
		return isSafelyFarFromUnits;
	}

	private static Collection<Unit> getUnitsInDangerOfMine() {
		ArrayList<Unit> unitsList = new ArrayList<>();
		for (Unit unit : xvr.getBwapi().getMyUnits()) {
			UnitType type = unit.getType();
			if (!type.isSpiderMine() && !type.isVulture()) {
				unitsList.add(unit);
			}
		}
		return unitsList;
	}

	private static MapPoint defineNeighborhoodToHarass(Unit unit) {

		// Try to get random base
		MapPoint pointToHarass = MapExploration.getRandomKnownEnemyBase();

		// If we don't know any base, get random building
		if (pointToHarass == null) {
			pointToHarass = MapExploration.getNearestEnemyBuilding();
		}

		// If still nothing...
		if (pointToHarass == null) {
			// pointToHarass = MapExploration.getRandomChokePoint();
		}

		return pointToHarass;
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(unitType);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(unitType);
	}

	public static UnitTypes getUnitType() {
		return unitType;
	}
}

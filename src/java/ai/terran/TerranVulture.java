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
import ai.managers.units.army.RunManager;
import ai.managers.units.coordination.ArmyUnitBasicBehavior;
import ai.utils.RUtilities;

public class TerranVulture {

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Vulture;

	private static final double SAFE_DISTANCE_FROM_ENEMY = 3.45;

	// =========================================================

	public static boolean act(Unit unit) {
		// int alliedUnitsNearby = xvr.countUnitsInRadius(unit, 10, true);
		// boolean shouldConsiderRunningAway =
		// !StrategyManager.isAnyAttackFormPending();

		// =========================
		// Look out for enemy defensive buildings.
		if (ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(unit)) {
			return true;
		}

		// Don't interrupt unit on march
		if (unit.isStartingAttack()) {
			return true;
		}

		if (RunManager.runFromCloseOpponentsIfNecessary(unit, SAFE_DISTANCE_FROM_ENEMY)) {
			unit.setAiOrder("Run from enemy");
			return true;
		}

		// Disallow fighting when overwhelmed.
		if (ArmyUnitBasicBehavior.tryRetreatingIfChancesNotFavorable(unit)) {
			unit.setAiOrder("Would lose");
			return true;
		}

		// ======== DEFINE NEXT MOVE =============================

		// Get base locations near enemy, or buildings and try to go there.
		MapPoint pointToHarass = defineNeighborhoodToHarass(unit);

		ArrayList<MapPoint> pointForHarassmentNearEnemy = new ArrayList<>();
		pointForHarassmentNearEnemy.addAll(MapExploration.getEnemyBasesDiscovered().values());
		pointForHarassmentNearEnemy.addAll(MapExploration.getEnemyBuildingsDiscovered());
		pointForHarassmentNearEnemy.addAll(MapExploration.getBaseLocationsNear(pointToHarass, 30));
		// pointForHarassmentNearEnemy.addAll(MapExploration.getChokePointsNear(pointToHarass,
		// 30));

		MapPoint goTo = null;
		if (!pointForHarassmentNearEnemy.isEmpty()) {

			// Randomly choose one of them.
			goTo = (MapPoint) RUtilities.getRandomListElement(pointForHarassmentNearEnemy);
		}

		else {
			goTo = MapExploration.getNearestUnknownPointFor(unit.getX(), unit.getY(), true);
			if (goTo != null
					&& xvr.getBwapi().getMap()
							.isConnected(unit, goTo.getX() / 32, goTo.getY() / 32)) {
			}
		}

		unit.setAiOrder("Harass the enemy");

		// Attack this randomly chosen base location.
		UnitActions.attackTo(unit, goTo);

		// =================================
		// Use mines if possible
		if (tryPlantingMines(unit)) {
			return true;
		}

		// if (!StrengthEvaluator.isStrengthRatioFavorableFor(unit)) {
		// UnitActions.moveToSafePlace(unit);
		// }
		//
		// UnitActions.actWhenLowHitPointsOrShields(unit, false);

		return false;
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
				if (isPlaceInterestingChoiceForMine && minesArentStackedTooMuchNear(unit)
						|| noMinesInRegion(unit)) {
					placeSpiderMine(unit, unit);
					return true;
				}
			}
		}
		return false;
	}

	private static boolean noMinesInRegion(Unit unit) {
		return xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Vulture_Spider_Mine, 6, unit,
				true) <= 1 || unit.getSpiderMineCount() == 3;
	}

	// =========================================================

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
			pointToHarass = MapExploration.getRandomChokePoint();
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

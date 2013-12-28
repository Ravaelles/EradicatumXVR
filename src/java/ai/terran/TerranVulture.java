package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.army.StrengthEvaluator;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.StrategyManager;
import ai.managers.TechnologyManager;
import ai.utils.RUtilities;

public class TerranVulture {

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Vulture;

	public static UnitTypes getUnitType() {
		return unitType;
	}

	public static void act(Unit unit) {
		// int alliedUnitsNearby = xvr.countUnitsInRadius(unit, 10, true);
		boolean shouldConsiderRunningAway = !StrategyManager.isAnyAttackFormPending();

		// =========================

		if (shouldConsiderRunningAway
				&& UnitActions.runFromEnemyDetectorOrDefensiveBuildingIfNecessary(unit, true, true,
						false)) {
			return;
		}

		// Don't interrupt unit on march
		if (unit.isStartingAttack()) {
			return;
		}

		if (xvr.isEnemyDefensiveGroundBuildingNear(unit)) {
			UnitActions.moveToSafePlace(unit);
			return;
		}

		// ======== DEFINE NEXT MOVE =============================

		// Get 3 base locations near enemy, or buildings and try to go there.
		MapPoint pointToHarass = defineNeighborhoodToHarass(unit);

		ArrayList<MapPoint> pointForHarassmentNearEnemy = new ArrayList<>();
		pointForHarassmentNearEnemy.addAll(MapExploration.getBaseLocationsNear(pointToHarass, 30));
		pointForHarassmentNearEnemy.addAll(MapExploration.getChokePointsNear(pointToHarass, 30));

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

		// Attack this randomly chosen base location.
		UnitActions.attackTo(unit, goTo.getX(), goTo.getY());

		// =================================
		// Use mines if possible
		handleMines(unit);

		if (unit.getGroundWeaponCooldown() > 0
				&& !StrengthEvaluator.isStrengthRatioFavorableFor(unit)) {
			UnitActions.moveToSafePlace(unit);
		}

		UnitActions.actWhenLowHitPointsOrShields(unit, false);
	}

	private static void handleMines(Unit unit) {
		if (unit.getSpiderMineCount() > 0) {

			// Make sure mine will be safely far from our buildings
			boolean isSafelyFarFromBuildings = isSafelyFarFromBuildings(unit);

			if (isQuiteNearChokePoint(unit) && isSafelyFarFromBuildings
					&& minesArentStackedTooMuchNear(unit)) {
				placeSpiderMine(unit, unit);
			}
		}
	}

	private static boolean isQuiteNearChokePoint(Unit unit) {
		ChokePoint choke = MapExploration.getNearestChokePointFor(unit);
		return unit.distanceToChokePoint(choke) <= 3 || unit.getSpiderMineCount() == 3;
	}

	private static void placeSpiderMine(Unit vulture, MapPoint place) {
		UnitActions.useTech(vulture, TechnologyManager.SPIDER_MINES, place);
	}

	private static boolean minesArentStackedTooMuchNear(Unit unit) {
		return xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Vulture_Spider_Mine, 2.3, unit,
				true) == 0;
	}

	private static boolean isSafelyFarFromBuildings(Unit unit) {
		Unit nearestBuilding = xvr.getUnitNearestFromList(unit, xvr.getUnitsBuildings(), true,
				false);
		boolean isSafelyFarFromBuilding = true;
		if (nearestBuilding != null) {

			double distanceToBuilding = nearestBuilding.distanceTo(unit);
			if (nearestBuilding.getType().isBunker()) {
				if (distanceToBuilding <= 11) {
					isSafelyFarFromBuilding = false;
				}
			} else {
				if (distanceToBuilding <= 17) {
					isSafelyFarFromBuilding = false;
				}
			}
		}
		return isSafelyFarFromBuilding;
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

}

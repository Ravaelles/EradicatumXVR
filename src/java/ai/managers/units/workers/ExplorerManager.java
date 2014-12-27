package ai.managers.units.workers;

import java.util.ArrayList;
import java.util.Collection;

import jnibwapi.model.BaseLocation;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitActions;
import ai.managers.units.coordination.ArmyUnitBasicBehavior;
import ai.terran.TerranCommandCenter;
import ai.utils.RUtilities;

public class ExplorerManager {

	private static Unit explorer;
	private static XVR xvr = XVR.getInstance();

	public static Unit getExplorer() {
		return explorer;
	}

	private static boolean _exploredSecondBase = false;
	private static boolean _exploredBackOfMainBase = false;
	private static boolean _isDiscoveringEnemyBase = false;

	// =========================================================

	public static void explore(Unit explorer) {
		// ExplorerCirclingEnemyBase.circleAroundEnemyBaseWith(explorer,
		// xvr.getFirstBase());
		//
		// if (true) {
		// return;
		// }

		if (!explorer.isCompleted()) {
			return;
		}
		ExplorerManager.explorer = explorer;

		// Disallow units to move close to the defensive building like
		// Photon Cannon
		if (ArmyUnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(explorer)) {
			explorer.setAiOrder("Avoid building");
			return;
		}

		if (tryAvoidGettingKilled(explorer)) {
			return;
		}

		// If explorer is marked to be discovering enemy base, but he's
		// attacked, then unmark him from his task, thus allowing him to run.
		if (_isDiscoveringEnemyBase && explorer.isUnderAttack() && explorer.getHP() < 24) {
			if (xvr.getEnemyBuildings().size() > 0) {
				_isDiscoveringEnemyBase = false;
			}
		}

		boolean isWounded = isExplorerWounded();
		double distToEnemy = getDistanceToNearestEnemy();

		// ===========================
		// Don't interfere if explorer is building or attacking etc.
		boolean shouldBeConstructing = explorer.isConstructing();
		boolean shouldContinueAttacking = explorer.isAttacking() && !isWounded;
		boolean shouldBeDiscovering = _isDiscoveringEnemyBase || !_exploredSecondBase
				|| MapExploration.getEnemyBuildingsDiscovered().isEmpty();
		boolean isEnemyClose = distToEnemy > 0 && distToEnemy < 3;
		boolean shouldBeMoving = explorer.isMoving() && isEnemyClose;

		// Try running around enemy base
		if (ExplorerCirclingEnemyBase.tryRunningAroundEnemyBaseIfPossible()) {
			return;
		}

		if (!explorer.isIdle()
				&& (shouldBeDiscovering || shouldBeMoving || shouldBeConstructing || shouldContinueAttacking)) {
			return;
		}

		// ===========================
		// If unit is WOUNDED, then retreat to the main base
		if (tryRunningFromEnemiesIfNecessary()) {
			explorer.setAiOrder("Run from enemies");
			return;
		}

		// ===========================
		// Explorer ACTIONS

		// If we need to scout next base location (Nexus construction can screw
		// it)
		if (tryScoutingNextBaseLocation()) {
			return;
		}

		// Discover base location of the enemy
		if (tryDiscoveringEnemyBaseLocation()) {
			explorer.setAiOrder("Discover enemy");
			return;
		}

		// Always when possible, try to trololo the enemy
		if (tryAttackingEnemyIfPossible()) {
			explorer.setAiOrder("Harass the enemy");
			return;
		}

		// Gather minerals if idle
		gatherResourcesIfIdle();
	}

	// =========================================================

	private static boolean tryAvoidGettingKilled(Unit unit) {
		boolean isInDanger = false;

		// We can get killed if there are at least two enemy units near
		int nearEnemyUnits = xvr.countUnitsEnemyInRadius(unit, 1.5);
		// unit.setAiOrder("TEST: " + nearEnemyUnits + " / " +
		// xvr.getTimeSeconds());
		if (unit.isWounded() && (nearEnemyUnits >= 2 || nearEnemyUnits == 1)) {
			unit.setAiOrder("Run from enemies (" + nearEnemyUnits + ")");
			isInDanger = true;
		}

		// We can get killed if there's even one enemy army unit near
		if (!isInDanger && xvr.countUnitsInRadius(unit, 1.7, xvr.getEnemyArmyUnits()) >= 1) {
			// System.out.println("              DANGER !!!!!!!!!");
			unit.setAiOrder("Avoid army unit");
			isInDanger = true;
		}

		// =========================================================

		if (isInDanger) {
			UnitActions.moveAwayFromNearestEnemy(unit);
			return true;
		} else {
			return false;
		}
	}

	// =========================================================

	private static void gatherResourcesIfIdle() {
		if (explorer.isIdle()) {
			explorer.setAiOrder("Gather resources");
			WorkerManager.gatherResources(explorer, xvr.getFirstBase());
		}
	}

	private static double getDistanceToNearestEnemy() {
		Unit nearestEnemy = xvr.getUnitNearestFromList(explorer.getX(), explorer.getY(),
				xvr.getEnemyUnitsVisible(), true, true);
		return explorer.distanceTo(nearestEnemy);
	}

	private static boolean isExplorerWounded() {
		return explorer.getHP() < 25;
	}

	private static boolean tryRunningFromEnemiesIfNecessary() {
		boolean isWounded = isExplorerWounded();

		// Never run if not even wounded.
		if (!isWounded) {
			return false;
		}

		Unit nearestEnemy = xvr.getUnitNearestFromList(explorer.getX(), explorer.getY(),
				xvr.getEnemyUnitsVisible(), true, true);
		double distToNearestEnemy = explorer.distanceTo(nearestEnemy);

		boolean isUnitUnderAttack = explorer.isUnderAttack();
		boolean isEnemyArmyUnitClose = isEnemyArmyUnitCloseToExplorer();
		boolean isEnemyCloseAndUnitIsWounded = distToNearestEnemy > 0 && distToNearestEnemy <= 5
				&& isWounded;
		boolean isOverwhelmed = xvr.getEnemyUnitsInRadius(6, explorer).size() >= 2
				&& xvr.getEnemyUnitsInRadius(3, explorer).size() >= 1;

		// System.out.println("#### " + explorer.getID());
		// System.out.println(isEnemyArmyUnitClose);
		// System.out.println(isEnemyCloseAndUnitIsWounded);
		// System.out.println(isAttackingAndIsOverwhelmed);

		if (isOverwhelmed || isUnitUnderAttack || isEnemyArmyUnitClose
				|| isEnemyCloseAndUnitIsWounded) {
			double distToMainBase = explorer.distanceTo(xvr.getFirstBase());

			// If already at base, run to the most distant base
			if (distToMainBase < 13) {
				UnitActions.moveTo(explorer, MapExploration.getMostDistantBaseLocation(explorer));
				return true;
			}

			// Not yet at base, still go there.
			else {
				UnitActions.moveToMainBase(explorer);
				return true;
			}
		}

		return false;
	}

	private static boolean isEnemyArmyUnitCloseToExplorer() {
		Unit nearestArmyUnit = xvr.getUnitNearestFromList(explorer, xvr.getEnemyArmyUnits(), true,
				true);
		if (nearestArmyUnit == null) {
			return false;
		} else {
			double dist = nearestArmyUnit.distanceTo(explorer);
			return dist < 12 && dist > 0 && !nearestArmyUnit.isWorker();
		}
	}

	public static boolean tryAttackingEnemyIfPossible() {
		Unit enemyUnit = null;

		// Disallow heavily wounded explorer to atttack.
		if (explorer.getHP() < 19) {
			return false;
		}

		// Disallow explorer to attack if he's circling enemy base.
		if (ExplorerCirclingEnemyBase.isCirclingOrHasCircled()) {
			return false;
		}

		// if (XVR.isEnemyProtoss()) {
		// Collection<Unit> pylons =
		// xvr.getEnemyUnitsOfType(ProtossPylon.getBuildingType());
		// if (!pylons.isEmpty()) {
		// nearestEnemyBuilding = (Unit) RUtilities.getRandomElement(pylons);
		// }
		// } else if (XVR.isEnemyZerg()) {
		// Collection<Unit> pools =
		// xvr.getEnemyUnitsOfType(UnitTypes.Zerg_Spawning_Pool);
		// if (!pools.isEmpty()) {
		// nearestEnemyBuilding = (Unit) RUtilities.getRandomElement(pools);
		// }
		// } else if (XVR.isEnemyTerran()) {
		// nearestEnemyBuilding = xvr.getEnemyWorkerInRadius(300, explorer);
		// }

		// Enemy is TERRAN
		if (xvr.isEnemyTerran()) {
			enemyUnit = xvr.getEnemyWorkerConstructingInRadius(20, explorer);
		}
		if (enemyUnit == null) {
			enemyUnit = getClosestEnemyWorker(explorer);
		}

		if (enemyUnit != null && enemyUnit.distanceTo(xvr.getFirstBase()) < 20) {
			enemyUnit = null;
		}

		// boolean isWounded = isExplorerWounded();
		boolean isWounded = isExplorerWounded();
		boolean isProperTargetSelected = enemyUnit != null;
		boolean isNeighborhoodQuiteSafe = (xvr.getEnemyUnitsInRadius(3, explorer).size() == 0 && xvr
				.getEnemyUnitsInRadius(6, explorer).size() <= 1);
		boolean hasFullLife = explorer.getHP() >= 59;
		boolean isVeryAlive = explorer.getHP() >= 40;
		boolean isOverwhelmed = xvr.getEnemyWorkersInRadius(6, explorer).size() >= 2;

		if ((!isOverwhelmed || isVeryAlive)
				&& isProperTargetSelected
				&& ((!isWounded && isNeighborhoodQuiteSafe) || (hasFullLife && isNeighborhoodQuiteSafe))) {
			// Debug.message(xvr, "Explorer attacks UNIT");
			UnitActions.attackEnemyUnit(explorer, enemyUnit);
			return true;
		}

		// If there's no unit to attack, try to attack a building
		if (enemyUnit == null && (!isWounded && isNeighborhoodQuiteSafe)) {
			enemyUnit = xvr.getEnemyUnitOfType(UnitTypes.Protoss_Pylon,
					UnitTypes.Terran_Supply_Depot, UnitTypes.Zerg_Spawning_Pool);
			// Debug.message(xvr, "---> building?");

			if (enemyUnit == null) {
				enemyUnit = xvr.getUnitNearestFromList(explorer, xvr.getEnemyBuildings(), true,
						false);
			}
		}

		if (enemyUnit != null) {
			// Debug.message(xvr, "Explorer attacks BUILDING");
			UnitActions.attackEnemyUnit(explorer, enemyUnit);
			return true;
		}

		return false;
	}

	private static Unit getClosestEnemyWorker(MapPoint point) {
		Collection<Unit> enemyWorkers = xvr.getEnemyWorkersInRadius(300, explorer);
		if (enemyWorkers != null && !enemyWorkers.isEmpty()) {
			return xvr.getUnitNearestFromList(point, enemyWorkers);
		} else {
			return null;
		}
	}

	private static boolean tryDiscoveringEnemyBaseLocation() {
		boolean hasDiscoveredBaseLocation = !MapExploration.getEnemyBuildingsDiscovered().isEmpty();
		if (!hasDiscoveredBaseLocation) {
			BaseLocation goTo = null;

			// Filter out visited bases.
			ArrayList<BaseLocation> possibleBases = new ArrayList<BaseLocation>();
			possibleBases.addAll(xvr.getBwapi().getMap().getStartLocations());
			possibleBases.removeAll(MapExploration.getBaseLocationsDiscovered());
			possibleBases.remove(MapExploration.getOurBaseLocation());

			// If there is any unvisited base- go there. If no- go to the random
			// base.
			if (possibleBases.isEmpty()) {
				goTo = (BaseLocation) RUtilities.getRandomListElement(xvr.getBwapi().getMap()
						.getStartLocations());
			} else {
				goTo = (BaseLocation) RUtilities.getRandomListElement(possibleBases);
			}

			if (goTo != null) {
				_isDiscoveringEnemyBase = true;
				UnitActions.moveTo(explorer, goTo);
				MapExploration.getBaseLocationsDiscovered().add(goTo);
				return true;
			}
		}
		return false;
	}

	private static Unit _explorerForBackOfBase = null;

	private static boolean tryScoutingNextBaseLocation() {

		// Explore place behind our minerals
		if (!_exploredBackOfMainBase) {
			explorer.setAiOrder("Explore back of base");
			MapPoint backOfTheBasePoint = scoutBackOfMainBase();
			if (backOfTheBasePoint != null && _explorerForBackOfBase == null) {
				_explorerForBackOfBase = xvr.getOptimalBuilder(backOfTheBasePoint);
				if (xvr.getDistanceBetween(_explorerForBackOfBase, backOfTheBasePoint) <= 30) {
					UnitActions.moveTo(_explorerForBackOfBase, backOfTheBasePoint);
				}
			}
			if (backOfTheBasePoint == null) {
				_exploredBackOfMainBase = true;
			}
			if (_explorerForBackOfBase == null
					|| _explorerForBackOfBase.distanceTo(backOfTheBasePoint) <= 1.5) {
				_exploredBackOfMainBase = true;
				MapPoint nearBaseLoc = MapExploration
						.getNearestBaseLocation(_explorerForBackOfBase);
				if (xvr.getDistanceBetween(_explorerForBackOfBase, nearBaseLoc) <= 30) {
					UnitActions.moveTo(_explorerForBackOfBase, nearBaseLoc);
				}
			}
			// return true;
		}

		// Explore the place where the second base will be built
		if (!_exploredSecondBase) {
			explorer.setAiOrder("Explore second base");
			MapPoint secondBase = TerranCommandCenter.getSecondBaseLocation();
			UnitActions.moveTo(explorer, secondBase);
			_exploredSecondBase = true;
			return true;
		}

		// Explore random base location
		if (!explorer.isMoving() && RUtilities.rand(0, 1) == 0) {
			explorer.setAiOrder("Scout random base");
			scoutRandomBaseLocation();
		}

		// Explore place for the 3rd and later bases
		if ((explorer.isGatheringGas() || explorer.isGatheringMinerals())
				&& TerranCommandCenter.shouldBuild()) {
			MapPoint nextBase = TerranCommandCenter.findTileForNextBase(false);
			if (nextBase != null && !xvr.getBwapi().isVisible(nextBase)) {
				explorer.setAiOrder("Explore base");
				UnitActions.moveTo(explorer, nextBase.translate(-1, -1));
			}
		}

		// // Explore place for the 3rd and later bases
		// if (UnitCounter.getNumberOfUnits(UnitManager.BASE) >= 2) {
		// MapPoint tileForNextBase =
		// TerranCommandCenter.findTileForNextBase(false);
		// if (!xvr.getBwapi().isVisible(tileForNextBase.getTx(),
		// tileForNextBase.getTy())) {
		// explorer.setAiOrder("Scout 3rd base");
		// UnitActions.moveTo(explorer, tileForNextBase);
		// return true;
		// } else {
		// if (!explorer.isMoving()) {
		// explorer.setAiOrder("Explore unknown");
		// UnitActions.moveTo(
		// explorer,
		// MapExploration.getNearestUnknownPointFor(explorer.getX(),
		// explorer.getY(), true));
		// return true;
		// }
		// }
		// }

		return false;
	}

	private static void scoutRandomBaseLocation() {
		BaseLocation base = null;

		boolean isOkay = false;
		while (!isOkay) {
			base = (BaseLocation) RUtilities.getRandomListElement(xvr.getMap().getBaseLocations());
			if (!TerranCommandCenter.existsBaseNear(base)) {
				isOkay = true;
			}
		}

		UnitActions.moveTo(explorer, base);
	}

	private static MapPoint scoutBackOfMainBase() {

		// Calculate average x and y of minerals
		int x = 0;
		int y = 0;
		int counter = 0;
		for (Unit mineral : TerranCommandCenter.getMineralsNearBase(xvr.getFirstBase())) {
			x += mineral.getX();
			y += mineral.getY();
			counter++;
		}

		if (counter == 0) {
			return null;
		}

		x /= counter;
		y /= counter;
		MapPoint backOfTheBase = new MapPointInstance(x, y);

		// System.out.println("BASE SCOUTING:");
		// System.out.println("BASE: " + xvr.getFirstBase().toStringLocation());
		// System.out.println("GARDEN: " + backOfTheBase.toStringLocation());
		// System.out.println(xvr.getFirstBase().toStringLocation());
		// System.out.println();

		for (int i = 15; i >= 1; i -= 2) {
			MapPoint pointToGo = UnitActions.moveInDirectionOfPointIfPossible(explorer,
					backOfTheBase, i);

			if (pointToGo != null && xvr.getMap().isConnected(explorer, pointToGo)) {
				// System.out.println("success: " + pointToGo.toStringLocation()
				// + " / " + i);
				return pointToGo;
			}
		}

		return null;
	}
}

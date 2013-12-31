package ai.handling.map;

import java.util.ArrayList;

import jnibwapi.model.BaseLocation;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.units.UnitActions;
import ai.handling.units.UnitCounter;
import ai.managers.WorkerManager;
import ai.managers.units.UnitBasicBehavior;
import ai.managers.units.UnitManager;
import ai.terran.TerranCommandCenter;
import ai.utils.RUtilities;

public class Explorer {

	private static Unit explorer;
	private static XVR xvr = XVR.getInstance();

	public static Unit getExplorer() {
		return explorer;
	}

	private static boolean _exploredSecondBase = false;
	private static boolean _exploredBackOfMainBase = false;
	private static boolean _isDiscoveringEnemyBase = false;

	public static void explore(Unit explorer) {
		if (!explorer.isCompleted()) {
			return;
		}
		Explorer.explorer = explorer;

		// Disallow units to move close to the defensive building like
		// Photon Cannon
		if (UnitBasicBehavior.tryRunningFromCloseDefensiveBuilding(explorer)) {
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
				|| MapExploration.enemyBuildingsDiscovered.isEmpty();
		boolean isEnemyClose = distToEnemy > 0 && distToEnemy < 3;
		boolean shouldBeMoving = explorer.isMoving() && isEnemyClose;
		if (!explorer.isIdle()
				&& (shouldBeDiscovering || shouldBeMoving || shouldBeConstructing || shouldContinueAttacking)) {
			return;
		}

		// ===========================
		// If unit is WOUNDED, then retreat to the main base
		if (tryRunningFromEnemiesIfNecessary()) {
			return;
		}

		// ===========================
		// Explorer ACTIONS

		// If we need to scout next base location (Nexus construction can screw
		// otherwise), do it.
		if (tryScoutingNextBaseLocation()) {
			return;
		}

		// Discover base location of the enemy
		if (tryDiscoveringEnemyBaseLocation()) {
			return;
		}

		// Always when possible, try to trololo the enemy
		if (tryAttackingEnemyIfPossible()) {
			return;
		}

		// Gather minerals if idle
		gatherResourcesIfIdle();
	}

	private static void gatherResourcesIfIdle() {
		if (explorer.isIdle()) {
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

	private static boolean tryAttackingEnemyIfPossible() {
		Unit enemyUnit = null;

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

		enemyUnit = xvr.getEnemyWorkerInRadius(300, explorer);
		if (XVR.isEnemyTerran()) {
			enemyUnit = xvr.getEnemyWorkerConstructingInRadius(300, explorer);
			if (enemyUnit == null) {
				enemyUnit = xvr.getEnemyWorkerInRadius(300, explorer);
			}
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

		// boolean isProperTargetSelected = nearestEnemyBuilding != null;
		// boolean isNormalBuilding = isProperTargetSelected
		// && (!nearestEnemyBuilding.isDefensiveGroundBuilding() ||
		// !nearestEnemyBuilding
		// .isCompleted());
		// boolean isNeighborhoodQuiteSafe = (xvr.getEnemyUnitsInRadius(3,
		// explorer).size() == 0 && xvr
		// .getEnemyUnitsInRadius(6, explorer).size() <= 1);
		// boolean isUnitVeryMuchAlive = explorer.getShields() >= 19;
		//
		// if (isProperTargetSelected && isNormalBuilding
		// && (isNeighborhoodQuiteSafe || isUnitVeryMuchAlive)) {
		// UnitActions.attackTo(explorer, nearestEnemyBuilding);
		// return true;
		// }
		//
		// // System.out.println(isNormalBuilding + " / " +
		// isNeighborhoodQuiteSafe
		// // + " / "
		// // + isUnitVeryMuchAlive);
		//
		// if (explorer.isIdle() || (!_isDiscoveringEnemyBase &&
		// nearestEnemyBuilding == null)) {
		// Unit nearestEnemyUnit = xvr.getUnitNearestFromList(explorer,
		// xvr.getBwapi()
		// .getEnemyUnits());
		// if (nearestEnemyUnit != null && xvr.getEnemyUnitsInRadius(6,
		// explorer).size() <= 1) {
		// UnitActions.attackTo(explorer, nearestEnemyUnit);
		// return true;
		// }
		// }

		return false;
	}

	private static boolean tryDiscoveringEnemyBaseLocation() {
		boolean hasDiscoveredBaseLocation = !MapExploration.enemyBuildingsDiscovered.isEmpty();
		if (!hasDiscoveredBaseLocation) {
			BaseLocation goTo = null;

			// Filter out visited bases.
			ArrayList<BaseLocation> possibleBases = new ArrayList<BaseLocation>();
			possibleBases.addAll(xvr.getBwapi().getMap().getStartLocations());
			possibleBases.removeAll(MapExploration.baseLocationsDiscovered);
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
				MapExploration.baseLocationsDiscovered.add(goTo);
				return true;
			}
		}
		return false;
	}

	private static Unit _explorerForBackOfBase = null;

	private static boolean tryScoutingNextBaseLocation() {

		// Explore place behind our minerals
		if (!_exploredBackOfMainBase) {
			MapPoint backOfTheBasePoint = scoutBackOfMainBase();
			if (backOfTheBasePoint != null && _explorerForBackOfBase == null) {
				_explorerForBackOfBase = TerranCommandCenter.getMineralWorkersNearBase(
						xvr.getFirstBase()).get(0);
				UnitActions.moveTo(_explorerForBackOfBase, backOfTheBasePoint);
			}
			if (backOfTheBasePoint == null) {
				_exploredBackOfMainBase = true;
			}
			if (_explorerForBackOfBase == null
					|| _explorerForBackOfBase.distanceTo(backOfTheBasePoint) <= 1.5) {
				_exploredBackOfMainBase = true;
				MapPoint nearBaseLoc = MapExploration
						.getNearestBaseLocation(_explorerForBackOfBase);
				UnitActions.moveTo(_explorerForBackOfBase, nearBaseLoc);
				// System.out.println("_exploredBackOfMainBase");
			}
			// return true;
		}

		// Explore the place where the second base will be built
		if (!_exploredSecondBase) {
			MapPoint secondBase = TerranCommandCenter.getSecondBaseLocation();
			UnitActions.moveTo(explorer, secondBase);
			_exploredSecondBase = true;
			return true;
		}

		// Explore random base location
		if (!explorer.isMoving() && RUtilities.rand(0, 1) == 0) {
			scoutRandomBaseLocation();
		}

		// Explore place for the 3rd and later bases
		if (UnitCounter.getNumberOfUnits(UnitManager.BASE) >= 2) {
			MapPoint tileForNextBase = TerranCommandCenter.findTileForNextBase(false);
			if (!xvr.getBwapi().isVisible(tileForNextBase.getTx(), tileForNextBase.getTy())) {
				UnitActions.moveTo(explorer, tileForNextBase);
				return true;
			} else {
				if (!explorer.isMoving()) {
					UnitActions.moveTo(
							explorer,
							MapExploration.getNearestUnknownPointFor(explorer.getX(),
									explorer.getY(), true));
					return true;
				}
			}
		}

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

package ai.handling.units;

import java.util.ArrayList;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.TechType.TechTypes;
import jnibwapi.types.UnitType;
import ai.core.XVR;
import ai.handling.army.ArmyPlacing;
import ai.handling.army.StrengthRatio;
import ai.handling.army.TargetHandling;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.managers.StrategyManager;
import ai.terran.TerranBunker;
import ai.utils.RUtilities;

public class UnitActions {

	private static XVR xvr = XVR.getInstance();

	public static void loadUnitInto(Unit unit, Unit loadTo) {
		if (unit != null && loadTo != null) {
			XVR.getInstance().getBwapi().load(unit.getID(), loadTo.getID());
		}
	}

	public static void moveTo(Unit unit, Unit destination) {
		if (unit == null || destination == null) {
			// System.err.println("moveTo # unit: " + unit + " # destination: "
			// + destination);
			return;
		}
		moveTo(unit, destination.getX(), destination.getY());
	}

	public static void moveTo(Unit unit, MapPoint point) {
		if (point == null) {
			return;
		}
		moveTo(unit, point.getX(), point.getY());
	}

	public static void moveTo(Unit unit, int x, int y) {
		XVR.getInstance().getBwapi().move(unit.getID(), x, y);
	}

	public static void attackTo(Unit ourUnit, MapPoint point) {
		if (ourUnit == null || point == null) {
			return;
		}
		attackTo(ourUnit, point.getX(), point.getY());
	}

	public static void attackTo(Unit ourUnit, int x, int y) {
		if (ourUnit != null) {
			xvr.getBwapi().attack(ourUnit.getID(), x, y);
		}
	}

	public static void attackEnemyUnit(Unit ourUnit, Unit enemy) {
		if (ourUnit != null && enemy != null && enemy.isDetected()) {
			xvr.getBwapi().attack(ourUnit.getID(), enemy.getID());
		}
	}

	public static void repair(Unit worker, Unit building) {
		if (worker != null && building != null) {
			xvr.getBwapi().repair(worker.getID(), building.getID());
		}
	}

	public static void callForHelp(Unit toRescue, boolean critical) {
		CallForHelp.issueCallForHelp(toRescue, critical);
	}

	public static void goToRandomChokePoint(Unit unit) {
		ChokePoint goTo = MapExploration.getRandomChokePoint();
		UnitActions.moveTo(unit, goTo.getCenterX(), goTo.getCenterY());
	}

	public static boolean moveAwayFromUnitIfPossible(Unit unit, MapPoint placeToMoveAwayFrom,
			int howManyTiles) {
		if (unit == null || placeToMoveAwayFrom == null) {
			return false;
		}

		int xDirectionToUnit = placeToMoveAwayFrom.getX() - unit.getX();
		int yDirectionToUnit = placeToMoveAwayFrom.getY() - unit.getY();

		double vectorLength = xvr.getDistanceBetween(placeToMoveAwayFrom, unit);
		double ratio = 32 * howManyTiles / vectorLength;

		MapPoint runTo = new MapPointInstance((int) (unit.getX() - ratio * xDirectionToUnit),
				(int) (unit.getY() - ratio * yDirectionToUnit));

		if (runTo.isWalkable() && runTo.isConnectedTo(unit)) {
			moveTo(unit, runTo);
			// UnitActions.moveToSafePlace(unit);
			return true;
		} else {
			return false;
		}
	}

	public static MapPoint moveInDirectionOfPointIfPossible(Unit unit,
			MapPoint pointThatMakesDirection, int howManyTiles) {
		if (unit == null || pointThatMakesDirection == null) {
			return null;
		}

		int xDirectionToUnit = pointThatMakesDirection.getX() - unit.getX();
		int yDirectionToUnit = pointThatMakesDirection.getY() - unit.getY();

		double vectorLength = xvr.getDistanceBetween(pointThatMakesDirection, unit);
		double ratio = 32 * howManyTiles / vectorLength;

		MapPointInstance targetPoint = new MapPointInstance((int) (unit.getX() + ratio
				* xDirectionToUnit), (int) (unit.getY() + ratio * yDirectionToUnit));
		if (xvr.getMap().isLowResWalkable(targetPoint)) {
			moveTo(unit, targetPoint);
			return targetPoint;
		} else {
			return null;
		}
	}

	public static boolean shouldSpreadOut(Unit unit) {
		return unit.isIdle() && !unit.isMoving() && !unit.isAttacking() && !unit.isUnderAttack();
	}

	public static void spreadOutRandomly(Unit unit) {
		if (!StrengthRatio.isStrengthRatioFavorableFor(unit)) {
			UnitActions.moveToSafePlace(unit);
			return;
		}

		// Act when enemy detector is nearby, run away
		if (!StrategyManager.isAttackPending()
				&& (xvr.isEnemyDetectorNear(unit.getX(), unit.getY()) || xvr
						.isEnemyDefensiveGroundBuildingNear(unit))) {
			Unit goTo = xvr.getLastBase();
			UnitActions.attackTo(unit, goTo.getX(), goTo.getY());
			return;
		}

		// WORKER: Act when enemy is nearby, run away
		Unit enemyNearby = TargetHandling.getEnemyNearby(unit, 8);
		if (enemyNearby != null && unit.isWorker() && unit.getHP() > 18) {
			Unit goTo = xvr.getFirstBase();
			UnitActions.attackTo(unit, goTo.getX(), goTo.getY());
			return;
		}

		// Look if there's really important unit nearby
		boolean groundAttackCapable = unit.canAttackGroundUnits();
		boolean airAttackCapable = unit.canAttackAirUnits();
		Unit importantEnemyUnit = TargetHandling.getImportantEnemyUnitTargetIfPossibleFor(unit,
				groundAttackCapable, airAttackCapable);
		if (importantEnemyUnit != null && importantEnemyUnit.isDetected()) {
			Unit goTo = importantEnemyUnit;
			UnitActions.attackTo(unit, goTo.getX(), goTo.getY());
		}

		// System.out.println("###### SPREAD OUT ########");
		if (!unit.isMoving() && !unit.isUnderAttack() && unit.getHP() > 18) {

			// If distance to current target is smaller than N it means that
			// unit can spread out and scout nearby grounds
			// if (xvr.getDistanceBetween(unit, unit.getTargetX(),
			// unit.getTargetY()) < 38) {
			MapPoint goTo = MapExploration
					.getNearestUnknownPointFor(unit.getX(), unit.getY(), true);
			if (goTo != null
					&& xvr.getBwapi().getMap()
							.isConnected(unit, goTo.getX() / 32, goTo.getY() / 32)) {
				UnitActions.attackTo(unit, goTo.getX(), goTo.getY());
			} else {
				UnitActions.attackTo(unit, unit.getX() + 1000 - RUtilities.rand(0, 2000),
						unit.getY() + 1000 - RUtilities.rand(0, 2000));
			}
			// }
		}

		if (!StrengthRatio.isStrengthRatioFavorableFor(unit)) {
			UnitActions.moveToSafePlace(unit);
		}
	}

	public static void moveToMainBase(Unit unit) {
		Unit firstBase = xvr.getFirstBase();
		if (unit.isWorker()) {
			if (xvr.getDistanceSimple(unit, firstBase) < 8) {
				moveTo(unit, MapExploration.getRandomChokePoint());
			} else {
				moveTo(unit, firstBase);
			}
		} else {
			moveTo(unit, firstBase);
		}
	}

	public static boolean runFromEnemyDetectorOrDefensiveBuildingIfNecessary(Unit unit,
			boolean tryAvoidingDetectors, boolean allowAttackingDetectorsIfSafe, boolean isAirUnit) {
		final int RUN_DISTANCE = 6;

		// If we should avoid detectors, look for one nearby.
		if (tryAvoidingDetectors) {
			boolean isEnemyDetectorNear = xvr.isEnemyDetectorNear(unit);

			// Okay, we know there's detector near.
			if (isEnemyDetectorNear) {
				boolean canDetectorShootAtThisUnit = false;
				boolean isAttackingDetectorSafe = false;

				// If unit can possibly attack detector safely, even if it's
				// supposed
				// to avoid them, define if this detector can shoot at our unit.
				if (allowAttackingDetectorsIfSafe) {
					Unit enemyDetector = xvr.getEnemyDetectorNear(unit);
					if (isAirUnit) {
						canDetectorShootAtThisUnit = enemyDetector.canAttackAirUnits();
					} else {
						canDetectorShootAtThisUnit = enemyDetector.canAttackGroundUnits();
					}

					// If detector cannot shoot at this unit, but is surrounded
					// by some enemies then do not attack
					if (!canDetectorShootAtThisUnit) {
						ArrayList<Unit> enemyUnitsNearDetector = xvr.getUnitsInRadius(
								enemyDetector, 9, xvr.getEnemyArmyUnits());
						if (enemyUnitsNearDetector.size() <= 1) {
							isAttackingDetectorSafe = true;
						}
					}
				}

				// If unit isn't allowed to attack detectors OR if the detector
				// cannot be attack safely, then run away from it.
				if (tryAvoidingDetectors
						&& (!allowAttackingDetectorsIfSafe || !isAttackingDetectorSafe)) {

					// Try to move away from this enemy detector on N tiles.
					UnitActions.moveAwayFromUnitIfPossible(unit,
							xvr.getEnemyDetectorNear(unit.getX(), unit.getY()), RUN_DISTANCE);
					return true;
				}
			}
		}

		boolean isEnemyBuildingNear = isAirUnit ? xvr.isEnemyDefensiveAirBuildingNear(unit) : xvr
				.isEnemyDefensiveGroundBuildingNear(unit);
		if (isEnemyBuildingNear) {
			Unit enemyBuilding = isAirUnit ? xvr.getEnemyDefensiveAirBuildingNear(unit.getX(),
					unit.getY()) : xvr
					.getEnemyDefensiveGroundBuildingNear(unit.getX(), unit.getY());
			UnitActions.moveAwayFromUnitIfPossible(unit, enemyBuilding, RUN_DISTANCE);
			return true;
		}

		return false;
	}

	public static boolean actWhenLowHitPointsOrShields(Unit unit, boolean isImportantUnit) {
		UnitType type = unit.getType();
		Unit goTo = null;

		// if (xvr.getTimeSeconds() < 340
		// && (UnitCounter.getNumberOfUnits(TerranBunker.getBuildingType()) < 2
		// || !XVR
		// .isEnemyProtoss())) {
		// return;
		// }
		//
		// if (xvr.getTimeSeconds() < 300
		// && UnitCounter.getNumberOfUnits(TerranBunker.getBuildingType()) == 0)
		// {
		// return;
		// }

		int currHP = unit.getHP();
		int maxHP = type.getMaxHitPoints();

		// If there's massive attack and unit has more than 60% of initial
		// shields, we treat it as healthy, as there's nothing to do about it.
		if (StrategyManager.isAttackPending()) {
			if (!isImportantUnit && currHP >= 0.6 * maxHP) {
				return false;
			}
		}

		// // Unit has almost all shields
		// if (currHP >= maxHP / 2) {
		// return;
		// }

		// // =============================
		// // Disallow running from some critical units
		//
		// // If there's BUNKER
		// if (xvr.getUnitsOfGivenTypeInRadius(UnitTypes.Terran_Bunker, 3, unit,
		// false).size() > 0) {
		// return;
		// }
		//
		// // If there's CANNON
		// if (xvr.getUnitsOfGivenTypeInRadius(TerranBunker.getBuildingType(),
		// 3, unit, false).size() > 0) {
		// return;
		// }
		//
		// // If there's enemy ARCHON
		// if (xvr.getUnitsInRadius(unit, 3,
		// xvr.getEnemyUnitsOfType(UnitTypes.Protoss_Archon)).size() > 0) {
		// return;
		// }
		//
		// // If there's SUKEN COLONY
		// if (xvr.getUnitsOfGivenTypeInRadius(UnitTypes.Zerg_Sunken_Colony, 3,
		// unit, false).size() > 0) {
		// return;
		// }

		// //
		// =====================================================================
		// // If unit is close to base then run away only if critically wounded.
		// if (xvr.countUnitsOfGivenTypeInRadius(UnitManager.BASE, 13, unit,
		// true) >= 1) {
		// if (unit.getHP() > (type.getMaxHitPoints() / 3 + 3)) {
		// return;
		// }
		// }

		// =====================================================================

		// Then try to go to cannon nearest to the last base, if exists.
		if (goTo == null) {
			goTo = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), xvr.getLastBase());
		}

		// If not, go to the first base.
		if (goTo == null) {
			goTo = xvr.getFirstBase();
		}

		if (goTo != null) {
			if (xvr.getDistanceBetween(unit, goTo) >= 5.5) {
				UnitActions.moveTo(unit, goTo);
				// UnitActions.attackTo(unit, goTo);
				return true;
			}
		}

		return false;
	}

	public static void rightClick(Unit unit, Unit clickTo) {
		if (unit == null || clickTo == null) {
			System.err.println("rightClick # unit: " + unit + " # clickTo: " + clickTo);
			return;
		}
		xvr.getBwapi().rightClick(unit.getID(), clickTo.getID());
	}

	public static void useTech(Unit wizard, TechTypes tech, Unit useOn) {
		xvr.getBwapi().useTech(wizard.getID(), tech.getID(), useOn.getID());
	}

	public static void useTech(Unit wizard, TechTypes tech, MapPoint place) {
		xvr.getBwapi().useTech(wizard.getID(), tech.getID(), place.getX(), place.getY());
	}

	public static void useTech(Unit wizard, TechTypes tech) {
		xvr.getBwapi().useTech(wizard.getID(), tech.getID());
	}

	public static void moveToSafePlace(Unit unit) {
		ArmyPlacing.goToSafePlaceIfNotAlreadyThere(unit);
	}

	public static void repairThisUnit(Unit unit) {
		if (unit.getType().isVulture()) {
			return;
		}

		Unit repairer = xvr.getOptimalBuilder(unit);
		if (unit == null || repairer == null) {
			return;
		}
		repair(repairer, unit);
	}

	public static void holdPosition(Unit unit) {
		xvr.getBwapi().holdPosition(unit.getID());
	}

	public static void moveAwayFromNearestEnemy(Unit unit) {
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		moveAwayFromUnit(unit, nearestEnemy);
	}

	public static void moveAwayFromUnit(Unit unit, Unit enemy) {
		if (enemy == null || unit == null) {
			return;
		}

		boolean unitHasMovedItsAss = false;

		MapPoint safePoint = ArmyPlacing.getSafePointFor(unit);
		double unitToSafePointDist = unit.distanceTo(safePoint);
		double enemyToSafePointDist = enemy.distanceTo(safePoint);

		boolean shouldRunFromUnitNotGoToSafePlace = unitToSafePointDist > enemyToSafePointDist
				|| unitToSafePointDist <= 6;

		// Try to move away from unit and if can't (e.g. a wall
		// behind), try to increase tiles away from current location
		if (shouldRunFromUnitNotGoToSafePlace) {
			for (int i = 7; i >= 3; i -= 2) {
				if (UnitActions.moveAwayFromUnitIfPossible(unit, enemy, i)) {
					unitHasMovedItsAss = true;
					break;
				}
			}
		}

		// If unit still didn't move (e.g. nowhere to go, a dead-end),
		// then just go back to the safe place.
		if (!unitHasMovedItsAss) {
			unitHasMovedItsAss = true;
			UnitActions.moveToSafePlace(unit);
		}
	}

}

package ai.managers.units.coordination;

import java.util.ArrayList;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranSiegeTank;

public class ArmyRendezvousManager {

	private static XVR xvr = XVR.getInstance();
	private static MapPoint _armyMedianPoint = null;

	// =========================================================

	public static void act(Unit unit) {
		goToSafePlaceIfNotAlreadyThere(unit);
	}

	/**
	 * @param unit
	 * @return safe point
	 */
	public static MapPoint goToSafePlaceIfNotAlreadyThere(Unit unit) {
		MapPoint safePlace = null;

		if (unit.shouldFollowTanks()) {
			if (TerranSiegeTank.getNumberOfUnitsCompleted() >= 2) {
				// safePlace = TerranSiegeTank.getMedianTank();
				safePlace = getRendezvousTankForGroundUnits();
			}
		}

		if (safePlace == null) {
			safePlace = getDefensivePoint(unit);
			if (safePlace == null) {
				return null;
			}
		}

		if (xvr.getDistanceSimple(unit, safePlace) >= 4.2) {
			UnitActions.moveTo(unit, safePlace);
			return safePlace;
			// } else {
			// UnitActions.moveAwayFromNearestEnemy(unit);
			// UnitActions.moveTo(unit, safePlace);
		}
		// else {
		// UnitActions.moveToMainBase(unit);
		// }

		return null;
	}

	// =========================================================

	public static void updateRendezvousPoints() {
		_armyMedianPoint = defineArmyMedianPoint();
	}

	// =========================================================

	/**
	 * 
	 * @return Target for offensive operations e.g. closest known enemy
	 *         building, or the expected base location if no building is known.
	 */
	public static MapPoint getOffensivePoint() {

		// Try to target nearest enemy building that isn't destroyed
		Unit nearestEnemyBuilding = MapExploration.getNearestEnemyBuilding();
		if (nearestEnemyBuilding != null && nearestEnemyBuilding.isExists()
				&& !nearestEnemyBuilding.isOnGeyser()) {
			return nearestEnemyBuilding;
		}

		// We don't know any exact building location, just try to target the
		// base
		else {
			Unit nearestEnemyBase = MapExploration.getNearestEnemyBase();
			if (nearestEnemyBase != null) {
				return nearestEnemyBase;
			} else {
				return null;
			}
		}
	}

	public static MapPoint getDefensivePoint(Unit unit) {
		MapPoint secondBaseLocation = TerranCommandCenter.getSecondBaseLocation();

		// Initially, go to the second base location
		MapPoint runTo = secondBaseLocation;

		// =====================================================
		// Define bunker where most of the units should head to.
		runTo = defineRendezvousBunkerIfPossible();

		// =====================================================
		// Try to go to the barracks if no bunker available
		if (runTo == null) {
			runTo = defineRendezvousBarracksIfPossible();
		}

		// =====================================================
		// If is infantry, try go to nearest medic with energy
		if (unit != null && unit.getType().isTerranInfantry() && unit.isWounded()) {
			Unit medicThatCanHealUs = defineRendezvousMedicIfPossible(unit);
			if (medicThatCanHealUs != null) {
				runTo = medicThatCanHealUs;
			}
		}

		// =====================================================
		// Tell the unit where to be
		if (unit != null) {
			unit.setProperPlaceToBe(runTo);
		}

		if (runTo == null) {
			return null;
		} else {
			return new MapPointInstance(runTo.getX(), runTo.getY()).translate(-4, 0);
		}
	}

	public static Unit getRendezvousTankForFlyers() {
		return TerranSiegeTank.getFrontTank();
	}

	public static MapPoint getRendezvousTankForGroundUnits() {
		return TerranSiegeTank.getFrontTank();
	}

	public static MapPoint getDefensivePointForTanks() {
		if (StrategyManager.isGlobalAttackActive()) {
			return getArmyMedianPoint();
		} else {
			if (TerranBunker.getNumberOfUnitsCompleted() > 0) {
				return defineRendezvousBunkerIfPossible();
			} else {
				return getArmyMedianPoint();
			}
		}
	}

	// =========================================================

	private static Unit defineRendezvousMedicIfPossible(Unit unit) {
		for (Unit medic : xvr.getUnitsOfType(UnitTypes.Terran_Medic)) {
			if (!medic.isWounded() && medic.getEnergy() > 70) {
				return medic;
			}
		}
		return null;
	}

	private static MapPoint defineArmyMedianPoint() {
		ArrayList<Unit> unitsArmy = xvr.getUnitsArmy();
		if (unitsArmy.isEmpty()) {
			return null;
		}

		ArrayList<Integer> xCoordinates = new ArrayList<Integer>();
		ArrayList<Integer> yCoordinates = new ArrayList<Integer>();
		for (Unit armyUnit : unitsArmy) {
			if (armyUnit.isSpiderMine() || !armyUnit.isCompleted()) {
				continue;
			}
			xCoordinates.add(armyUnit.getX());
			yCoordinates.add(armyUnit.getY());
		}
		java.util.Collections.sort(xCoordinates);
		java.util.Collections.sort(yCoordinates);

		int middleIndex = xCoordinates.size() / 2;

		if (xCoordinates.isEmpty()) {
			return null;
		}

		MapPointInstance medianPoint = new MapPointInstance(xCoordinates.get(middleIndex),
				yCoordinates.get(middleIndex));

		return xvr.getUnitNearestFromList(medianPoint, xvr.getUnitsArmy());
	}

	// =========================================================

	private static MapPoint defineRendezvousBarracksIfPossible() {
		Unit barracks = xvr.getUnitOfTypeNearestTo(TerranBarracks.getBuildingType(),
				TerranCommandCenter.getSecondBaseLocation());
		if (barracks != null) {
			return barracks;
		} else {
			return null;
		}
	}

	private static MapPoint defineRendezvousBunkerIfPossible() {
		if (TerranBunker.getNumberOfUnits() == 0) {
			return null;
		}

		Unit nearestEnemyBuilding = MapExploration.getNearestEnemyBuilding();
		MapPoint bunkersNearestTo = nearestEnemyBuilding != null ? nearestEnemyBuilding
				: TerranCommandCenter.getSecondBaseLocation();

		// Get the list of bunkers that are near to the specified point.
		ArrayList<Unit> bunkersNearby = xvr.getUnitsInRadius(bunkersNearestTo, 300,
				xvr.getUnitsOfType(UnitTypes.Terran_Bunker));
		for (Unit bunker : bunkersNearby) {
			return bunker.translate(40, 40);
		}
		return null;

		// MapPoint bunkerNearThisPoint = MapExploration.getNearestEnemyBase();
		// if (bunkerNearThisPoint == null) {
		// bunkerNearThisPoint = TerranCommandCenter.getSecondBaseLocation();
		// }
		//
		// Unit bunker =
		// xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(),
		// bunkerNearThisPoint);
		// if (bunker != null) {
		// return bunker;
		// } else {
		// return null;
		// }
	}

	public static MapPoint getArmyCenterPoint() {
		int totalX = 0;
		int totalY = 0;
		int counter = 0;
		for (Unit unit : xvr.getUnitsNonWorker()) {
			totalX += unit.getX();
			totalY += unit.getY();
			counter++;
			if (counter > 10) {
				break;
			}
		}
		return new MapPointInstance((int) ((double) totalX / counter),
				(int) ((double) totalY / counter));
	}

	public static MapPoint getArmyMedianPoint() {
		return _armyMedianPoint;
	}

	public static MapPoint getFlyersGatheringPoint() {
		MapPoint attackPoint = null;
		if (TerranSiegeTank.getNumberOfUnitsCompleted() > 0) {
			attackPoint = MapExploration.getNearestEnemyBuilding();
			if (attackPoint == null) {
				attackPoint = MapExploration.getMostDistantBaseLocation(xvr.getFirstBase());
			}
			// return xvr.getUnitNearestFromList(attackPoint,
			// TerranSiegeTank.getAllCompletedTanks());
			return xvr.getUnitNearestFromList(attackPoint, xvr.getUnitsArmy());
		} else {
			return TerranCommandCenter.getSecondBaseLocation();
		}
	}

}

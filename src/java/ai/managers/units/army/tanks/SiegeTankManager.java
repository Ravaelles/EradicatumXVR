package ai.managers.units.army.tanks;

import java.util.HashMap;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.TankTargeting;
import ai.handling.units.UnitActions;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitManager;
import ai.managers.units.coordination.ArmyRendezvousManager;
import ai.terran.TerranBunker;
import ai.terran.TerranSiegeTank;
import ai.utils.RUtilities;

public class SiegeTankManager {

	private static final int MIN_TIME_TO_UNSIEGE = 20;

	private static XVR xvr = XVR.getInstance();

	private static UnitTypes unitType = UnitTypes.Terran_Siege_Tank_Tank_Mode;

	// =========================================================

	static class TargettingDetails {
		// static boolean _isUnitWhereItShouldBe;
		// static MapPoint _properPlace;
		static Unit _nearestEnemy;
		static Unit _nearestEnemyBuilding;
		static double _nearestEnemyDist;

		/**
		 * If a tank is in this map it means it is considering unsieging, but we
		 * should wait some time before this happens.
		 */
		static HashMap<Unit, Integer> unsiegeIdeasMap = new HashMap<>();
	}

	// =========================================================

	public static void act(Unit unit) {
		// TargettingDetails._properPlace = unit.getProperPlaceToBe();
		// updateProperPlaceToBeForTank(unit);
		// TargettingDetails._isUnitWhereItShouldBe =
		// TargettingDetails._properPlace == null
		// || TargettingDetails._properPlace.distanceTo(unit) <= 3;
		TargettingDetails._nearestEnemy = xvr.getNearestGroundEnemy(unit);
		TargettingDetails._nearestEnemyBuilding = MapExploration.getNearestEnemyBuilding(unit);
		TargettingDetails._nearestEnemyDist = TargettingDetails._nearestEnemy != null ? TargettingDetails._nearestEnemy
				.distanceTo(unit) : -1;

		if (unit.isSieged()) {
			actWhenSieged(unit);
		} else {
			actWhenInNormalMode(unit);
		}
	}

	// =========================================================

	private static void actWhenInNormalMode(Unit unit) {

		// =========================================================
		// Check if should siege

		if (canSiegeInThisPlace(unit)) {
			if (shouldSiege(unit) && notTooManySiegedUnitHere(unit) && didntJustUnsiege(unit)) {
				unit.siege();
				return;
			}

			if (mustSiege(unit)) {
				unit.siege();
				return;
			}

			int enemiesVeryClose = xvr.countUnitsEnemyInRadius(unit, 4);
			int enemiesInSight = xvr.countUnitsEnemyInRadius(unit, 13);

			if (enemiesInSight >= 2 && enemiesVeryClose == 0) {
				unit.siege();
				return;
			}
		}

		// =========================================================
		// Define hi-level behaviour

		// if (TerranSiegeTank.getNumberOfUnitsCompleted() >= 7) {
		// actOffensively(unit);
		// } else {
		// actDefensively(unit);
		// }
	}

	// =========================================================

	/**
	 * Preferably, try going to the bunker in order to entrench there.
	 * 
	 * @param unit
	 */
	// private static void actDefensively(Unit unit) {
	// MapPoint rendezvousPointForTanks =
	// ArmyRendezvousManager.getDefensivePointForTanks();
	// if (unit.distanceTo(rendezvousPointForTanks) > 5) {
	// unit.setAiOrder("Entrench");
	// UnitActions.attackTo(unit, rendezvousPointForTanks);
	// }
	// }
	//
	// private static void actOffensively(Unit unit) {
	// FrontLineManager.actOffensively(unit, FrontLineManager.MODE_VANGUARD);
	//
	// if (!StrategyManager.FORCE_CRAZY_ATTACK &&
	// tryGoingBackToMedianTankIfNeeded(unit)) {
	// return;
	// }
	// }

	// =========================================================

	private static void actWhenSieged(Unit unit) {
		unit.setLastTimeSieged(xvr.getTimeSeconds());

		if (unit.getGroundWeaponCooldown() > 0) {
			return;
		}

		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		double nearestEnemyDist = nearestEnemy != null ? nearestEnemy.distanceTo(unit) : -1;

		boolean enemyVeryClose = nearestEnemyDist >= 0 && nearestEnemyDist <= 2.5;
		int enemiesVeryClose = xvr.countUnitsEnemyInRadius(unit, 4);

		boolean enemyAlmostInSight = nearestEnemyDist >= 0 && nearestEnemyDist <= 14;
		boolean neighborhoodDangerous = enemyVeryClose && unit.getStrengthRatio() < 1.8;
		// boolean chancesRatherBad = unit.getStrengthRatio() < 1.1;
		if (neighborhoodDangerous && enemyVeryClose && enemiesVeryClose >= 2) {
			unit.setAiOrder("Unsiege: Urgent");
			unit.unsiege();
			return;
		}

		// If tank from various reasons shouldn't be here, unsiege.
		if (!enemyAlmostInSight && !unit.isStartingAttack() && !shouldSiege(unit)
				&& !mustSiege(unit)) {
			infoTankIsConsideringUnsieging(unit);
		}

		// If tank should be here, try to attack proper target.
		else {
			MapPoint targetForTank = TankTargeting.defineTargetForSiegeTank(unit);
			if (targetForTank != null) {
				if (targetForTank instanceof Unit && ((Unit) targetForTank).isDetected()) {
					UnitActions.attackEnemyUnit(unit, (Unit) targetForTank);
				} else {
					UnitActions.attackTo(unit, targetForTank);
				}
			} else {
				if (nearestEnemy != null && !enemyAlmostInSight) {
					if (nearestEnemyDist >= 0 && (nearestEnemyDist < 2 || nearestEnemyDist <= 7)) {
						infoTankIsConsideringUnsieging(unit);
					}
				}
			}
		}

		if (isUnsiegingIdeaTimerExpired(unit)) {
			unit.setAiOrder("Unsiege: OK");
			if (!mustSiege(unit) && !shouldSiege(unit)) {
				unit.unsiege();
				return;
			}
		}

		// =========================================================
		// Check if should unsiege because of pending attack
		if (shouldUnsiegeBecauseOfPendingAttack(unit) && !mustSiege(unit)) {
			unit.unsiege();
			return;
		}
	}

	private static boolean shouldUnsiegeBecauseOfPendingAttack(Unit unit) {
		return unit.isSieged() && StrategyManager.isGlobalAttackActive()
				&& unit.distanceTo(ArmyRendezvousManager.getDefensivePoint(unit)) < 7;
	}

	private static boolean tryGoingBackToMedianTankIfNeeded(Unit unit) {
		if (TerranSiegeTank.getNumberOfUnitsCompleted() <= 1) {
			return false;
		}

		// Define tank that is "in the center"
		Unit medianTank = TerranSiegeTank.getMedianTank();

		// If unit is too far from "median" tank, go back to it.
		if (medianTank != null && medianTank.distanceTo(unit) > 7) {
			UnitActions.attackTo(
					unit,
					medianTank.translate(-64 + RUtilities.rand(0, 128),
							-64 + RUtilities.rand(0, 128)));

			return true;
		}

		// Didn't doo anything, return false
		return false;
	}

	// =========================================================

	// private static void updateProperPlaceToBeForTank(Unit unit) {
	// Unit nearestArmyUnit = xvr.getUnitNearestFromList(unit,
	// xvr.getUnitsArmyNonTanks(), true,
	// false);
	// if (nearestArmyUnit != null && !nearestArmyUnit.isLoaded()) {
	// TargettingDetails._properPlace = nearestArmyUnit;
	// }
	//
	// if (StrategyManager.isAnyAttackFormPending()) {
	// TargettingDetails._properPlace = StrategyManager.getTargetPoint();
	// }
	// }

	// private static void updateProperPlaceToBeForTank(Unit unit) {
	// Unit nearestArmyUnit = xvr.getUnitNearestFromList(unit,
	// xvr.getUnitsArmyNonTanks(), true,
	// false);
	// if (nearestArmyUnit != null && !nearestArmyUnit.isLoaded()) {
	// TargettingDetails._properPlace = nearestArmyUnit;
	// }
	//
	// if (StrategyManager.isAnyAttackFormPending()) {
	// TargettingDetails._properPlace = StrategyManager.getTargetPoint();
	// }
	// }

	private static boolean shouldSiege(Unit unit) {
		if (!canSiegeInThisPlace(unit)) {
			return false;
		}

		boolean isEnemyNearShootRange = (TargettingDetails._nearestEnemyDist > 0 && TargettingDetails._nearestEnemyDist <= (TargettingDetails._nearestEnemy
				.getType().isBuilding() ? 10.6 : 13));

		// Check if should siege, based on unit proper place to be (e.g. near
		// the bunker), but consider the neighborhood, if it's safe etc.
		if (isTankWhereItShouldBe(unit) && notTooManySiegedInArea(unit) || isEnemyNearShootRange) {
			if (canSiegeInThisPlace(unit) && isNeighborhoodSafeToSiege(unit)
					&& (!isNearMainBase(unit) || isNearBunker(unit))) {
				return true;
			}
		}

		// If there's an enemy in the range of shoot and there are some other
		// units around this tank, then siege.
		int oursNearby = xvr.countUnitsOursInRadius(unit, 7);
		if (isEnemyNearShootRange && oursNearby >= 5) {
			return true;
		}

		// If there's enemy building in range, siege.
		if (TargettingDetails._nearestEnemyBuilding != null
				&& TargettingDetails._nearestEnemyBuilding.distanceTo(unit) <= 10.5
				&& oursNearby >= 2) {
			return true;
		}

		return false;
	}

	private static boolean mustSiege(Unit unit) {
		if (!canSiegeInThisPlace(unit)) {
			return false;
		}

		// If there's enemy building in range, siege.
		if (TargettingDetails._nearestEnemyBuilding != null
				&& TargettingDetails._nearestEnemyBuilding.distanceTo(unit) <= 10.1) {
			return true;
		}

		return false;
	}

	// =========================================================

	private static boolean didntJustUnsiege(Unit unit) {
		return unit.getLastTimeSieged() + 5 <= xvr.getTimeSeconds();
	}

	private static boolean isUnsiegingIdeaTimerExpired(Unit unit) {
		if (TargettingDetails.unsiegeIdeasMap.containsKey(unit)) {
			if (xvr.getTimeSeconds() - TargettingDetails.unsiegeIdeasMap.get(unit) >= MIN_TIME_TO_UNSIEGE) {
				return true;
			}
		}
		return false;
	}

	private static void infoTankIsConsideringUnsieging(Unit unit) {
		if (!TargettingDetails.unsiegeIdeasMap.containsKey(unit)) {
			unit.setAiOrder("Consider unsieging");
			TargettingDetails.unsiegeIdeasMap.put(unit, xvr.getTimeSeconds());
		}
	}

	private static boolean isNearMainBase(Unit unit) {
		return unit.distanceTo(xvr.getFirstBase()) > 28;
	}

	private static boolean isNearBunker(Unit unit) {
		Unit nearestBunker = xvr.getUnitOfTypeNearestTo(TerranBunker.getBuildingType(), unit);
		return nearestBunker != null && nearestBunker.distanceTo(unit) < 4.4;
	}

	private static boolean isTankWhereItShouldBe(Unit unit) {
		MapPoint rendezvous = ArmyRendezvousManager.getDefensivePointForTanks();
		if (rendezvous != null) {
			return unit.distanceTo(rendezvous) < 4.7;
		} else {
			return true;
		}
	}

	private static boolean notTooManySiegedInArea(Unit unit) {
		// return xvr.countUnitsOfGivenTypeInRadius(type, tileRadius, point,
		// onlyMyUnits);
		return true;
	}

	private static boolean notTooManySiegedUnitHere(Unit unit) {
		Unit nearBuilding = xvr.getUnitNearestFromList(unit, xvr.getUnitsBuildings());
		boolean isNearBuilding = nearBuilding != null && nearBuilding.distanceTo(unit) <= 7;

		ChokePoint nearChoke = MapExploration.getNearestChokePointFor(unit);
		boolean isNearChoke = nearChoke != null && unit.distanceToChokePoint(nearChoke) <= 3;

		if (isNearBuilding || isNearChoke) {
			return xvr.countUnitsOfGivenTypeInRadius(UnitTypes.Terran_Siege_Tank_Siege_Mode, 2.7,
					unit, true) <= 2;
		} else {
			return false;
		}
	}

	private static boolean isNeighborhoodSafeToSiege(Unit unit) {
		if (xvr.countUnitsEnemyInRadius(unit, 4) <= 0) {
			return true;
		}
		return false;
	}

	private static boolean canSiegeInThisPlace(Unit unit) {
		ChokePoint nearestChoke = MapExploration.getNearestChokePointFor(unit);

		// Don't siege in the choke point near base, or you'll... lose.
		if (nearestChoke.getRadiusInTiles() <= 3.8 && unit.distanceToChokePoint(nearestChoke) <= 4) {
			Unit nearestBase = xvr.getUnitOfTypeNearestTo(UnitManager.BASE, unit);
			if (nearestBase != null && nearestBase.distanceTo(unit) <= 20) {
				return false;
			}
		}

		return true;
	}

	public static UnitTypes getUnitType() {
		return unitType;
	}

}

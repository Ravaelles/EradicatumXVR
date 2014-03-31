package ai.managers.units.coordination;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.WeaponType;
import ai.core.XVR;
import ai.handling.strength.StrengthRatio;
import ai.handling.units.CallForHelp;
import ai.handling.units.UnitActions;
import ai.managers.economy.TechnologyManager;
import ai.managers.units.UnitManager;
import ai.managers.units.army.tanks.SiegeTankManager;
import ai.managers.units.workers.RepairAndSons;
import ai.terran.TerranBunker;
import ai.terran.TerranMedic;
import ai.terran.TerranVulture;
import ai.utils.RUtilities;

public class ArmyUnitBasicBehavior {

	private static XVR xvr = XVR.getInstance();

	// =============================================

	public static void act(Unit unit) {
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
			if (tryConsideringCallForHelpMission(unit)) {
				UnitManager.actWhenOnCallForHelpMission(unit);
			} else {
				ArmyRendezvousManager.act(unit);
			}
		}

		// ======================================
		// SPECIFIC ACTIONS for units, but DON'T FULLY OVERRIDE standard
		// behavior

		// Tank
		if (unitType.isTank()) {
			SiegeTankManager.act(unit);
		}
	}

	// =========================================================

	private static boolean tryConsideringCallForHelpMission(Unit unit) {

		// If any call for help has been issued, decide whether to help or not
		if (CallForHelp.isAnyCallForHelp()) {
			UnitManager.decideWhetherToHelpSomeoneCalledForHelp(unit);
		}

		// =========================================================
		// If unit has personalized order
		if (unit.isOnCallForHelpMission()) {
			return true;
		}

		return false;
	}

	public static boolean tryRunningFromCloseDefensiveBuilding(Unit unit) {
		Unit defensiveBuilding = xvr.getEnemyDefensiveGroundBuildingNear(unit);
		if (defensiveBuilding != null) {
			if (unit.getType().isTank()) {
				if (unit.distanceTo(defensiveBuilding) <= 10.8) {
					unit.siege();
					unit.setAiOrder("Siege because building");
				}
				return false;
			} else {
				UnitActions.moveAwayFromUnit(unit, defensiveBuilding);
				unit.setIsRunningFromEnemyNow(defensiveBuilding);
				unit.setAiOrder("Avoid building");
				return true;
			}
		} else {
			return false;
		}
	}

	public static boolean tryAvoidingSeriousSpellEffectsIfNecessary(Unit unit) {
		if (unit.isUnderStorm() || unit.isUnderDisruptionWeb()) {
			if (unit.isMoving()) {
				return true;
			}
			unit.setAiOrder("Avoid spell !!!");
			UnitActions.moveTo(unit, unit.getX() + 5 * 32 * (-1 * RUtilities.rand(0, 1)),
					unit.getY() + 5 * 32 * (-1 * RUtilities.rand(0, 1)));
			return true;
		}
		return false;
	}

	public static boolean tryAvoidingActivatedSpiderMines(Unit unit) {
		if (unit.getType().isFlyer()) {
			return false;
		}

		Unit activatedMine = null;

		// Check if there's any activted mine nearby and if so, get the fuck out
		// of here.
		for (Unit spiderMine : xvr.getUnitsOfGivenTypeInRadius(
				UnitTypes.Terran_Vulture_Spider_Mine, 5, unit, true)) {
			if (spiderMine.isMoving() || spiderMine.isAttacking()) {
				activatedMine = spiderMine;
				break;
			}
		}

		// Move away from activated mine
		if (activatedMine != null) {
			Unit nearestEnemyToMine = xvr.getEnemyNearestTo(unit, true, false);
			UnitActions.moveAwayFromUnit(unit, nearestEnemyToMine);
			unit.setIsRunningFromEnemyNow();
			unit.setAiOrder("MINE!");
			return true;
		}

		return false;
	}

	public static boolean tryRunningIfSeriouslyWounded(Unit unit) {
		double ratio = 0.4;
		// if (xvr.getTimeSeconds() < 330) {
		// ratio = 0.4;
		// }

		if (unit.getHP() <= unit.getMaxHP() * ratio
				|| (unit.getType().isTerranInfantry() && unit.getHP() < 25)) {
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

			// If unit can be repaired
			if (unit.isRepairable()) {

				// Issue order to repair this unit
				RepairAndSons.issueTicketToRepairIfHasnt(unit);

				// Get SCV that is supposed to repair it
				Unit repairer = RepairAndSons.getRepairerForUnit(unit);
				if (repairer != null) {

					// If wrong worker was assigned (e.g. busy), change request.
					if (repairer.isConstructing() || !repairer.isExists()
							|| (!repairer.isRepairing() && !repairer.isMoving())) {
						RepairAndSons.removeTicketFor(unit, repairer);
						RepairAndSons.issueTicketToRepairIfHasnt(unit);
					}

					// If unit is far from the worker that's supposed to repair
					// it, go near him.
					if (repairer.distanceTo(unit) >= 1.9) {
						UnitActions.moveTo(unit, repairer);
						unit.setAiOrder("Go to repair");
					}

					// Repairer is close to this unit.
					else {
						Unit nearEnemy = xvr.getNearestEnemyInRadius(unit, 6, true, true);
						if (nearEnemy != null && nearEnemy.canAttack(unit)) {
							WeaponType groundWeapon = nearEnemy.getType().getGroundWeapon();
							if (groundWeapon != null
									&& unit.distanceTo(nearEnemy) <= groundWeapon
											.getMaxRangeInTiles() + 1.9) {
								unit.setAiOrder("Should be repaired, but RUN!");
								// UnitActions.moveAwayFromNearestEnemy(unit);
								UnitActions.moveToSafePlace(unit);
							}
						}
					}
				}

			}
			return lowLife;
		}
		return false;
	}

	public static void tryUsingStimpacksIfNeeded(Unit unit) {
		if (unit.getType().canUseStimpacks() && TechnologyManager.isStimpacksResearched()
				&& !unit.isStimmed()) {
			if (!unit.isWounded() && xvr.countUnitsEnemyInRadius(unit, 8) >= 2) {
				UnitActions.useTech(unit, TechnologyManager.STIMPACKS);
			}
		}
	}

	public static boolean tryRetreatingIfChancesNotFavorable(Unit unit) {

		// If no base isn't existing, screw this.
		Unit firstBase = xvr.getFirstBase();
		if (firstBase == null) {
			return false;
		}

		// ============================================
		// Some top level situations when don't try retreating

		// If no enemy is critically close, don't retreat
		// if (xvr.getNearestEnemyDistance(unit, true, false) <= 2) {
		// return false;
		// }

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
			return false;
		}

		// ===============================================
		// If all is fine, we can CALCULATE CHANCES TO WIN
		// and if we wouldn't win, then go where it's safe
		// and by doing this we may encounter some help.
		if (!StrengthRatio.isStrengthRatioFavorableFor(unit)) {
			double nearestEnemyDistance = xvr.getNearestEnemyDistance(unit, true, true);

			if (nearestEnemyDistance < 0 || nearestEnemyDistance > 11) {
				UnitActions.moveToSafePlace(unit);
			} else {
				UnitActions.moveAwayFromNearestEnemy(unit);
			}
			unit.setAiOrder("Would lose");
			return true;
		}

		return false;
	}

}

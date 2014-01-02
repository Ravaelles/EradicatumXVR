package ai.core;

import java.awt.Point;

import jnibwapi.JNIBWAPI;
import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitCommandType.UnitCommandTypes;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.util.BWColor;
import ai.handling.army.StrengthRatio;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.other.NukeHandling;
import ai.handling.units.UnitCounter;
import ai.managers.ArmyCreationManager;
import ai.managers.BuildingManager;
import ai.managers.StrategyManager;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.units.UnitManager;
import ai.terran.TerranCommandCenter;
import ai.utils.CodeProfiler;
import ai.utils.RUtilities;

public class Painter {

	public static final boolean FULL_DEBUG = true;
	public static boolean errorOcurred = false;
	public static String errorOcurredDetails = "";

	private static int messageCounter = 1;
	private static int mainMessageRowCounter = 0;

	public static int ourDeaths = 0;
	public static int enemyDeaths = 0;

	public static void paintAll(XVR xvr) {
		CodeProfiler.startMeasuring("Painting");

		int oldMainMessageRowCounter = mainMessageRowCounter;
		mainMessageRowCounter = 0;

		// MapPoint assimilator = Constructing.findTileForAssimilator();
		// if (assimilator != null) {
		// xvr.getBwapi().drawBox(assimilator.getX(), assimilator.getY(),
		// 3 * 32, 2 * 32, BWColor.TEAL, false, false);
		// }

		if (FULL_DEBUG) {
			paintTimeConsumption(xvr);
			paintNextBuildingsPosition(xvr);
		}
		paintUnitsDetails(xvr);

		if (FULL_DEBUG) {
			paintValuesOverUnits(xvr);
		}

		// // Draw regions
		// for (Region region : xvr.getBwapi().getMap().getRegions()) {
		// int[] bounds = region.getCoordinates();
		// xvr.getBwapi().drawBox(bounds[0] - bounds[2],
		// bounds[1] - bounds[3], 2 * bounds[2], 2 * bounds[3],
		// BWColor.TEAL, false, false);
		// xvr.getBwapi()
		// .drawText(
		// region.getCenterX(),
		// region.getCenterY(),
		// String.format("Region [%d]", region
		// .getChokePoints().size()), false);
		// }

		// // Draw next building place
		// MapPoint buildTile = ProtossPylon.findTileNearPylonForNewBuilding();
		// if (buildTile != null) {
		// xvr.getBwapi().drawCircle(buildTile.getX() - 64,
		// buildTile.getX() - 48, 50, BWColor.TEAL, false, false);
		// }

		// xvr.getBwapi()
		// .drawText(
		// region.getCenterX(),
		// region.getCenterY(),
		// String.format("Region [%d]", region
		// .getChokePoints().size()), false);

		// Draw choke points
		paintChokePoints(xvr);

		// Draw where to attack
		paintAttackLocation(xvr);

		// Statistics
		paintStatistics(xvr);

		if (Painter.errorOcurred) {
			String string = "!!! EXCEPTION (" + errorOcurredDetails + ") !!!";
			xvr.getBwapi().drawText(new Point(320 - string.length() * 3, 20),
					BWColor.getToStringHex(BWColor.RED) + string, true);
		}

		// ========
		mainMessageRowCounter = oldMainMessageRowCounter;

		CodeProfiler.endMeasuring("Painting");
	}

	private static final int timeConsumptionLeftOffset = 575;
	private static final int timeConsumptionTopOffset = 30;
	private static final int timeConsumptionBarMaxWidth = 50;
	private static final int timeConsumptionBarHeight = 14;
	private static final int timeConsumptionYInterval = 16;

	private static void paintTimeConsumption(XVR xvr) {
		JNIBWAPI bwapi = xvr.getBwapi();

		int counter = 0;
		double maxValue = RUtilities.getMaxElement(CodeProfiler.getAspectsTimeConsumption()
				.values());

		// System.out.println();
		// for (double val : TimeMeasurer.getAspectsTimeConsumption().values())
		// {
		// System.out.println("   " + val);
		// }

		// System.out.println(TimeMeasurer.getAspectsTimeConsumption().keySet().size());
		for (String aspectTitle : CodeProfiler.getAspectsTimeConsumption().keySet()) {
			int x = timeConsumptionLeftOffset;
			int y = timeConsumptionTopOffset + timeConsumptionYInterval * counter++;

			int value = CodeProfiler.getAspectsTimeConsumption().get(aspectTitle).intValue();

			// Draw aspect time consumption bar
			int barWidth = (int) (timeConsumptionBarMaxWidth * value / maxValue);
			if (barWidth < 3) {
				barWidth = 3;
			}
			if (barWidth > timeConsumptionBarMaxWidth) {
				barWidth = timeConsumptionBarMaxWidth;
			}
			// System.out.println("   " + aspectTitle + " x:" + x + ", y:" + y +
			// "  ## " + barWidth);
			bwapi.drawBox(x, y, x + barWidth, y + timeConsumptionBarHeight, BWColor.WHITE, true,
					true);
			bwapi.drawBox(x, y, x + timeConsumptionBarMaxWidth, y + timeConsumptionBarHeight,
					BWColor.BLACK, false, true);

			// Draw aspect label
			bwapi.drawText(x + 2, y - 1, BWColor.getToStringHex(BWColor.YELLOW) + aspectTitle, true);
		}
	}

	private static void paintValuesOverUnits(XVR xvr) {
		JNIBWAPI bwapi = xvr.getBwapi();
		String text;
		String cooldown;
		double strength;

		for (Unit unit : bwapi.getMyUnits()) {
			UnitType type = unit.getType();
			if (type.isBuilding() || type.isSpiderMine() || type.isWorker()) {
				continue;
			}

			// ==========================
			// Strength evaluation
			// strength = StrengthEvaluator.calculateStrengthRatioFor(unit);
			strength = unit.getStrengthRatio();
			if (strength != StrengthRatio.STRENGTH_RATIO_FULLY_SAFE) {
				strength -= 1; // make +/- values display
				text = (strength > 0 ? (BWColor.getToStringHex(BWColor.GREEN) + "+") : (BWColor
						.getToStringHex(BWColor.RED) + "")) + String.format("%.1f", strength);
				bwapi.drawText(unit.getX() - 7, unit.getY() + 30, text, false);
			}

			// ==========================
			// Cooldown
			if (unit.getGroundWeaponCooldown() > 0) {
				int cooldownWidth = 20;
				int cooldownHeight = 4;
				int cooldownLeft = unit.getX() - cooldownWidth / 2;
				int cooldownTop = unit.getY() + 23;
				cooldown = BWColor.getToStringHex(BWColor.YELLOW) + "("
						+ unit.getGroundWeaponCooldown() + ")";

				// Paint box
				int cooldownProgress = cooldownWidth * unit.getGroundWeaponCooldown()
						/ (unit.getType().getGroundWeapon().getDamageCooldown() + 1);
				bwapi.drawBox(cooldownLeft, cooldownTop, cooldownLeft + cooldownProgress,
						cooldownTop + cooldownHeight, BWColor.RED, true, false);

				// Paint box borders
				bwapi.drawBox(cooldownLeft, cooldownTop, cooldownLeft + cooldownWidth, cooldownTop
						+ cooldownHeight, BWColor.BLACK, false, false);

				// Paint label
				bwapi.drawText(cooldownLeft + cooldownWidth - 4, cooldownTop, cooldown, false);
			}
		}
	}

	private static void paintAttackLocation(XVR xvr) {
		JNIBWAPI bwapi = xvr.getBwapi();
		if (StrategyManager.getTargetUnit() != null) {
			bwapi.drawCircle(StrategyManager.getTargetUnit().getX(), StrategyManager
					.getTargetUnit().getY(), 33, BWColor.RED, false, false);
			bwapi.drawCircle(StrategyManager.getTargetUnit().getX(), StrategyManager
					.getTargetUnit().getY(), 32, BWColor.RED, false, false);
			bwapi.drawCircle(StrategyManager.getTargetUnit().getX(), StrategyManager
					.getTargetUnit().getY(), 3, BWColor.RED, true, false);
		}

		if (NukeHandling.nuclearDetectionPoint != null) {
			MapPoint nuclearPoint = NukeHandling.nuclearDetectionPoint;
			bwapi.drawCircle(nuclearPoint.getX(), nuclearPoint.getY(), 20, BWColor.RED, false,
					false);
			bwapi.drawCircle(nuclearPoint.getX(), nuclearPoint.getY(), 18, BWColor.RED, false,
					false);
			bwapi.drawCircle(nuclearPoint.getX(), nuclearPoint.getY(), 16, BWColor.RED, false,
					false);
			bwapi.drawCircle(nuclearPoint.getX(), nuclearPoint.getY(), 14, BWColor.RED, false,
					false);
		}
	}

	private static void paintNextBuildingsPosition(XVR xvr) {
		// MapPoint building;

		// Paint next NEXUS position
		// building = TerranCommandCenter.findTileForNextBase(false);
		// if (building != null) {
		// xvr.getBwapi().drawBox(building.getX(), building.getY(),
		// building.getX() + 4 * 32,
		// building.getY() + 3 * 32, BWColor.TEAL, false, false);
		// // xvr.getBwapi().drawText(building.getX() + 10, building.getY() +
		// // 30,
		// // "Nexus", false);
		// }

		// Paint next BUNKER position
		// if (TerranBunker.getNumberOfUnits() == 0) {
		// building = TerranBunker.findTileForBunker();
		// if (building != null) {
		// xvr.getBwapi().drawBox(building.getX(), building.getY(),
		// building.getX() + 2 * 32,
		// building.getY() + 2 * 32, BWColor.TEAL, false, false);
		// xvr.getBwapi()
		// .drawText(building.getX() + 10, building.getY() + 30, "Bunker",
		// false);
		// }
		// }

		// // Paint next PYLON position
		// building = ProtossPylon.findTileForPylon();
		// if (building != null) {
		// xvr.getBwapi().drawBox(building.getX(), building.getY(),
		// building.getX() + 2 * 32, building.getY() + 2 * 32,
		// BWColor.TEAL, false, false);
		// xvr.getBwapi().drawText(building.getX() + 10, building.getY() + 30,
		// "Pylon", false);
		// }
		//
		// // Paint GATEWAY position
		// building = ProtossPylon
		// .findTileNearPylonForNewBuilding(UnitTypes.Protoss_Gateway);
		// if (building != null) {
		// xvr.getBwapi().drawBox(building.getX(), building.getY(),
		// building.getX() + 2 * 32, building.getY() + 2 * 32,
		// BWColor.TEAL, false, false);
		// xvr.getBwapi().drawText(building.getX() + 10, building.getY() + 30,
		// "Gateway", false);
		// }
	}

	private static void paintChokePoints(XVR xvr) {
		for (ChokePoint choke : MapExploration.getChokePoints()) {
			// xvr.getBwapi().drawBox(bounds[0], bounds[1],
			// bounds[2], bounds[3], BWColor.TEAL, false, false);
			// xvr.getBwapi().drawCircle(chokePoint.getFirstSideX(),
			// chokePoint.getFirstSideY(), 3, BWColor.RED, true, false);
			// xvr.getBwapi().drawCircle(chokePoint.getSecondSideX(),
			// chokePoint.getSecondSideY(), 3, BWColor.BLUE, true, false);
			xvr.getBwapi().drawCircle(choke.getCenterX(), choke.getCenterY(),
					(int) choke.getRadius(), BWColor.BLACK, false, false);
			// xvr.getBwapi().drawText(
			// chokePoint.getCenterX(),
			// chokePoint.getCenterY(),
			// String.format("Choke [%d,%d]",
			// chokePoint.getCenterX() / 32,
			// chokePoint.getCenterY() / 32), false);

			// Region ourRegion =
			// xvr.getBwapi().getMap().getRegion(xvr.getFirstBase());
			// boolean onlyOurRegion = false;
			// if (choke.getSecondRegion().getConnectedRegions().size() == 1) {
			// Region region = (Region) RUtilities.getSetElement(choke
			// .getSecondRegion().getConnectedRegions(), 0);
			// if (region.equals(ourRegion)) {
			// onlyOurRegion = true;
			// }
			// }
			// if (choke.getFirstRegion().getConnectedRegions().size() == 1) {
			// Region region = (Region) RUtilities.getSetElement(choke
			// .getFirstRegion().getConnectedRegions(), 0);
			// if (region.equals(ourRegion)) {
			// onlyOurRegion = true;
			// }
			// }
			//
			// String string = choke.getFirstRegionID() + " ("
			// + choke.getFirstRegion().getConnectedRegions().size()
			// + "), " + choke.getSecondRegionID() + "("
			// + choke.getSecondRegion().getConnectedRegions().size()
			// + "), " + (onlyOurRegion ? "YESSSSS" : "no");
			//
			// xvr.getBwapi().drawText(choke.getCenterX() - 40,
			// choke.getCenterY(),
			// string, false);
		}

	}

	private static void paintUnitsDetails(XVR xvr) {
		// Draw circles over workers (blue if they're gathering minerals, green
		// if gas, yellow if they're constructing).
		JNIBWAPI bwapi = xvr.getBwapi();
		for (Unit u : bwapi.getMyUnits()) {
			if (!u.isCompleted()) {
				continue;
			}

			boolean isBuilding = u.getType().isBuilding();
			if (FULL_DEBUG && !isBuilding) {
				if (u.isGatheringMinerals()) {
					bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.BLUE, false, false);
				} else if (u.isGatheringGas()) {
					bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.GREEN, false, false);
				} else if (u.isMoving() && !u.isConstructing()) {
					bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.GREY, false, false);

					if (xvr.getDistanceBetween(u, u.getTargetX(), u.getTargetY()) <= 15) {
						bwapi.drawLine(u.getX(), u.getY(), u.getTargetX(), u.getTargetY(),
								BWColor.GREY, false);
					}
				} else if (u.isRepairing()) {
					bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.PURPLE, false, false);
					bwapi.drawCircle(u.getX(), u.getY(), 11, BWColor.PURPLE, false, false);
					bwapi.drawCircle(u.getX(), u.getY(), 10, BWColor.PURPLE, false, false);
				} else if (u.isConstructing()
						|| u.getLastCommandID() == UnitCommandTypes.Build.ordinal()) {
					bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.ORANGE, false, false);
					bwapi.drawCircle(u.getX(), u.getY(), 11, BWColor.ORANGE, false, false);
				} else if (u.isStuck()) {
					bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.TEAL, false, false);
					bwapi.drawCircle(u.getX(), u.getY(), 11, BWColor.TEAL, false, false);
					bwapi.drawCircle(u.getX(), u.getY(), 10, BWColor.TEAL, false, false);
					bwapi.drawCircle(u.getX(), u.getY(), 9, BWColor.TEAL, false, false);
				}

				// ATTACKING: Display red circle around unit and paint a
				// line to the target
				else if (u.isAttacking()) {
					bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.RED, false, false);
					bwapi.drawCircle(u.getX(), u.getY(), 11, BWColor.RED, false, false);
				}

				// HEALED unit, draw Red Cross on white background
				else if (u.isBeingHealed() || u.isBeingRepaired()) {
					bwapi.drawBox(u.getX() - 8, u.getY() - 8, u.getX() + 8, u.getY() + 8,
							BWColor.WHITE, true, false);
					bwapi.drawBox(u.getX() - 5, u.getY() - 2, u.getX() + 5, u.getY() + 2,
							BWColor.RED, true, false);
					bwapi.drawBox(u.getX() - 2, u.getY() - 5, u.getX() + 2, u.getY() + 5,
							BWColor.RED, true, false);
				}

				// IDLE unit, draw question mark
				else if (u.isIdle() && !u.getType().isBuilding()) {
					bwapi.drawText(u.getX() - 2, u.getY() - 2,
							BWColor.getToStringHex(BWColor.YELLOW) + "?", false);
				}

				// CONSTRUCTING: display building name
				if (u.isConstructing()) {
					paintConstructionProgress(u);
					String name = (UnitType.getUnitTypesByID(u.getBuildTypeID()) + "").replace(
							"Terran_", "");
					bwapi.drawText(u.getX() - 30, u.getY(), BWColor.getToStringHex(BWColor.GREY)
							+ "-> " + name, false);
				}

				// ACTION LABEL: display action like #RUN, #LOAD
				if (u.hasAiOrder()) {
					bwapi.drawText(u.getX() - u.getAiOrderString().length() * 3, u.getY(),
							BWColor.getToStringHex(BWColor.WHITE) + u.getAiOrderString(), false);
				}
			}

			// IS BUILDING: display action name that's currently pending.
			else if (isBuilding) {
				if (u.isTraining()) {
					String name = (bwapi.getUnitCommandType(u.getLastCommandID()).getName() + "")
							.replace("Terran_", "");
					bwapi.drawText(u.getX() - 30, u.getY() + 10,
							BWColor.getToStringHex(BWColor.GREY) + "-> " + name, false);
				}

				int enemiesNearby = xvr.countUnitsEnemyInRadius(u, 11);
				if (enemiesNearby > 0) {
					String string = enemiesNearby + " enemies";
					bwapi.drawText(u.getX() - string.length() * 4, u.getY(),
							BWColor.getToStringHex(BWColor.RED) + string, false);
				}
				if (u.getType().isBunker()) {
					int repairers = BuildingManager.countNumberOfRepairersForBuilding(u);
					if (repairers > 0) {
						String repairersString = repairers + " repairers";
						bwapi.drawText(u.getX() - repairersString.length() * 4, u.getY() + 10,
								BWColor.getToStringHex(BWColor.ORANGE) + repairersString, false);
					}
				}
			}
		}
	}

	private static void paintConstructionProgress(Unit unit) {
		if (!unit.isCompleted()) {
			JNIBWAPI bwapi = XVR.getInstance().getBwapi();
			String stringToDisplay;

			int labelMaxWidth = 20;
			int labelHeight = 4;
			int labelLeft = unit.getX() - labelMaxWidth / 2;
			int labelTop = unit.getY() + 23;

			int labelProgress = 1 + 99 * unit.getHP() / unit.getType().getMaxHitPoints();
			stringToDisplay = BWColor.getToStringHex(BWColor.YELLOW) + "(" + labelProgress + "%)";

			// Paint box
			bwapi.drawBox(labelLeft, labelTop, labelLeft + labelProgress / labelMaxWidth, labelTop
					+ labelHeight, BWColor.GREY, true, false);

			// Paint box borders
			bwapi.drawBox(labelLeft, labelTop, labelLeft + labelMaxWidth, labelTop + labelHeight,
					BWColor.BLACK, false, false);

			// Paint label
			bwapi.drawText(labelLeft + labelMaxWidth - 4, labelTop, stringToDisplay, false);
		}
	}

	@SuppressWarnings("static-access")
	private static void paintStatistics(XVR xvr) {
		if (xvr.getFirstBase() == null) {
			return;
		}

		int time = xvr.getFrames();
		paintMainMessage(xvr, "Time: " + (time / 30) + "s"); // (" + time + ")"
		paintMainMessage(xvr, "Killed: " + enemyDeaths);
		paintMainMessage(xvr, "Lost: " + ourDeaths);
		if (StrategyManager.getTargetUnit() != null) {
			Unit attack = StrategyManager.getTargetUnit();
			paintMainMessage(xvr,
					"Attack target: " + attack.getName() + " ## visible:" + attack.isVisible()
							+ ", exists:" + attack.isExists() + ", HP:" + attack.getHP());
		}

		if (FULL_DEBUG) {
			paintMainMessage(xvr, "--------------------");
			paintMainMessage(xvr, "Enemy: " + xvr.getEnemyRace());
			paintMainMessage(xvr, "HQs: " + UnitCounter.getNumberOfUnitsCompleted(UnitManager.BASE));

			if (UnitCounter.getNumberOfUnitsCompleted(UnitManager.BASE) > 0)
				paintMainMessage(xvr,
						"Barracks: " + UnitCounter.getNumberOfUnitsCompleted(UnitManager.BASE));

			if (UnitCounter.getNumberOfUnitsCompleted(UnitTypes.Terran_Bunker) > 0)
				paintMainMessage(
						xvr,
						"Bunkers: "
								+ UnitCounter.getNumberOfUnitsCompleted(UnitTypes.Terran_Bunker));

			paintMainMessage(xvr, "--------------------");

			paintMainMessage(
					xvr,
					"SCVs: ("
							+ UnitCounter.getNumberOfUnitsCompleted(UnitManager.WORKER)
							+ " / "
							+ TerranCommandCenter.getOptimalMineralGatherersAtBase(xvr
									.getFirstBase()) + ")");

			paintMainMessage(
					xvr,
					"Gath. gas: ("
							+ TerranCommandCenter.getNumberOfGasGatherersForBase(xvr.getFirstBase())
							+ ")");

			UnitTypes type;

			// type = UnitTypes.Terran_Marine;
			// if (UnitCounter.getNumberOfUnitsCompleted(type) > 0)
			// paintMainMessage(xvr, "Marines: " +
			// UnitCounter.getNumberOfUnitsCompleted(type));
			//
			// type = UnitTypes.Terran_Medic;
			// if (UnitCounter.getNumberOfUnitsCompleted(type) > 0)
			// paintMainMessage(xvr, "Medics: " +
			// UnitCounter.getNumberOfUnitsCompleted(type));
			//
			// type = UnitTypes.Terran_Firebat;
			// if (UnitCounter.getNumberOfUnitsCompleted(type) > 0)
			// paintMainMessage(xvr, "Firebats: " +
			// UnitCounter.getNumberOfUnitsCompleted(type));

			int infantry = UnitCounter.getNumberOfInfantryUnitsCompleted();
			if (infantry > 0)
				paintMainMessage(xvr, "Infantry: " + infantry);

			type = UnitTypes.Terran_Vulture;
			if (UnitCounter.getNumberOfUnitsCompleted(type) > 0)
				paintMainMessage(xvr, "Vultures: " + UnitCounter.getNumberOfUnitsCompleted(type));

			type = UnitTypes.Terran_Goliath;
			if (UnitCounter.getNumberOfUnitsCompleted(type) > 0)
				paintMainMessage(xvr, "Goliaths: " + UnitCounter.getNumberOfUnitsCompleted(type));

			type = UnitTypes.Terran_Siege_Tank_Siege_Mode;
			if (UnitCounter.getNumberOfUnitsCompleted(type) > 0)
				paintMainMessage(xvr, "Tanks: " + UnitCounter.getNumberOfUnitsCompleted(type));

			paintMainMessage(xvr, "--------------------");

			String minUnitsString = "";
			if (StrategyManager.getMinBattleUnits() > 0) {
				minUnitsString += " (min. " + StrategyManager.getMinBattleUnits() + ")";
			}
			paintMainMessage(xvr, "Battle units: " + UnitCounter.getNumberOfBattleUnits()
					+ minUnitsString);

			String buildArmy = "";
			if (ArmyCreationManager.weShouldBuildBattleUnits()) {
				buildArmy = "true";
			} else {
				buildArmy = "# FALSE #";
			}
			paintMainMessage(xvr, "Build army: " + buildArmy);

			boolean attackPending = StrategyManager.isAttackPending();
			paintMainMessage(xvr, "Attack ready: " + (attackPending ? "YES" : "no"));

			if (attackPending) {
				paintMainMessage(
						xvr,
						"(distance allowed: "
								+ (int) StrategyManager.getAllowedDistanceFromSafePoint() + ")");
			}

			paintMainMessage(xvr, "--------------------");

		}

		if (xvr.getFrames() % 10 == 0) {

		}

		for (UnitTypes type : ShouldBuildCache.getBuildingsThatShouldBeBuild()) {
			paintMainMessage(xvr,
					"-> "
							+ type.name().substring(0, Math.min(17, type.name().length()))
									.toUpperCase().replace("TERRAN_", "") + ": true");
		}
	}

	private static void paintMainMessage(XVR xvr, String string) {
		// string = "\u001F" + string;
		string = BWColor.getToStringHex(BWColor.WHITE) + string;
		xvr.getBwapi().drawText(new Point(5, 12 * mainMessageRowCounter++), string, true);
	}

	public static void message(XVR xvr, String txt, boolean displayCounter) {
		xvr.getBwapi().printText((displayCounter ? ("(" + messageCounter++ + ".) ") : "") + txt);
	}

	public static void message(XVR xvr, String txt) {
		message(xvr, txt, true);
	}

	public static void messageBuild(XVR xvr, UnitTypes type) {
		String building = "#" + UnitType.getUnitTypesByID(type.ordinal()).name();

		message(xvr, "Trying to build " + building);
	}

}

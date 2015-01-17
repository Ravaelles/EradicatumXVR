package ai.core;

import java.util.ArrayList;
import java.util.HashMap;

import jnibwapi.BWAPIEventListener;
import jnibwapi.JNIBWAPI;
import jnibwapi.model.Player;
import jnibwapi.model.Unit;
import jnibwapi.types.RaceType.RaceTypes;
import jnibwapi.types.UnitDamages;
import jnibwapi.types.UnitType;
import ai.handling.map.MapExploration;
import ai.handling.other.NukeHandling;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.army.tanks.EnemyTanksManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranComsatStation;

public class XVRClient implements BWAPIEventListener {

	private JNIBWAPI bwapi;
	private XVR xvr;

	private ArrayList<Integer> historyOfOurUnits = new ArrayList<>(400);
	private HashMap<Integer, UnitType> historyOfOurUnitsObjects = new HashMap<>();

	// =========================================

	@Override
	public void gameStarted() {

		// Game settings
		// bwapi.enableUserInput();
		bwapi.setGameSpeed(XVR.GAME_SPEED);
		bwapi.loadMapData(true);

		MapExploration.disableChokePointsNearFirstBase();

		// ========================================

		xvr.SELF = bwapi.getSelf();
		xvr.SELF_ID = bwapi.getSelf().getID();

		xvr.NEUTRAL = bwapi.getNeutralPlayer();

		Player enemy = bwapi.getEnemies().get(0);
		xvr.setENEMY(enemy);
		xvr.ENEMY_ID = enemy.getID();

		// ========================================

		// Creates map where values of attacks of all unit types are stored.
		UnitDamages.rememberUnitDamageValues();

		// Removes some of initial choke points e.g. those on the edge of the
		// map.
		MapExploration.processInitialChokePoints();

		// ========================================

		// Enemy -> Protoss
		if (enemy.getRaceID() == RaceTypes.Protoss.getID()) {
			XVR.setEnemyRace("Protoss");
		}
		// ENEMY -> Terran
		else if (enemy.getRaceID() == RaceTypes.Terran.getID()) {
			XVR.setEnemyRace("Terran");
		}
		// ENEMY -> Zerg
		else if (enemy.getRaceID() == RaceTypes.Zerg.getID()) {
			XVR.setEnemyRace("Zerg");
		}

		// ==========
		// HotFix
		TerranCommandCenter.initialMineralGathering();
	}

	// =========================================

	public JNIBWAPI getBwapi() {
		return bwapi;
	}

	public static void main(String[] args) {
		new XVRClient();
	}

	public XVRClient() {
		bwapi = new JNIBWAPI(this);
		xvr = new XVR(this);

		bwapi.start();
	}

	// =========================================

	@Override
	public void connected() {
		bwapi.loadTypeData();
	}

	@Override
	public void gameUpdate() {
		// if (xvr.getTime() % 10 == 0) {
		// }
		Painter.paintAll(xvr);

		xvr.act();
	}

	public void gameEnded() {
	}

	public void keyPressed(int keyCode) {
	}

	public void matchEnded(boolean winner) {
		Painter.message(xvr, "## Rock 'n' Roll! ##", false);
	}

	public void sendText(String text) {
	}

	private static boolean responded = false;

	public void receiveText(String text) {
		if (!responded) {
			responded = true;
			xvr.getBwapi().sendText("sorry, cant talk right now");
			xvr.getBwapi().sendText("have to click very fast, u kno");
		}
	}

	public void nukeDetect(int x, int y) {
		System.out.println("DETECTED NUKE AT: " + x + ", " + y);
		NukeHandling.nukeDetected(x, y);
	}

	public void nukeDetect() {
		System.out.println("DETECTED NUKE OVERALL");
	}

	public void playerLeft(int playerID) {
		xvr.getBwapi().sendText("########################");
		xvr.getBwapi().sendText("## Sayonara, gringo! ^_^ ##");
		xvr.getBwapi().sendText("########################");
	}

	public void unitCreate(int unitID) {
		xvr.unitCreated(unitID);
		Unit unit = bwapi.getUnit(unitID);
		UnitType unitType = unit.getType();
		if (!unit.isEnemy()) {
			historyOfOurUnits.add(unitID);
			historyOfOurUnitsObjects.put(unitID, unit.getType());
			// if (unitType.isBuilding() && unitType.isBase()) {
			//
			// // Build pylon nearby
			// Constructing.forceConstructingPylonNear(unit);
			// }
		}
		// if (unitType.isBuilding()) {
		// TerranConstructing.removeIsBeingBuilt(unitType);
		// }

		if (unit.isMyUnit() && unitType.isBase()) {
			TerranCommandCenter.updateNextBaseToExpand();
		}
	}

	public void unitDestroy(int unitID) {
		boolean wasOurUnit = historyOfOurUnits.contains(unitID);
		if (wasOurUnit) {
			Painter.ourDeaths++;
		} else {
			Painter.enemyDeaths++;
		}

		if (!wasOurUnit) {
			// System.out.println("DESTROYED: " + unitID);
			boolean removedSomething = MapExploration.enemyUnitDestroyed(unitID);

			// Check if massive attack target has just been destroyed; if so,
			// redefine it.
			if (removedSomething && StrategyManager.getTargetUnit() != null
					&& StrategyManager.getTargetUnit().getID() == unitID) {
				// System.out.println("REDIFINING... " +
				// MassiveAttack.getTargetUnit().toStringShort());
				StrategyManager.forceRedefinitionOfNextTarget();
			}
		}

		// =====================================
		// Check what type was the destroyed unit
		UnitType unitType = null;
		for (int historyUnitID : historyOfOurUnitsObjects.keySet()) {
			if (historyUnitID == unitID) {
				unitType = historyOfOurUnitsObjects.get(historyUnitID);
				break;
				// System.out.println();
				// System.out.println("Destroyed unit was " + unitType);
			}
		}

		if (unitType != null) {
			if (unitType.isBase() && wasOurUnit) {
				TerranCommandCenter.updateNextBaseToExpand();
			}

			// Do not count spider mines as losses.
			if (unitType.isSpiderMine()) {
				Painter.ourDeaths--;
			}
		}

		// Unit unit = Unit.getByID(unitID);
		// if (unit == null) {
		// return;
		// }
		// System.out.println("Destroyed unit was " + unit.toStringShort());
		// UnitType unitType = UnitType.getUnitTypeByID(unit.getTypeID());
	}

	private static int distantEnemyUnitsDiscovered = 0;

	private int discoverCounter = 0;

	public void unitDiscover(int unitID) {
		Unit unit = Unit.getByID(unitID);
		if (unit == null || !unit.isEnemy()) {
			return;
		}

		if (discoverCounter == 0) {
			if (unit.getType().getRaceID() == RaceTypes.Protoss.getID()) {
				XVR.setEnemyRace("Protoss");
			} else if (unit.getType().getRaceID() == RaceTypes.Zerg.getID()) {
				XVR.setEnemyRace("Zerg");
			} else if (unit.getType().getRaceID() == RaceTypes.Terran.getID()) {
				XVR.setEnemyRace("Terran");
			}
		}

		discoverCounter++;

		// Add info that we discovered enemy unit
		MapExploration.enemyUnitDiscovered(unit);

		// System.out.println("Unit discover: " + (unit != null ? unit.getName()
		// : "null"));

		if (StrategyManager.getMinBattleUnits() < 10) {
			UnitType type = unit.getType();
			boolean isAdvUnit = type.isDragoon() || type.isCarrier() || type.isVulture()
					|| type.isTank() || type.isHydralisk() || type.isLurker() || type.isMutalisk();
			if (isAdvUnit) {
				distantEnemyUnitsDiscovered++;
			}
			if (distantEnemyUnitsDiscovered >= 1) {
				StrategyManager.waitForMoreUnits();
			}
		}

		// if (XVR.isEnemyProtoss()) {
		// if (unit.getType().isDragoon()) {
		// TerranAcademy.forceShouldBuild();
		// }
		// }
	}

	public void unitEvade(int unitID) {
		Unit unit = Unit.getByID(unitID);
		if (unit == null || !unit.isEnemy()) {
			return;
		}
		// System.out.println("Unit evade: "
		// + (unit != null ? unit.getName() : "null"));
	}

	public void unitHide(int unitID) {
		Unit unit = Unit.getByID(unitID);
		if (unit == null || !unit.isEnemy()) {
			return;
		}

		// System.out.println("Unit hide: "
		// + (unit != null ? unit.getName() : "null"));
		if (unit.isEnemy() && unit.isHidden()) {
			TerranComsatStation.hiddenUnitDetected(unit);
		}
	}

	public void unitMorph(int unitID) {
	}

	public void unitShow(int unitID) {
		Unit unit = Unit.getByID(unitID);
		if (unit == null || !unit.isEnemy()) {
			return;
		}

		UnitType type = unit.getType();
		if (type.isCarrier() && !TerranBarracks.isPlanAntiAirActive()) {
			TerranBarracks.changePlanToAntiAir();
		}

		// TANK
		if (type.isTank()) {
			EnemyTanksManager.updateTankPosition(unit);
		}
	}

	public void unitRenegade(int unitID) {
		Unit unit = Unit.getByID(unitID);
		if (unit == null || !unit.isEnemy()) {
			return;
		}

		// Add info that we discovered enemy unit
		MapExploration.enemyUnitDiscovered(unit);
	}

	public void saveGame(String gameName) {
	}

	public void unitComplete(int unitID) {
		Unit unit = bwapi.getUnit(unitID);
		UnitType unitType = unit.getType();
		if (unit.isMyUnit() && unitType.isBase()) {
			TerranCommandCenter.updateNextBaseToExpand();
		}
	}

	public void playerDropped(int playerID) {
		xvr.getBwapi().sendText("########################");
		xvr.getBwapi().sendText("## Sayonara, gringo! ^_^ ##");
		xvr.getBwapi().sendText("########################");
	}

}

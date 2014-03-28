package ai.core;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import jnibwapi.JNIBWAPI;
import jnibwapi.model.Map;
import jnibwapi.model.Player;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.ConstructionManager;
import ai.managers.economy.TechnologyManager;
import ai.managers.enemy.HiddenEnemyUnitsManager;
import ai.managers.strategy.StrategyManager;
import ai.managers.units.UnitManager;
import ai.managers.units.army.ArmyCreationManager;
import ai.managers.units.buildings.FlyingBuildingManager;
import ai.managers.units.workers.WorkerManager;
import ai.terran.TerranBarracks;
import ai.terran.TerranBunker;
import ai.terran.TerranCommandCenter;
import ai.terran.TerranSiegeTank;
import ai.utils.CodeProfiler;
import ai.utils.RUtilities;

/**
 * Main controller of AI. It contains main method act() and has many, many
 * utility functions that most of other classes use.
 */
public class XVR {

	/** Less = faster. */
	public static final int GAME_SPEED = 0;

	/**
	 * There are several methods of type like "getUnitsNear". This value is this
	 * "near" distance, expressed in game tiles (32 pixels).
	 */
	private static final int WHAT_IS_NEAR_DISTANCE = 13;

	private static XVR xvr;

	private Player ENEMY;
	public int ENEMY_ID;
	private String ENEMY_RACE = "Undefined";
	public Player SELF;
	public Player NEUTRAL;
	public int SELF_ID;

	protected static boolean enemyTerran = false;
	protected static boolean enemyZerg = false;
	protected static boolean enemyProtoss = false;

	private static boolean _gameSpeedChangeApplied = false;

	private XVRClient client;
	private JNIBWAPI bwapi;

	private int frameCounter = 0;
	private int secondCounter = 0;

	// =====================================================

	@SuppressWarnings("static-access")
	public XVR(XVRClient bwapiClient) {
		this.xvr = this;
		this.client = bwapiClient;
		this.bwapi = bwapiClient.getBwapi();
	}

	// =====================================================

	/** This method is called every 30th frame (approx. once a second). */
	public void act() {
		try {

			// Slow down after the start
			if (!_gameSpeedChangeApplied && secondCounter > 60) {
				bwapi.setGameSpeed(8);
				_gameSpeedChangeApplied = true;
			}

			// Update time
			frameCounter = bwapi.getFrameCount();
			secondCounter = frameCounter / 30;

			// Calculate numbers of units by type, so this info can be used in
			// other methods.
			if (getFrames() % 4 == 0) {
				UnitCounter.recalculateUnits();
			}

			// If there are some enemy units invisible
			if (getFrames() % 10 == 0) {
				MapExploration.updateInfoAboutHiddenUnits();
			}

			// Try to detect hidden enemy units
			if (getFrames() % 13 == 0) {
				HiddenEnemyUnitsManager.act();
			}

			// See if we're strong enough to attack the enemy
			if (getFrames() % 21 == 0) {
				CodeProfiler.startMeasuring("Strategy");
				StrategyManager.evaluateMassiveAttackOptions();
				CodeProfiler.endMeasuring("Strategy");
			}

			// Handle technologies
			if (getFrames() % 23 == 0) {
				CodeProfiler.startMeasuring("Technology");
				TechnologyManager.act();
				CodeProfiler.endMeasuring("Technology");
			}

			// Now let's mine minerals with your idle workers.
			if (getFrames() % 11 == 0) {
				CodeProfiler.startMeasuring("Workers");
				WorkerManager.act();
				CodeProfiler.endMeasuring("Workers");
			}

			// Handle behavior of units and buildings.
			// Handle units in neighborhood of army units.
			if (getFrames() % 11 == 0) {
				CodeProfiler.startMeasuring("Army");
				UnitManager.act();
				CodeProfiler.endMeasuring("Army");
			}

			// Avoid being under psionic storm, disruptive web etc.
			if (getFrames() % 8 == 0) {
				UnitManager.avoidSpellEffectsAndMinesIfNecessary();
			}

			// Handle Nexus behavior differently, more often.
			if (getFrames() % 8 == 0) {
				TerranCommandCenter.act();
			}

			// Handle army building.
			if (getFrames() % 11 == 0) {
				CodeProfiler.startMeasuring("Army build");
				ArmyCreationManager.act();
				CodeProfiler.endMeasuring("Army build");
			}

			// Handle constructing new buildings
			if (getFrames() % 9 == 0) {
				CodeProfiler.startMeasuring("Construct");
				ConstructionManager.act();
				CodeProfiler.endMeasuring("Construct");
			}

			// Define median siege tank i.e. the one in the center of others
			if (getFrames() % 48 == 0) {
				TerranSiegeTank.defineMedianTank();
			}

			// Handle flying building, that will enhance shoot range for tanks
			if (getFrames() % 35 == 0) {
				FlyingBuildingManager.act();
			}
		} catch (Exception e) {
			Painter.errorOccured(e.getStackTrace()[0].toString());
			System.err.println("--------------------------------------");
			System.err.println("---------- NON CRITICAL ERROR OCCURED: ");
			e.printStackTrace();
		}
	}

	public void unitCreated(int unitID) {
		// System.out.println("UNIT CREATED " + Unit.getByID(unitID).getTypeID()
		// + "     / "
		// + UnitTypes.Protoss_Refinery.ordinal());

		// Unit unit = Unit.getMyUnitByID(unitID);
		// if (unit.getTypeID() == UnitTypes.Protoss_Refinery.ordinal()) {
		// TerranCommandCenter.sendSCVsToRefinery(unit);
		// }
		// UnitType unitType =
		// UnitType.getUnitTypeByID(Unit.getByID(unitID).getTypeID());
		// if (unitType.isBuilding()) {
		// TerranConstructing.removeIsBeingBuilt(unitType);
		// }
	}

	// =========================================================
	// Getters

	public static XVR getInstance() {
		return xvr;
	}

	public XVRClient getClient() {
		return client;
	}

	public JNIBWAPI getBwapi() {
		return bwapi;
	}

	public Map getMap() {
		return bwapi.getMap();
	}

	public static boolean isEnemyTerran() {
		return enemyTerran;
	}

	public static boolean isEnemyZerg() {
		return enemyZerg;
	}

	public static boolean isEnemyProtoss() {
		return enemyProtoss;
	}

	// =========================================================
	// UTILITIES

	public int getFrames() {
		return frameCounter;
	}

	public int getTimeSeconds() {
		return secondCounter;
	}

	public int getTimeDifferenceBetweenNowAnd(int oldTime) {
		return frameCounter - oldTime;
	}

	public void buildUnit(Unit building, UnitTypes type) {
		if (building == null || type == null) {
			return;
		}

		getBwapi().train(building.getID(), type.ordinal());
	}

	public ArrayList<Unit> getUnitsOfType(UnitTypes unitType) {
		return getUnitsOfType(unitType.ordinal());
	}

	public ArrayList<Unit> getUnitsOfType(int unitType) {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getTypeID() == unitType) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getUnitsOfTypeCompleted(UnitTypes unitType) {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getTypeID() == unitType.getID() && unit.isCompleted()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public int countUnitsOfType(UnitTypes unitType) {
		int counter = 0;

		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getTypeID() == unitType.ordinal()) {
				counter++;
			}
		}

		return counter;
	}

	public ArrayList<Unit> getUnitsNonWorker() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			if (!unit.isWorker() && unit.isCompleted()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getUnitsNonWorkerAllowIncompleted() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			if (!unit.isWorker()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getArmyUnitsIncludingDefensiveBuildings() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : getUnitsNonWorker()) {
			UnitType type = unit.getType();
			if (unit.isCompleted() && (!type.isBuilding() || unit.isDefensiveGroundBuilding())) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getEnemyArmyUnitsIncludingDefensiveBuildings() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getEnemyUnits()) {
			UnitType type = unit.getType();
			if ((!type.isBuilding() && !unit.isWorker() && !unit.getType().isLarvaOrEgg())
					|| (type.isBuilding() && unit.isDefensiveGroundBuilding())) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getWorkers() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.isWorker() && unit.isCompleted()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public int getMinerals() {
		return bwapi.getSelf().getMinerals();
	}

	public int getGas() {
		return bwapi.getSelf().getGas();
	}

	public int getSuppliesFree() {
		return (bwapi.getSelf().getSupplyTotal() - bwapi.getSelf().getSupplyUsed()) / 2;
	}

	public int getSuppliesTotal() {
		return (bwapi.getSelf().getSupplyTotal()) / 2;
	}

	public int getSuppliesUsed() {
		return getSuppliesTotal() - getSuppliesFree();
	}

	public double getDistanceBetween(Unit u1, Point point) {
		return getDistanceBetween(u1, point.x, point.y);
	}

	public double getDistanceBetween(MapPoint u1, MapPoint point) {
		if (u1 == null || point == null) {
			return -1;
		}
		return getDistanceBetween(u1.getX(), u1.getY(), point.getX(), point.getX());
	}

	public double getDistanceBetween(Unit u1, Unit u2) {
		if (u2 == null) {
			return -1;
		}
		return getDistanceBetween(u1, u2.getX(), u2.getY());
	}

	public double getDistanceBetween(MapPoint point, int x, int y) {
		if (point == null) {
			return -1;
		}
		return getDistanceBetween(point.getX(), point.getY(), x, y);
	}

	public double getDistanceBetween(int x1, int y1, int x2, int y2) {
		return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)) / 32;
	}

	public ArrayList<Unit> getMineralsUnits() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		int m1 = UnitTypes.Resource_Mineral_Field.ordinal();
		int m2 = UnitTypes.Resource_Mineral_Field_Type_2.ordinal();
		int m3 = UnitTypes.Resource_Mineral_Field_Type_3.ordinal();

		for (Unit unit : bwapi.getNeutralUnits()) {
			if (unit.getTypeID() == m1 || unit.getTypeID() == m2 || unit.getTypeID() == m3) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getGeysersUnits() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getNeutralUnits()) {
			if (unit.getTypeID() == UnitTypes.Resource_Vespene_Geyser.ordinal()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public Unit getFirstBase() {
		ArrayList<Unit> bases = getUnitsOfType(UnitManager.BASE.ordinal());
		if (!bases.isEmpty()) {
			return bases.get(0);
		} else {
			return null;
		}
	}

	public Unit getUnitOfTypeNearestTo(UnitTypes type, MapPoint closeTo) {
		if (closeTo == null) {
			return null;
		}
		return getUnitOfTypeNearestTo(type, closeTo.getX(), closeTo.getY(), false);
	}

	public Unit getUnitOfTypeNearestTo(UnitTypes type, MapPoint closeTo, boolean allowIncompleted) {
		if (closeTo == null) {
			return null;
		}
		return getUnitOfTypeNearestTo(type, closeTo.getX(), closeTo.getY(), allowIncompleted);
	}

	public Unit getUnitOfTypeNearestTo(UnitTypes type, int x, int y, boolean allowIncompleted) {
		double nearestDistance = 999999;
		Unit nearestUnit = null;

		for (Unit otherUnit : getUnitsOfType(type)) {
			if (allowIncompleted || !otherUnit.isCompleted()) {
				continue;
			}

			double distance = getDistanceBetween(otherUnit, x, y);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestUnit = otherUnit;
			}
		}

		return nearestUnit;
	}

	public boolean canAfford(int minerals) {
		return canAfford(minerals, 0, 0);
	}

	public boolean canAfford(int minerals, int gas) {
		return canAfford(minerals, gas, 0);
	}

	public boolean canAfford(int minerals, int gas, int supply) {
		if (minerals > 0 && getMinerals() < minerals) {
			return false;
		}
		if (gas > 0 && getGas() < gas) {
			return false;
		}
		if (supply > 0 && getSuppliesFree() < supply) {
			return false;
		}
		return true;
	}

	public int countUnitsOfGivenTypeInRadius(UnitTypes type, double tileRadius, MapPoint point,
			boolean onlyMyUnits) {
		if (point == null) {
			return -1;
		}
		return countUnitsOfGivenTypeInRadius(type, tileRadius, point.getX(), point.getY(),
				onlyMyUnits);
	}

	public int countUnitsEnemyOfGivenTypeInRadius(UnitTypes type, int tileRadius, MapPoint point) {
		if (point == null) {
			return -1;
		}
		int result = 0;
		for (Unit unit : bwapi.getEnemyUnits()) {
			if (type.ordinal() == unit.getTypeID() && getDistanceBetween(unit, point) <= tileRadius) {
				result++;
			}
		}
		return result;
	}

	public int countUnitsOfGivenTypeInRadius(UnitTypes type, double tileRadius, int x, int y,
			boolean onlyMyUnits) {
		int result = 0;
		Collection<Unit> unitsList = onlyMyUnits ? bwapi.getMyUnits() : bwapi.getAllUnits();
		for (Unit unit : unitsList) {
			if (type.ordinal() == unit.getTypeID() && getDistanceBetween(unit, x, y) <= tileRadius) {
				result++;
			}
		}
		return result;
	}

	public ArrayList<Unit> getUnitsOfGivenTypeInRadius(UnitTypes type, int tileRadius,
			MapPoint point, boolean onlyMyUnits) {
		if (point == null) {
			return new ArrayList<>();
		}
		return getUnitsOfGivenTypeInRadius(type, tileRadius, point.getX(), point.getY(),
				onlyMyUnits);
	}

	public ArrayList<Unit> getUnitsOfGivenTypeInRadius(UnitTypes type, int tileRadius, int x,
			int y, boolean onlyMyUnits) {
		HashMap<Unit, Double> unitToDistance = new HashMap<Unit, Double>();

		for (Unit unit : (onlyMyUnits ? bwapi.getMyUnits() : bwapi.getAllUnits())) {
			double distance = getDistanceBetween(unit, x, y);
			if (type.ordinal() == unit.getTypeID() && distance <= tileRadius) {
				unitToDistance.put(unit, distance);
			}
		}

		// Return listed sorted by distance ascending.
		ArrayList<Unit> resultList = new ArrayList<Unit>();
		resultList.addAll(RUtilities.sortByValue(unitToDistance, true).keySet());
		return resultList;
	}

	public int countUnitsInRadius(MapPoint point, int tileRadius, boolean onlyMyUnits) {
		return countUnitsInRadius(point.getX(), point.getY(), tileRadius, onlyMyUnits);
	}

	public int countUnitsOursInRadius(MapPoint point, int tileRadius) {
		// return countUnitsInRadius(point, tileRadius, bwapi.getMyUnits());
		return getUnitsInRadius(point, tileRadius, bwapi.getMyUnits()).size();
	}

	public int countUnitsEnemyInRadius(MapPoint point, double tileRadius) {
		// return countUnitsInRadius(point, tileRadius, getEnemyUnitsVisible());
		return getUnitsInRadius(point, tileRadius, getEnemyUnitsVisible()).size();
	}

	public int countUnitsInRadius(int x, int y, int tileRadius, boolean onlyMyUnits) {
		return getUnitsInRadius(new MapPointInstance(x, y), tileRadius,
				(onlyMyUnits ? bwapi.getMyUnits() : bwapi.getAllUnits())).size();
		// return countUnitsInRadius(new MapPointInstance(x, y), tileRadius,
		// (onlyMyUnits ? bwapi.getMyUnits() : bwapi.getAllUnits()));
	}

	// public int countUnitsInRadius(MapPoint point, int tileRadius,
	// Collection<Unit> units) {
	// int result = 0;
	//
	// for (Unit unit : units) {
	// if (!unit.getType().isBuilding() && getDistanceBetween(unit, point) <=
	// tileRadius) {
	// result++;
	// }
	// }
	//
	// return result;
	// }

	public ArrayList<Unit> getArmyUnitsInRadius(int x, int y, int tileRadius, boolean onlyMyUnits) {
		ArrayList<Unit> resultList = new ArrayList<Unit>();

		for (Unit unit : (onlyMyUnits ? bwapi.getMyUnits() : bwapi.getAllUnits())) {
			if (unit.getType().isArmy() && getDistanceBetween(unit, x, y) <= tileRadius) {
				resultList.add(unit);
			}
		}

		return resultList;
	}

	public int countMineralsInRadiusOf(int tileRadius, int x, int y) {
		int result = 0;
		for (Unit unit : getMineralsUnits()) {
			if (getDistanceBetween(unit, x, y) <= tileRadius) {
				result++;
			}
		}
		return result;
	}

	public ArrayList<Unit> getIdleArmyUnitsInRadiusOf(int x, int y, int tileRadius) {
		ArrayList<Unit> units = new ArrayList<Unit>();
		for (Unit unit : getUnitsNonWorker()) {
			if (unit.isIdle() && getDistanceBetween(unit, x, y) <= tileRadius) {
				units.add(unit);
			}
		}
		return units;
	}

	public ArrayList<Unit> getEnemyBuildingsVisible() {
		ArrayList<Unit> buildings = new ArrayList<Unit>();
		for (Unit unit : getBwapi().getEnemyUnits()) {
			if (unit.getType().isBuilding()) {
				buildings.add(unit);
			}
		}
		return buildings;
	}

	public Unit getUnitNearestFromList(MapPoint location, Collection<Unit> units) {
		return getUnitNearestFromList(location, units, true, true);
	}

	public Unit getUnitNearestFromList(MapPoint location, Collection<Unit> units,
			boolean includeGroundUnits, boolean includeAirUnits) {
		if (location == null) {
			return null;
		}
		return getUnitNearestFromList(location.getX(), location.getY(), units, includeGroundUnits,
				includeAirUnits);
	}

	public Unit getUnitNearestFromList(int x, int y, Collection<Unit> units,
			boolean includeGroundUnits, boolean includeAirUnits) {
		double nearestDistance = 999999;
		Unit nearestUnit = null;

		for (Unit otherUnit : units) {
			if (!otherUnit.isCompleted()) {
				continue;
			}

			UnitType type = otherUnit.getType();
			if (type.isLarvaOrEgg()) {
				continue;
			}

			boolean isAirUnit = type.isFlyer();
			if (isAirUnit && !includeAirUnits) {
				continue;
			} else if (!isAirUnit && !includeGroundUnits) {
				continue;
			}

			double distance = getDistanceBetween(otherUnit, x, y);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestUnit = otherUnit;
			}
		}

		return nearestUnit;
	}

	public ArrayList<Unit> getEnemyUnitsVisible(boolean includeGroundUnits, boolean includeAirUnits) {
		ArrayList<Unit> units = new ArrayList<Unit>();
		for (Unit unit : bwapi.getEnemyUnits()) {
			UnitType type = unit.getType();
			if (!type.isBuilding()) {
				if (!type.isFlyer() && includeGroundUnits && !type.isLarvaOrEgg()) {
					units.add(unit);
				} else if (type.isFlyer() && includeAirUnits) {
					units.add(unit);
				}
			}
		}
		return units;
	}

	public ArrayList<Unit> getEnemyAntiAirUnits() {
		ArrayList<Unit> units = new ArrayList<Unit>();
		for (Unit unit : bwapi.getEnemyUnits()) {
			UnitType type = unit.getType();
			if (type.canAirAttack()) {
				units.add(unit);
			}
		}
		return units;
	}

	public ArrayList<Unit> getEnemyUnitsVisible() {
		return getEnemyUnitsVisible(true, true);
	}

	public Collection<Unit> getEnemyArmyUnits() {
		ArrayList<Unit> armyUnits = new ArrayList<Unit>();
		for (Unit enemy : MapExploration.getEnemyUnitsDiscovered()) {
			if (!enemy.isWorker() && !enemy.getType().isBuilding()
					&& enemy.getType().getGroundAttackNormalized() > 0) {
				armyUnits.add(enemy);
			}
		}
		return armyUnits;
	}

	public Collection<Unit> getEnemyUnitsOfType(UnitTypes... types) {

		// Create set object containing all allowed types of units to return
		ArrayList<UnitTypes> typesList = new ArrayList<UnitTypes>();
		for (UnitTypes unitTypes : types) {
			typesList.add(unitTypes);
		}

		// Iterate through enemy units and check if they're types match
		ArrayList<Unit> armyUnits = new ArrayList<Unit>();
		for (UnitTypes type : types) {
			for (Unit enemy : getBwapi().getEnemyUnits()) {
				// for (Unit enemy : MapExploration.getEnemyUnitsDiscovered()) {
				if (type.getID() == enemy.getType().getID()) {
					armyUnits.add(enemy);
				}
			}
		}
		return armyUnits;
	}

	public Collection<Unit> getUnitsOurOfTypes(UnitTypes... types) {

		// Create set object containing all allowed types of units to return
		ArrayList<UnitTypes> typesList = new ArrayList<UnitTypes>();
		for (UnitTypes unitTypes : types) {
			typesList.add(unitTypes);
		}

		// Iterate through enemy units and check if they're types match
		ArrayList<Unit> units = new ArrayList<Unit>();
		for (UnitTypes type : types) {
			for (Unit unit : getBwapi().getMyUnits()) {
				// for (Unit enemy : MapExploration.getEnemyUnitsDiscovered()) {
				if (type.getID() == unit.getType().getID()) {
					units.add(unit);
				}
			}
		}
		return units;
	}

	public Unit getEnemyUnitOfType(UnitTypes... types) {

		// Create set object containing all allowed types of units to return
		ArrayList<UnitTypes> typesList = new ArrayList<UnitTypes>();
		for (UnitTypes unitTypes : types) {
			typesList.add(unitTypes);
		}

		// Iterate through enemy units and check if they're types match
		for (UnitTypes type : types) {
			for (Unit enemy : getBwapi().getEnemyUnits()) {
				// for (Unit enemy : MapExploration.getEnemyUnitsDiscovered()) {
				if (type.getID() == enemy.getType().getID()) {
					return enemy;
				}
			}
		}
		return null;
	}

	public Collection<Unit> getEnemyBuildings() {
		return MapExploration.getEnemyBuildingsDiscovered();
	}

	public int getNumberOfUnitsInRadius(Unit unit, int tileRadius, Collection<Unit> unitsList) {
		return getNumberOfUnitsInRadius(unit.getX(), unit.getY(), tileRadius, unitsList);
	}

	public int getNumberOfUnitsInRadius(int x, int y, int tileRadius, Collection<Unit> unitsList) {
		int counter = 0;

		for (Unit unit : unitsList) {
			if (getDistanceBetween(unit, x, y) <= tileRadius) {
				counter++;
			}
		}

		return counter;
	}

	public ArrayList<Unit> getUnitsInRadius(int x, int y, int tileRadius) {
		return getUnitsInRadius(new MapPointInstance(x, y), tileRadius, getAllUnits());
	}

	private ArrayList<Unit> getAllUnits() {
		ArrayList<Unit> allUnits = new ArrayList<Unit>();
		allUnits.addAll(bwapi.getAllUnits());
		return allUnits;
	}

	/** @return List of units from unitsList sorted ascending by distance. */
	public ArrayList<Unit> getUnitsInRadius(MapPoint point, double tileRadius,
			Collection<Unit> unitsList) {
		HashMap<Unit, Double> unitToDistance = new HashMap<Unit, Double>();

		for (Unit unit : unitsList) {
			double distance = getDistanceBetween(unit, point.getX(), point.getY());
			if (distance <= tileRadius) {
				unitToDistance.put(unit, distance);
			}
		}

		// Return listed sorted by distance ascending.
		ArrayList<Unit> resultList = new ArrayList<Unit>();
		resultList.addAll(RUtilities.sortByValue(unitToDistance, true).keySet());
		return resultList;
	}

	public static String getEnemyRace() {
		return xvr.ENEMY_RACE;
	}

	public static void setEnemyRace(String enemyRaceString) {
		xvr.ENEMY_RACE = enemyRaceString;

		// String enemyBotName = ENEMY.getName().toLowerCase();
		// System.out.println("BOT: " + ENEMY.getName());

		// if (MapExploration.getNumberOfStartLocations(lastInstance.getBwapi()
		// .getMap().getStartLocations()) - 1 > 1) {
		// BotStrategyManager.setExpandWithCannons(true);
		// }

		// ============
		// Protoss
		if ("Protoss".equals(xvr.ENEMY_RACE)) {
			enemyProtoss = true;
			TerranBarracks.enemyIsProtoss();
			TerranBunker.MAX_STACK++;

			// boolean shouldExpandWithCannons =
			// enemyBotName.contains("alberta");
			// boolean shouldExpandWithCannons = true;
			// BotStrategyManager.setExpandWithCannons(true);
		}

		// ============
		// Zerg
		else if ("Zerg".equals(xvr.ENEMY_RACE)) {
			enemyZerg = true;
			TerranBarracks.enemyIsZerg();
			TerranBunker.MAX_STACK++;
		}

		// ============
		// Terran
		else if ("Terran".equals(xvr.ENEMY_RACE)) {
			enemyTerran = true;
			TerranBarracks.enemyIsTerran();
		}
	}

	public Unit getRandomWorker() {
		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getTypeID() == UnitManager.WORKER.ordinal() && !unit.isConstructing()) {
				return unit;
			}
		}
		return null;
	}

	public Unit getOptimalBuilder(MapPoint buildTile) {
		ArrayList<Unit> freeWorkers = new ArrayList<Unit>();
		for (Unit worker : getWorkers()) {
			if (worker.isCompleted()
					&& !worker.isConstructing()
					&& !worker.isRepairing()
					&& !worker.isUnderAttack()
					&& (getTimeSeconds() <= 100 || (getTimeSeconds() > 100
							&& !worker.equals(WorkerManager.getProfessionalRepairer()) && !worker
								.equals(WorkerManager.getGuyToChaseOthers())))) {
				freeWorkers.add(worker);
			}
		}
		// System.out.println("freeWorkers.size() = " + freeWorkers.size());

		// Return the closest builder to the tile
		return getUnitNearestFromList(buildTile, freeWorkers, true, false);
	}

	public boolean isEnemyDetectorNear(int x, int y) {
		return getEnemyDetectorNear(x, y) != null;
	}

	public boolean isEnemyDetectorNear(MapPoint point) {
		return isEnemyDetectorNear(point.getX(), point.getY());
	}

	public Unit getEnemyDetectorNear(MapPoint point) {
		return getEnemyDetectorNear(point.getX(), point.getY());
	}

	public Unit getEnemyDetectorNear(int x, int y) {
		ArrayList<Unit> enemiesNearby = getUnitsInRadius(new MapPointInstance(x, y),
				WHAT_IS_NEAR_DISTANCE, bwapi.getEnemyUnits());
		for (Unit enemy : enemiesNearby) {
			if (enemy.isCompleted() && enemy.getType().isDetector()) {
				return enemy;
			}
		}
		return null;
	}

	public boolean isEnemyDefensiveGroundBuildingNear(MapPoint point) {
		ArrayList<Unit> enemiesNearby = getUnitsInRadius(point, 11, getEnemyBuildings());
		for (Unit enemy : enemiesNearby) {
			if (enemy.isCompleted() && enemy.getType().isAttackCapable()
					&& enemy.canAttackGroundUnits()) {
				int maxEnemyRange = enemy.getType().getGroundWeapon().getMaxRangeInTiles();
				if (point.distanceTo(enemy) <= maxEnemyRange + 4.6) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isEnemyDefensiveAirBuildingNear(MapPoint point) {
		ArrayList<Unit> enemiesNearby = getUnitsInRadius(point, WHAT_IS_NEAR_DISTANCE,
				getEnemyBuildings());
		for (Unit enemy : enemiesNearby) {
			if (enemy.isCompleted() && enemy.getType().isAttackCapable()
					&& enemy.canAttackAirUnits()) {
				int maxEnemyRange = enemy.getType().getAirWeapon().getMaxRangeInTiles();
				if (point.distanceTo(enemy) <= maxEnemyRange + 1) {
					return true;
				}
				return true;
			}
		}
		return false;
	}

	public Unit getEnemyDefensiveGroundBuildingNear(MapPoint point) {
		return getEnemyDefensiveGroundBuildingNear(point.getX(), point.getY());
	}

	public Unit getEnemyDefensiveGroundBuildingNear(int x, int y, int tileRadius) {
		ArrayList<Unit> enemiesNearby = getUnitsInRadius(new MapPointInstance(x, y),
				WHAT_IS_NEAR_DISTANCE, getEnemyBuildings());
		for (Unit enemy : enemiesNearby) {
			if (enemy.isCompleted() && enemy.getType().isAttackCapable()
					&& enemy.canAttackGroundUnits()) {
				if (getDistanceBetween(enemy, x, y) + 3.6 <= enemy.getType().getGroundWeapon()
						.getMaxRangeInTiles()) {
					return enemy;
				}
			}
		}
		return null;
	}

	public Unit getEnemyDefensiveGroundBuildingNear(int x, int y) {
		return getEnemyDefensiveGroundBuildingNear(x, y, WHAT_IS_NEAR_DISTANCE);
	}

	public Unit getEnemyDefensiveAirBuildingNear(int x, int y) {
		ArrayList<Unit> enemiesNearby = getUnitsInRadius(new MapPointInstance(x, y),
				WHAT_IS_NEAR_DISTANCE, getEnemyBuildings());
		for (Unit enemy : enemiesNearby) {
			if (enemy.isCompleted() && enemy.getType().isAttackCapable()
					&& enemy.canAttackAirUnits()) {
				return enemy;
			}
		}
		return null;
	}

	public Unit getLastBase() {
		ArrayList<Unit> bases = getUnitsOfType(UnitManager.BASE.ordinal());
		if (!bases.isEmpty()) {
			return bases.get(bases.size() - 1);
		} else {
			return null;
		}
	}

	public ArrayList<Unit> getUnitsArmy() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			UnitType type = unit.getType();
			if (!type.isBuilding() && !type.isWorker()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getUnitsArmyFlyers() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			UnitType type = unit.getType();
			if (type.isFlyer()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getUnitsArmyNonTanks() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			UnitType type = unit.getType();
			if (!type.isBuilding() && !type.isWorker() && !type.isTank() && !type.isSpiderMine()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	/** Returns Manhattan distance between two locations, expressed in tiles. */
	public int getDistanceSimple(MapPoint point1, MapPoint point2) {
		if (point1 == null || point2 == null) {
			return -1;
		}
		return (Math.abs(point1.getX() - point2.getX()) + Math.abs(point1.getY() - point2.getY())) / 32;
	}

	public Unit getBaseNearestToEnemy() {

		// Try to tell where may be some enemy base.
		Unit nearestEnemyBase = MapExploration.getNearestEnemyBase();
		if (nearestEnemyBase == null && !MapExploration.getEnemyBuildingsDiscovered().isEmpty()) {
			nearestEnemyBase = MapExploration.getEnemyBuildingsDiscovered().iterator().next();
		}

		// If we have no knowledge at all about enemy position, return the last
		// base.
		if (nearestEnemyBase == null) {
			return getLastBase();
		} else {
			Unit base = getUnitNearestFromList(nearestEnemyBase, TerranCommandCenter.getBases(),
					true, false);
			if (base.equals(getFirstBase())) {
				base = getLastBase();
			}
			return base;
		}
	}

	public boolean isEnemyInRadius(MapPoint point, int tileRadius) {
		return getNumberOfUnitsInRadius(point.getX(), point.getY(), tileRadius,
				getEnemyUnitsVisible()) > 0;
	}

	public Unit getNearestEnemyInRadius(MapPoint point, int tileRadius, boolean includeGroundUnits,
			boolean includeAirUnits) {
		Unit enemy = getUnitNearestFromList(point, bwapi.getEnemyUnits(), includeGroundUnits,
				includeAirUnits);
		if (enemy == null || getDistanceBetween(enemy, point) > tileRadius) {
			return null;
		}
		return enemy;
	}

	public Unit getNearestGroundEnemy(MapPoint point) {
		return getUnitNearestFromList(point, getEnemyUnitsVisible(), true, false);
	}

	public Unit getNearestEnemy(MapPoint point) {
		return getUnitNearestFromList(point, getEnemyUnitsVisible(), true, true);
	}

	// private Collection<Unit> getEnemyGroundUnits() {
	// List<Unit> enemyUnits = bwapi.getEnemyUnits();
	// for (Iterator<Unit> iterator = enemyUnits.iterator();
	// iterator.hasNext();) {
	// Unit unit = (Unit) iterator.next();
	// if (unit.getType().isFlyer()) {
	// iterator.remove();
	// }
	// }
	// return enemyUnits;
	// }
	//
	// @SuppressWarnings("unused")
	// private Collection<Unit> getEnemyAirUnits() {
	// List<Unit> enemyUnits = bwapi.getEnemyUnits();
	// for (Iterator<Unit> iterator = enemyUnits.iterator();
	// iterator.hasNext();) {
	// Unit unit = (Unit) iterator.next();
	// if (!unit.getType().isFlyer()) {
	// iterator.remove();
	// }
	// }
	// return enemyUnits;
	// }

	public double getNearestEnemyDistance(MapPoint point, boolean includeGroundUnits,
			boolean includeAirUnits) {
		Unit nearestEnemy = getUnitNearestFromList(point, bwapi.getEnemyUnits(),
				includeGroundUnits, includeAirUnits);
		if (nearestEnemy == null) {
			return -1;
		} else {
			return nearestEnemy.distanceTo(point);
		}
	}

	public Player getENEMY() {
		return ENEMY;
	}

	public void setENEMY(Player eNEMY) {
		xvr.ENEMY = eNEMY;
	}

	public ArrayList<Unit> getUnitsNonBuilding() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			if (!unit.getType().isBuilding() && unit.isCompleted()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public ArrayList<Unit> getUnitsBuildings() {
		ArrayList<Unit> objectsOfThisType = new ArrayList<Unit>();

		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getType().isBuilding()) {
				objectsOfThisType.add(unit);
			}
		}

		return objectsOfThisType;
	}

	public Unit getEnemyWorkerInRadius(int tileRadius, Unit explorer) {
		for (Unit enemy : getBwapi().getEnemyUnits()) {
			if (enemy.getType().isWorker()) {
				if (getDistanceBetween(explorer, enemy) <= tileRadius) {
					return enemy;
				}
			}
		}
		return null;
	}

	public Unit getEnemyWorkerRepairingInRadius(int tileRadius, Unit explorer) {
		for (Unit enemy : getBwapi().getEnemyUnits()) {
			if (enemy.getType().isWorker()) {
				if (enemy.isRepairing() && getDistanceBetween(explorer, enemy) <= tileRadius) {
					return enemy;
				}
			}
		}
		return null;
	}

	public Unit getEnemyWorkerConstructingInRadius(int tileRadius, Unit explorer) {
		for (Unit enemy : getBwapi().getEnemyUnits()) {
			if (enemy.getType().isWorker()) {
				if (enemy.isConstructing() && getDistanceBetween(explorer, enemy) <= tileRadius) {
					return enemy;
				}
			}
		}
		return null;
	}

	public Collection<Unit> getEnemyWorkersInRadius(int tileRadius, Unit explorer) {
		ArrayList<Unit> result = new ArrayList<>();

		for (Unit enemy : getBwapi().getEnemyUnits()) {
			if (enemy.getType().isWorker()) {
				if (getDistanceBetween(explorer, enemy) <= tileRadius) {
					result.add(enemy);
				}
			}
		}
		return result;
	}

	public Collection<Unit> getEnemyUnitsInRadius(int tileRadius, Unit explorer) {
		ArrayList<Unit> result = new ArrayList<>();

		for (Unit enemy : getBwapi().getEnemyUnits()) {
			if (!enemy.getType().isBuilding()) {
				if (getDistanceBetween(explorer, enemy) <= tileRadius) {
					result.add(enemy);
				}
			}
		}
		return result;
	}

	public Unit getNearestTankTo(MapPoint point) {
		Unit nearestNonSiege = getUnitOfTypeNearestTo(UnitTypes.Terran_Siege_Tank_Tank_Mode, point);
		Unit nearestSiege = getUnitOfTypeNearestTo(UnitTypes.Terran_Siege_Tank_Siege_Mode, point);
		if (nearestNonSiege == null && nearestSiege == null) {
			return null;
		}
		if (nearestNonSiege == null) {
			return nearestSiege;
		}
		if (nearestSiege == null) {
			return nearestNonSiege;
		}
		return (nearestNonSiege.distanceTo(point) < nearestSiege.distanceTo(point) ? nearestNonSiege
				: nearestSiege);
	}

	public double getNearestTankDistance(Unit unit) {
		Unit nearestTank = getNearestTankTo(unit);
		if (nearestTank == null) {
			return -1;
		} else {
			return nearestTank.distanceTo(unit);
		}
	}

	public Collection<Unit> getUnitsPossibleToHeal() {
		return getUnitsOurOfTypes(UnitTypes.Terran_Marine, UnitTypes.Terran_Firebat,
				UnitTypes.Terran_Medic, UnitTypes.Terran_Ghost, UnitTypes.Terran_SCV);
	}

}

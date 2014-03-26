package ai.terran;

import java.util.ArrayList;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.map.MapPointInstance;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;
import ai.managers.strategy.BotStrategyManager;
import ai.managers.units.UnitManager;
import ai.managers.units.buildings.BuildingManager;

public class TerranBunker {

	private static final UnitTypes type = UnitTypes.Terran_Bunker;
	private static XVR xvr = XVR.getInstance();

	private static final double MAX_DIST_FROM_CHOKE_POINT_MODIFIER = 1.8;
	public static int MAX_STACK = 1;

	private static MapPoint _placeToReinforceWithCannon = null;
	private static int _skipForTurns = 0;

	// =========================================================

	public static boolean shouldBuild() {
		if (_skipForTurns > 0) {
			_skipForTurns--;
			return false;
		}

		if (xvr.getTimeSeconds() > 200 && UnitCounter.getNumberOfBattleUnits() < 7) {
			return false;
		}

		if (UnitCounter.weHaveBuilding(TerranBarracks.getBuildingType())
				|| BuildingManager.countConstructionProgress(TerranBarracks.getBuildingType()) >= 95) {
			int maxCannonStack = calculateMaxBunkerStack();

			int bunkers = UnitCounter.getNumberOfUnits(type);
			int infantryUnits = UnitCounter.getNumberOfInfantryUnits();

			if (bunkers == 0) {
				ShouldBuildCache.cacheShouldBuildInfo(type, true);
				return true;
			}

			// if (battleUnits <= 5) {
			// ShouldBuildCache.cacheShouldBuildInfo(type, false);
			// return false;
			// }

			if (bunkers < MAX_STACK && infantryUnits >= (bunkers * 4 + 1)) {
				ShouldBuildCache.cacheShouldBuildInfo(type, true);
				return true;
			}

			if (bunkers >= TerranCommandCenter.getNumberOfUnits() * MAX_STACK) {
				ShouldBuildCache.cacheShouldBuildInfo(type, false);
				return false;
			}

			boolean weAreBuilding = Constructing.weAreBuilding(type);
			if (weAreBuilding) {
				ShouldBuildCache.cacheShouldBuildInfo(type, false);
				return false;
			}

			// // If main base isn't protected at all, build some bunkers
			// if (shouldBuildNearMainBase()) {
			// ShouldBuildCache.cacheShouldBuildInfo(type, true);
			// return true;
			// }

			if (bunkers <= maxCannonStack
					&& TerranSupplyDepot.calculateExistingPylonsStrength() >= 1.35
					&& calculateExistingBunkersStrength() < maxCannonStack) {
				ShouldBuildCache.cacheShouldBuildInfo(type, true);
				return true;
			}

			// Select one place to reinforce
			for (MapPoint base : getPlacesToReinforce()) {
				if (UnitCounter.getNumberOfUnits(UnitManager.BASE) == 1) {
					if (shouldBuildFor((MapPoint) base)) {
						ShouldBuildCache.cacheShouldBuildInfo(type, true);
						return true;
					}
				}
			}

			// // If reached here, then check if build cannon at next base
			// MapPoint tileForNextBase =
			// TerranCommandCenter.findTileForNextBase(false);
			// if (shouldBuildFor(tileForNextBase)) {
			// ShouldBuildCache.cacheShouldBuildInfo(type, true);
			// return true;
			// }
		}

		ShouldBuildCache.cacheShouldBuildInfo(type, false);
		return false;
	}

	// private static boolean shouldBuildNearMainBase() {
	// if (xvr.getTimeSeconds() < 220) {
	// return false;
	// }
	//
	// // If main base isn't protected at all, build some cannons
	// int bunkers = UnitCounter.getNumberOfUnits(type);
	// int bunkersNearMainBase = xvr.countUnitsOfGivenTypeInRadius(type, 7,
	// xvr.getFirstBase(),
	// true);
	// if (bunkers >= TerranBunker.MAX_STACK
	// && UnitCounter.getNumberOfUnits(UnitManager.BARRACKS) >= 2
	// && bunkersNearMainBase == 0) {
	// return true;
	// }
	// return false;
	// }

	private static double calculateExistingBunkersStrength() {
		double result = 0;
		UnitType unitType = UnitType.getUnitTypeByUnitTypes(type);
		int maxHitPoints = unitType.getMaxHitPoints();

		for (Unit cannon : xvr.getUnitsOfType(type)) {
			double cannonTotalHP = (double) (cannon.getHP()) / maxHitPoints;
			if (!cannon.isCompleted()) {
				cannonTotalHP = Math.sqrt(cannonTotalHP);
			}
			result += cannonTotalHP;
		}

		return result;
	}

	private static boolean shouldBuildFor(MapPoint base) {
		if (base == null) {
			return false;
		}

		// Build just at second base
		if (base.equals(xvr.getFirstBase())) {
			return false;
		}

		// Build at first base
		// if (UnitCounter.getNumberOfUnits(UnitManager.BASE) >= 2) {
		// if (base.equals(xvr.getFirstBase())) {
		// return false;
		// }
		// }

		// Get the nearest choke point to base
		ChokePoint chokePoint = MapExploration.getImportantChokePointNear(base);

		// // If this is new base, try to force building of cannon here.
		// if (!base.equals(xvr.getFirstBase())) {
		// _placeToReinforceWithCannon = base;
		// return true;
		// }

		// If in the neighborhood of choke point there's too many cannons, don't
		// build next one.
		if (shouldBuildFor(chokePoint)) {
			_placeToReinforceWithCannon = chokePoint;
			return true;
		} else {
			return false;
		}
	}

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			ShouldBuildCache.cacheShouldBuildInfo(type, true);
			for (MapPoint base : getPlacesToReinforce()) {
				tryToBuildFor(base);
			}
		}
		ShouldBuildCache.cacheShouldBuildInfo(type, false);
	}

	private static ArrayList<MapPoint> getPlacesToReinforce() {
		ArrayList<MapPoint> placesToReinforce = new ArrayList<>();

		// Second base should be one huge defensive bunker.
		placesToReinforce.add(TerranCommandCenter.getSecondBaseLocation());

		// Add bases from newest, to the oldest (I guess?)
		ArrayList<Unit> bases = TerranCommandCenter.getBases();
		for (int i = bases.size() - 1; i >= 0; i--) {
			Unit base = bases.get(i);
			ChokePoint chokePoint = MapExploration.getImportantChokePointNear(base);
			placesToReinforce.add(chokePoint);
		}

		return placesToReinforce;
	}

	private static void tryToBuildFor(MapPoint base) {
		if (shouldBuildFor(base)) {
			Constructing.construct(xvr, type);
		}
	}

	private static boolean shouldBuildFor(ChokePoint chokePoint) {
		// return findTileForCannon() != null;
		if (chokePoint.isDisabled()) {
			return false;
		}

		int numberOfDefensiveBuildingsNearby = calculateBunkersNearby(chokePoint);

		int bonus = 0;
		// if
		// (xvr.getDistanceBetween(TerranCommandCenter.getSecondBaseLocation(),
		// chokePoint) < 14) {
		// bonus = 1;
		// }

		// If there isn't too many cannons defending this choke point
		if (numberOfDefensiveBuildingsNearby < calculateMaxBunkerStack() + bonus) {
			return true;
		}

		// No, there's too many defensive buildings. Don't build next one.
		else {
			return false;
		}
	}

	public static int calculateMaxBunkerStack() {
		return BotStrategyManager.isExpandWithBunkers() ? MAX_STACK : (UnitCounter
				.getNumberOfBattleUnits() >= 8 ? 1 : MAX_STACK);
	}

	private static int calculateBunkersNearby(MapPoint mapPoint) {
		int radius;

		ChokePoint choke = null;
		if (mapPoint instanceof ChokePoint) {
			choke = (ChokePoint) mapPoint;
			radius = (int) choke.getRadius() / 32;
		} else {
			radius = 8;
		}

		int searchInDistance = (int) (1.5 * MAX_DIST_FROM_CHOKE_POINT_MODIFIER * radius);
		if (searchInDistance < 9) {
			searchInDistance = 9;
		}

		ArrayList<Unit> cannonsNearby = xvr.getUnitsOfGivenTypeInRadius(type, searchInDistance,
				mapPoint, true);

		double result = 0;
		double maxCannonHP = 200;
		for (Unit cannon : cannonsNearby) {
			// if (!cannon.isCompleted()) {
			// result -= 1;
			// }
			result += (double) cannon.getHP() / maxCannonHP;
		}

		return (int) result;
	}

	private static MapPoint findProperBuildTile(MapPoint mapPoint) {

		// Define approximate tile for cannon
		MapPoint initialBuildTile = mapPoint;

		// Define random worker, for technical reasons
		Unit workerUnit = xvr.getRandomWorker();

		// ================================
		// Define minimum and maximum distance from a choke point for a bunker
		int minimumDistance = 5;
		int numberOfBunkersNearby = calculateBunkersNearby(mapPoint);
		if (mapPoint instanceof ChokePoint) {
			ChokePoint choke = (ChokePoint) mapPoint;
			if (choke.getRadius() / 32 >= 8) {
				minimumDistance = 3;
			}
		}
		int maximumDistance = minimumDistance + (10 / Math.max(1, numberOfBunkersNearby));

		// ================================
		// Find proper build tile
		Unit nearBunker = xvr.getUnitOfTypeNearestTo(type, initialBuildTile, true);
		MapPoint properBuildTile = null;
		if (nearBunker != null && nearBunker.distanceTo(initialBuildTile) <= maximumDistance) {
			properBuildTile = Constructing.getLegitTileToBuildNear(workerUnit, type, nearBunker, 0,
					maximumDistance);
		} else {
			properBuildTile = Constructing.getLegitTileToBuildNear(workerUnit, type,
					initialBuildTile, minimumDistance, maximumDistance);
		}

		return properBuildTile;
	}

	public static MapPoint findTileForBunker() {
		MapPoint tileForBunker = null;

		// Protected main base
		// if (shouldBuildNearMainBase()) {
		// tileForCannon = findBuildTileNearMainBase();
		// } else {
		if (getNumberOfUnits() < MAX_STACK) {
			if (XVR.isEnemyZerg() && getNumberOfUnits() == 0) {
				tileForBunker = findTileAtBase(xvr.getFirstBase());
			} else {
				tileForBunker = findTileAtBase(TerranCommandCenter.getSecondBaseLocation());
			}
		} else {

			// return findProperBuildTile(_chokePointToReinforce, true);
			if (_placeToReinforceWithCannon == null) {
				_placeToReinforceWithCannon = MapExploration
						.getNearestChokePointFor(getInitialPlaceToReinforce());
			}

			// Try to find normal tile.
			tileForBunker = findProperBuildTile(_placeToReinforceWithCannon);
		}

		// }
		if (tileForBunker != null) {
			return tileForBunker;
		}

		// ===================
		// If we're here it can mean we should build bunkers at position of the
		// next base
		MapPoint tileForNextBase = TerranCommandCenter.findTileForNextBase(false);
		if (shouldBuildFor(tileForNextBase)) {
			tileForBunker = findProperBuildTile(tileForNextBase);
			if (tileForBunker != null) {
				return tileForBunker;
			}
		}

		_skipForTurns = 30;

		return null;
	}

	private static MapPoint findTileAtBase(MapPoint base) {
		if (base == null) {
			return null;
		}

		// Change first base to second base.
		// base = TerranCommandCenter.getSecondBaseLocation();
		// if (base == null) {
		// return null;
		// }

		// Find point being in the middle of way second base<->nearest choke
		// point.
		// ChokePoint choke = MapExploration.getNearestChokePointFor(base);
		ChokePoint choke = MapExploration.getImportantChokePointNear(base);
		if (choke == null) {
			return null;
		}

		// MapPointInstance location = new MapPointInstance(
		// (base.getX() + 2 * choke.getX()) / 3,
		// (base.getY() + 2 * choke.getY()) / 3);
		MapPointInstance location = MapPointInstance.getMiddlePointBetween(base, choke);

		// Find place for bunker between choke point and the second base.
		// return Constructing.getLegitTileToBuildNear(xvr.getRandomWorker(),
		// type, location, 0, 100);

		MapPoint properBuildTile = null;

		int maximumDistance = 100;
		Unit nearBunker = xvr.getUnitOfTypeNearestTo(type, location, true);
		if (getNumberOfUnits() == 1) {
			ArrayList<Unit> unitsOfType = xvr.getUnitsOfType(type);
			if (!unitsOfType.isEmpty()) {
				nearBunker = unitsOfType.get(0);
			}
		}

		// if (nearBunker == null) {
		// System.out.println("No bunker near");
		// } else {
		// System.out.println("nearBunker = " + nearBunker.toStringLocation() +
		// " / compl:"
		// + nearBunker.isCompleted());
		// }

		if (nearBunker != null && nearBunker.distanceTo(location) <= maximumDistance) {
			properBuildTile = Constructing.getLegitTileToBuildNear(type, nearBunker, 0,
					maximumDistance);
		} else {
			properBuildTile = Constructing.getLegitTileToBuildNear(type, location, 0,
					maximumDistance);
		}
		return properBuildTile;
	}

	// private static MapPoint findBuildTileNearMainBase() {
	//
	// // ===================
	// // If main base isn't protected at all, build some cannons
	// Unit firstBase = xvr.getFirstBase();
	// MapPoint point = firstBase;
	//
	// MapPoint tileForCannon =
	// Constructing.getLegitTileToBuildNear(xvr.getRandomWorker(), type,
	// point, 0, 10);
	//
	// // Debug.message(xvr, "## Build cannon for main base ##");
	// // System.out.println(" ################################ ");
	// //
	// System.out.println(" ################################ PROTECTED THE BASE");
	// // System.out.println(" ################################ ");
	// // System.out.println(tileForCannon);
	// // System.out.println();
	//
	// if (tileForCannon != null) {
	// return tileForCannon;
	// }
	// return null;
	// }

	private static MapPoint getInitialPlaceToReinforce() {
		return TerranCommandCenter.getSecondBaseLocation();
	}

	public static UnitTypes getBuildingType() {
		return type;
	}

	public static int getNumberOfUnits() {
		return UnitCounter.getNumberOfUnits(type);
	}

	public static int getNumberOfUnitsCompleted() {
		return UnitCounter.getNumberOfUnitsCompleted(type);
	}

}

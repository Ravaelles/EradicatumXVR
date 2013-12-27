package ai.terran;

import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitCounter;
import ai.managers.constructing.Constructing;
import ai.managers.constructing.ShouldBuildCache;

public class TerranMissileTurret {

	private static final UnitTypes type = UnitTypes.Terran_Missile_Turret;
	private static XVR xvr = XVR.getInstance();

	private static final int MIN_DIST_OF_TURRET_FROM_BUNKER = 1;
	private static final int MAX_DIST_OF_TURRET_FROM_BUNKER = 15;

	// ==========================================

	public static void buildIfNecessary() {
		if (shouldBuild()) {
			ShouldBuildCache.cacheShouldBuildInfo(type, true);
			Constructing.construct(xvr, type);
		}
		ShouldBuildCache.cacheShouldBuildInfo(type, false);
	}

	public static boolean shouldBuild() {
		if (!UnitCounter.weHaveBuildingFinished(UnitTypes.Terran_Engineering_Bay)
				|| Constructing.weAreBuilding(type)) {
			ShouldBuildCache.cacheShouldBuildInfo(type, false);
			return false;
		}

		int bunkers = TerranBunker.getNumberOfUnitsCompleted();
		int turrets = getNumberOfUnits();
		// System.out.println("2 --> " + bunkers + " / " + turrets);

		if (bunkers > 0 && bunkers > turrets) {
			MapPoint buildTile = findTileForTurret();
			if (buildTile != null) {
				ShouldBuildCache.cacheShouldBuildInfo(type, true);
				return true;
			}
		}

		ShouldBuildCache.cacheShouldBuildInfo(type, false);
		return false;
	}

	public static MapPoint findTileForTurret() {

		// Every bunker needs to have one Turret nearby (acting as a detector)
		for (Unit bunker : xvr.getUnitsOfType(TerranBunker.getBuildingType())) {
			if (xvr.countUnitsOfGivenTypeInRadius(type, MAX_DIST_OF_TURRET_FROM_BUNKER, bunker,
					true) == 0) {
				MapPoint tileForTurret = Constructing.getLegitTileToBuildNear(type, bunker,
						MIN_DIST_OF_TURRET_FROM_BUNKER, MAX_DIST_OF_TURRET_FROM_BUNKER);
				// System.out.println("###tile## ForTurret = " + tileForTurret +
				// " / bunker: "
				// + bunker.toStringLocation());
				return tileForTurret;
			}
		}

		return null;
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

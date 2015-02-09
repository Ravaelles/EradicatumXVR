package ai.handling.units;

import java.util.Collection;

import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapPoint;
import ai.managers.units.coordination.ArmyRendezvousManager;

public class MoveAway {

	public static void moveAwayFromEnemyOrEnemies(Unit unit) {
		Unit nearestEnemy = xvr.getNearestGroundEnemy(unit);
		if (nearestEnemy == null) {
			return;
		}

		Collection<Unit> enemyUnitsVeryNear = xvr.getEnemyUnitsInRadius(3, unit);

		// If there's no very close enemies, simply avoid this one enemy unit
		if (enemyUnitsVeryNear.isEmpty()) {
			UnitActions.moveAwayFrom(unit, nearestEnemy);
			return;
		}

		// There're some critically close enemies, it's possible we're kinda
		// stuck
		else {
			MapPoint defensivePoint = ArmyRendezvousManager.getDefensivePoint(unit);
			if (defensivePoint != null) {
				if (defensivePoint.distanceTo(unit) > 6) {
					UnitActions.attackTo(unit, defensivePoint);
					return;
				}
			}
		}

		// If nothing chosen, simply get away from nearest enemy
		UnitActions.moveAwayFrom(unit, nearestEnemy);
	}

	// =========================================================

	private static XVR xvr = XVR.getInstance();

}

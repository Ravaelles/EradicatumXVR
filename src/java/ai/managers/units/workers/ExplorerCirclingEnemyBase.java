package ai.managers.units.workers;

import jnibwapi.model.Map;
import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.handling.map.MapExploration;
import ai.handling.map.MapPoint;
import ai.handling.units.UnitActions;

public class ExplorerCirclingEnemyBase {

	private static XVR xvr = XVR.getInstance();

	private static boolean _forbidCirclingBase = false;
	private static int _circlingEnemyBasePhase = 0;
	private static Unit _lastExplorer = null;

	// =========================================================

	public static boolean tryRunningAroundEnemyBaseIfPossible() {
		if (!shouldTryThisMode()) {
			return false;
		}

		// Define enemy base
		Unit enemyBase = MapExploration.getNearestEnemyBase();

		// Made the actual move
		circleAroundEnemyBaseWith(_lastExplorer, enemyBase);

		return true;
	}

	public static void circleAroundEnemyBaseWith(Unit explorer, MapPoint aroundThisPoint) {
		// System.out.println("$$$$ " + explorer.toStringShort() + " / "
		// + enemyBase.toStringLocation());
		if (explorer == null || aroundThisPoint == null) {
			return;
		}

		int CIRCLING_RADIUS = 13;

		// Define explorer action according to the last action
		MapPoint tryGoTo = null;
		if (_circlingEnemyBasePhase <= 1) {
			tryGoTo = aroundThisPoint.getMapPoint().translateSafe(0, CIRCLING_RADIUS);
		} else if (_circlingEnemyBasePhase == 2) {
			tryGoTo = aroundThisPoint.getMapPoint().translateSafe(-CIRCLING_RADIUS, 0);
		} else if (_circlingEnemyBasePhase == 3) {
			tryGoTo = aroundThisPoint.getMapPoint().translateSafe(0, -CIRCLING_RADIUS);
		} else if (_circlingEnemyBasePhase == 4) {
			tryGoTo = aroundThisPoint.getMapPoint().translateSafe(CIRCLING_RADIUS, 0);
		}

		// Paint destination for this unit
		explorer.setPainterGoTo(tryGoTo);

		// =========================================================

		// Move to the proper point
		tryGoTo = Map.getClosestWalkablePointTo(tryGoTo);
		if (tryGoTo != null) {
			UnitActions.moveTo(explorer, tryGoTo);
			explorer.setAiOrder("Around the base!");
		}

		// if already near the point, update the circling phase
		if (explorer.distanceTo(tryGoTo) <= 3 || _circlingEnemyBasePhase == 0) {
			_circlingEnemyBasePhase++;
			if (_circlingEnemyBasePhase > 4) {
				_circlingEnemyBasePhase = 1;
			}
		}
	}

	public static boolean shouldTryThisMode() {

		// // Only allow to circle if has attacked (and is wounded)
		// if (_lastExplorer != null && !_lastExplorer.isWounded()) {
		// return false;
		// }

		// If we shouldn't be in this mode (e.g. because we get killed all the
		// time) just quit.
		if (_forbidCirclingBase) {
			return false;
		}

		// If there's no enemy bases known, quit.
		if (MapExploration.getEnemyBasesDiscovered().isEmpty()) {
			return false;
		}

		// =========================================================

		// Remember the last explorer, so we circle around only once, to avoid
		// getting killed all our workers.
		if (_lastExplorer == null) {
			_lastExplorer = ExplorerManager.getExplorer();
		}
		if (!_lastExplorer.equals(ExplorerManager.getExplorer())) {
			_forbidCirclingBase = true;
			return false;
		}

		return true;
	}

	public static boolean isCirclingOrHasCircled() {
		return _circlingEnemyBasePhase > 0;
	}

	public static int get_circlingEnemyBasePhase() {
		return _circlingEnemyBasePhase;
	}

}

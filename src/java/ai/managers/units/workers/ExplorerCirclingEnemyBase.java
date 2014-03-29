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
	private static boolean _dontAttack = false;

	private static final int CIRCLING_RADIUS = 12;

	// =========================================================

	public static void circleAroundEnemyBaseWith(Unit explorer, MapPoint aroundThisPoint) {
		// System.out.println("$$$$ " + explorer.toStringShort() + " / "
		// + enemyBase.toStringLocation());
		if (explorer == null || aroundThisPoint == null) {
			return;
		}

		// Define explorer action according to the last action
		MapPoint tryGoTo = defineGoToPointAccordingToPhase(aroundThisPoint, _circlingEnemyBasePhase);

		// Paint destination for this unit
		explorer.setPainterGoTo(tryGoTo);

		// =========================================================

		// Move to the proper point
		tryGoTo = Map.getClosestWalkablePointTo(tryGoTo);
		if (tryGoTo != null) {
			UnitActions.moveTo(explorer, tryGoTo);
			explorer.setAiOrder("Around the base! (" + _circlingEnemyBasePhase + ")");
		}

		// if already near the point, update the circling phase
		if (explorer.distanceTo(tryGoTo) <= 2 || !explorer.isMoving() || tryGoTo == null
				|| _circlingEnemyBasePhase == 0) {
			_circlingEnemyBasePhase++;
			if (_circlingEnemyBasePhase > 4) {
				_circlingEnemyBasePhase = 1;
			}
		}
	}

	// =========================================================

	public static boolean tryRunningAroundEnemyBaseIfPossible() {
		if (ExplorerManager.getExplorer() == null) {
			return false;
		}

		if (!shouldTryThisMode()) {
			return false;
		}

		if (tryAttackingIfNotWounded()) {
			return true;
		}

		// Define enemy base
		Unit enemyBase = MapExploration.getNearestEnemyBase();
		if (!enemyBase.isCompleted()) {
			return false;
		}

		// Made the actual move
		circleAroundEnemyBaseWith(ExplorerManager.getExplorer(), enemyBase);

		return true;
	}

	private static boolean tryAttackingIfNotWounded() {
		boolean foundTarget = false;
		Unit explorer = ExplorerManager.getExplorer();

		// If explorer under attack, don't attack
		if (explorer.isUnderAttack() || _dontAttack) {
			_dontAttack = true;
			return false;
		}

		// If explorer surrounded by enemy, don't attack
		if (xvr.countUnitsEnemyInRadius(explorer, 1.5) >= 3) {
			return false;
		}

		if (!explorer.isWounded()) {
			foundTarget = ExplorerManager.tryAttackingEnemyIfPossible();
			return foundTarget;
		}
		// System.out.println("HP: " + ExplorerManager.getExplorer().getHP() +
		// " (WOUNDED: "
		// + ExplorerManager.getExplorer().isWounded() + "),   FOUND TARGET:" +
		// foundTarget);

		return foundTarget;
	}

	private static MapPoint defineGoToPointAccordingToPhase(MapPoint centralPoint, int phase) {
		// double SINGLE_COORDINATE = CIRCLING_RADIUS * 1.35;

		if (_circlingEnemyBasePhase <= 1) {
			return centralPoint.getMapPoint().translateSafe(CIRCLING_RADIUS, CIRCLING_RADIUS);
		} else if (_circlingEnemyBasePhase == 2) {
			return centralPoint.getMapPoint().translateSafe(-CIRCLING_RADIUS, CIRCLING_RADIUS);
		} else if (_circlingEnemyBasePhase == 3) {
			return centralPoint.getMapPoint().translateSafe(-CIRCLING_RADIUS, -CIRCLING_RADIUS);
		} else if (_circlingEnemyBasePhase == 4) {
			return centralPoint.getMapPoint().translateSafe(CIRCLING_RADIUS, -CIRCLING_RADIUS);
		}

		// if (_circlingEnemyBasePhase == 0) {
		// return centralPoint.getMapPoint().translateSafe(CIRCLING_RADIUS,
		// CIRCLING_RADIUS);
		// } else if (_circlingEnemyBasePhase == 1) {
		// return centralPoint.getMapPoint().translateSafe(0,
		// SINGLE_COORDINATE);
		// } else if (_circlingEnemyBasePhase == 2) {
		// return centralPoint.getMapPoint().translateSafe(-CIRCLING_RADIUS,
		// CIRCLING_RADIUS);
		// } else if (_circlingEnemyBasePhase == 3) {
		// return centralPoint.getMapPoint().translateSafe(-SINGLE_COORDINATE,
		// 0);
		// } else if (_circlingEnemyBasePhase == 4) {
		// return centralPoint.getMapPoint().translateSafe(-CIRCLING_RADIUS,
		// -CIRCLING_RADIUS);
		// } else if (_circlingEnemyBasePhase == 5) {
		// return centralPoint.getMapPoint().translateSafe(0,
		// -SINGLE_COORDINATE);
		// } else if (_circlingEnemyBasePhase == 6) {
		// return centralPoint.getMapPoint().translateSafe(CIRCLING_RADIUS,
		// -CIRCLING_RADIUS);
		// } else if (_circlingEnemyBasePhase == 7) {
		// return centralPoint.getMapPoint().translateSafe(SINGLE_COORDINATE,
		// 0);
		// }

		return null;
	}

	public static boolean shouldTryThisMode() {

		// // Only allow to circle if has attacked (and is wounded)
		// if (ExplorerManager.getExplorer() != null &&
		// !ExplorerManager.getExplorer().isWounded()) {
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
		} else if (!_lastExplorer.equals(ExplorerManager.getExplorer())) {
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

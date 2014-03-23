package ai.handling.map;

import jnibwapi.model.ChokePoint;
import jnibwapi.model.Map;
import jnibwapi.model.Region;
import jnibwapi.model.Unit;
import ai.core.XVR;
import ai.utils.RUtilities;

public abstract class MapPoint {

	private static Map map;

	// ========================================

	public abstract int getX();

	public abstract int getY();

	public MapPoint getMapPoint() {
		return this;
	}

	public int getTx() {
		return getX() / 32;
	}

	public int getTy() {
		return getY() / 32;
	}

	public String toStringLocation() {
		return "[" + getTx() + ", " + getTy() + "]";
	}

	public String toString() {
		return toStringLocation();
	}

	public double distanceTo(MapPoint unit) {
		if (unit == null) {
			return -1;
		}
		return XVR.getInstance().getDistanceBetween(unit, getX(), getY());
	}

	public double distanceToChokePoint(ChokePoint choke) {
		if (choke == null) {
			return -1;
		}
		return XVR.getInstance().getDistanceBetween(choke, getX(), getY()) - choke.getRadius() / 32;
	}

	public Region getRegion() {
		return getMap().getRegion(this);
	}

	// =================

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = 1000000 + prime * result + getX();
		result = prime * result + getY();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MapPoint other = (MapPoint) obj;
		if (getX() != other.getX())
			return false;
		if (getY() != other.getY())
			return false;
		return true;
	}

	public MapPoint translate(int dx, int dy) {
		return new MapPointInstance(getX() + dx, getY() + dy);
	}

	public MapPoint translateSafe(int dTileX, int dTileY) {
		int safeX = getX() + dTileX * 32;
		int safeY = getY() + dTileY * 32;

		safeX = RUtilities.forceValueInRange(safeX, 0, getMap().getWidth() * 32 - 32);
		safeY = RUtilities.forceValueInRange(safeY, 0, getMap().getHeight() * 32 - 32);

		return new MapPointInstance(safeX, safeY);
	}

	private Map getMap() {
		if (map != null) {
			return map;
		} else {
			map = XVR.getInstance().getMap();
			return map;
		}
	}

	public boolean isWalkable() {
		return getMap().isLowResWalkable(getTx(), getTy());
	}

	public boolean isConnectedTo(Unit unit) {
		return map.isConnected(this, unit);
	}

}

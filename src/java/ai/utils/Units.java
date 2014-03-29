package ai.utils;

import java.util.ArrayList;

import jnibwapi.model.Unit;

public class Units {

	private ArrayList<Unit> list = new ArrayList<>();

	// =========================================================

	public Units() {

	}

	public Units(Unit singleUnit) {
		addUnit(singleUnit);
	}

	// =========================================================

	public void addUnit(Unit unitToAdd) {
		this.list.add(unitToAdd);
	}

	public void removeUnit(Unit unitToAdd) {
		this.list.remove(unitToAdd);
	}

	// =========================================================

	public ArrayList<Unit> getList() {
		return list;
	}

}

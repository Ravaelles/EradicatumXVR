package ai.managers.units.army;

import jnibwapi.model.Unit;
import ai.handling.army.ArmyPlacing;

public class ArmyRendezvousActions {

	public static void act(Unit unit) {
		ArmyPlacing.goToSafePlaceIfNotAlreadyThere(unit);
	}

}

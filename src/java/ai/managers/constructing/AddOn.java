package ai.managers.constructing;

import java.util.ArrayList;
import java.util.Collections;

import ai.core.XVR;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;

public class AddOn {
	
	private static XVR xvr = XVR.getInstance();
	
	public static Unit getBuildingWithNoAddOn(UnitTypes parentType) {
		ArrayList<Unit> buildings = xvr.getUnitsOfType(parentType);
		Collections.shuffle(buildings);
		
		for (Unit building : buildings) {
			if (building.getAddOnID() == -1) {
				return building;
			}
		}
		return null;
	}

}

package ai.managers.constructing;

import ai.core.XVR;
import jnibwapi.model.Unit;
import jnibwapi.types.UnitType.UnitTypes;

public class AddOn {
	
	private static XVR xvr = XVR.getInstance();
	
	public static Unit getBuildingWithNoAddOn(UnitTypes parentType) {
		for (Unit building : xvr.getUnitsOfType(parentType)) {
			if (building.getAddOnID() == -1) {
				return building;
			}
		}
		return null;
	}

}

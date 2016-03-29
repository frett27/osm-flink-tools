package org.frett27.spatialflink.tools;

import com.esri.core.geometry.MultiPath;

public class MultiPathAndRole {

	MultiPath multiPath;
	Role role;

	public MultiPathAndRole(MultiPath p, Role r) {
		this.multiPath = p;
		this.role = r;
	}

	public Role getRole() {
		return role;
	}

	public MultiPath getMultiPath() {
		return multiPath;
	}

	@Override
	public String toString() {
		StringBuffer sb = PolygonCreator.firstAndLastPoints(role.toString(), multiPath);

		return sb.toString();
	}
}
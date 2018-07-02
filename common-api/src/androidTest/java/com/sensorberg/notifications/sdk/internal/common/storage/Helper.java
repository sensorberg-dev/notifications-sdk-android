package com.sensorberg.notifications.sdk.internal.common.storage;

public class Helper {

	public static String buildDistanceQuery(double latitude, double longitude,
									  String whereParams) {
		double sin_lat_rad = Math.sin(latitude * Math.PI / 180);
		double sin_lon_rad = Math.sin(longitude * Math.PI / 180);
		double cos_lat_rad = Math.cos(latitude * Math.PI / 180);
		double cos_lon_rad = Math.cos(longitude * Math.PI / 180);
		StringBuilder a = new StringBuilder("SELECT *,");

		// @formatter:off
		a.append("(").append(sin_lat_rad).append("*\"sin_lat_rad\"+").append(cos_lat_rad)
		 .append("*\"cos_lat_rad\"*");
		a.append("(").append(cos_lon_rad).append("*\"cos_lon_rad\"+").append(sin_lon_rad)
		 .append("*\"sin_lon_rad\"))");
		a.append(" AS ").append("\"distance_acos\"");
		a.append(" FROM ").append("table_name");
		if (whereParams != null) {
			if (whereParams.trim().length() > 0) {
				a.append(" WHERE ");
				a.append(whereParams);
			}
		}
		a.append(" ORDER BY \"distance_acos\" DESC");

		return a.toString();
	}
}

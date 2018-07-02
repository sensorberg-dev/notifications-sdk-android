package com.sensorberg.notifications.sdk.internal.common.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

// https://stackoverflow.com/questions/2234204/latitude-longitude-find-nearest-latitude-longitude-complex-sql-or-complex-calc
/*
SELECT
id,
(
6371 *
acos(cos(radians(37)) *
cos(radians(lat)) *
cos(radians(lng) -
radians(-122)) +
sin(radians(37)) *
sin(radians(lat )))
) AS distance
FROM markers
HAVING distance < 100
ORDER BY distance LIMIT 100;
 */

// https://github.com/sozialhelden/wheelmap-android/wiki/Sqlite,-Distance-calculations
/*
SELECT "location",
(6371 * ACOS(SIN(RADIANS($latitude)) * SIN(RADIANS("latitude")) +
			COS(RADIANS($latitude)) * COS(RADIANS("latitude")) *
			(COS(RADIANS($longitude) - RADIANS("longitude")))) AS "distance"
FROM "locations"
HAVING "distance" < $distance
ORDER BY "distance" ASC
LIMIT 10;

SELECT "location",
(sin_lat_rad * "sin_lat_rad" + cos_lat_rad * "cos_lat_rad" *
(sin_lon_rad * "sin_lon_rad" + cos_lon_rad * "cos_lon_rad")) AS "distance_acos"
FROM "locations"
ORDER BY "distance_acos" DESC
LIMIT 10;

SELECT *, (:in_sin_lat_rad * sin_lat_rad + :in_cos_lat_rad * cos_lat_rad * (:in_sin_lon_rad * sin_lon_rad + :in_cos_lon_rad * cos_lon_rad)) AS distance_acos FROM table_geofence ORDER BY distance_acos DESC LIMIT 99

 */

@Entity(tableName = "table_geofence")
data class GeofenceQuery(@PrimaryKey val id: String,
						 val latitude: Double,
						 val longitude: Double,
						 val radius: Float,
						 val type: Trigger.Type,

						 val sin_lat_rad: Double,
						 val sin_lon_rad: Double,
						 val cos_lat_rad: Double,
						 val cos_lon_rad: Double)

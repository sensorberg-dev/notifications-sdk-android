package com.sensorberg.notifications.sdk.internal.storage

import androidx.room.*
import android.location.Location
import com.sensorberg.notifications.sdk.internal.model.GeofenceQuery
import com.sensorberg.notifications.sdk.internal.model.RegisteredGeoFence

@Dao
internal abstract class GeofenceDao {

	@Query("SELECT * FROM table_geofence")
	abstract fun getGeofences(): List<GeofenceQuery>

	@Query("SELECT *, (:in_sin_lat_rad * sin_lat_rad + :in_cos_lat_rad * cos_lat_rad * (:in_sin_lon_rad * sin_lon_rad + :in_cos_lon_rad * cos_lon_rad)) AS \"distance_acos\" FROM table_geofence ORDER BY \"distance_acos\" DESC LIMIT 99")
	abstract fun findClosestGeofences(in_sin_lat_rad: Double, in_cos_lat_rad: Double, in_sin_lon_rad: Double, in_cos_lon_rad: Double): List<GeofenceQuery>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertGeofences(geofences: List<GeofenceQuery>)

	@Query("DELETE FROM table_geofence") abstract fun clearGeofences()

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertRegisteredGeoFence(registeredGeoFence: List<RegisteredGeoFence>)

	@Query("DELETE FROM table_registered_geofences") abstract fun clearAllRegisteredGeoFences()

	@Query("SELECT * FROM table_registered_geofences WHERE id NOT IN (:list)")
	abstract fun getRemovableGeofences(list: List<String>): List<RegisteredGeoFence>

	@Transaction
	open fun clearAllAndInstertNewRegisteredGeoFences(registeredFences: List<RegisteredGeoFence>?) {
		clearAllRegisteredGeoFences()
		registeredFences?.let { insertRegisteredGeoFence(it) }
	}

	/**
	 * Because Google Play Services is restricted to 100 GeoFences which can be registered we only add 99 GeoFences (the closest ones)
	 * plus 1 extra GeoFence to tell that we are leaving this area to reprocess the next 99 GeoFences from this new Area
	 */
	@Transaction
	open fun findMostRelevantGeofences(location: Location): GeofenceQueryResult {
		val fences = findClosestGeofenceQueries(location)
		var maxDistance = 300f
		fences.forEach {
			val fenceLocation = Location("").apply {
				latitude = it.latitude
				longitude = it.longitude
			}
			maxDistance = Math.max(maxDistance, location.distanceTo(fenceLocation))
		}
		val fencesToRemove = getRemovableGeofences(fences.map { it.id }).map { it.id }
		return GeofenceQueryResult(fences.map { GeofenceMapper.mapQuery(it) }, maxDistance, fencesToRemove)
	}

	internal fun findClosestGeofenceQueries(location: Location): List<GeofenceQuery> { // for testing
		return findClosestGeofences(
				in_sin_lat_rad = Math.sin(location.latitude * Math.PI / 180),
				in_cos_lat_rad = Math.cos(location.latitude * Math.PI / 180),
				in_sin_lon_rad = Math.sin(location.longitude * Math.PI / 180),
				in_cos_lon_rad = Math.cos(location.longitude * Math.PI / 180))
	}
}
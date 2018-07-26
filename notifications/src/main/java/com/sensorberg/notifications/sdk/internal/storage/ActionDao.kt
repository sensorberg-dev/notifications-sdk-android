package com.sensorberg.notifications.sdk.internal.storage

import android.arch.persistence.room.*
import android.location.Location
import com.sensorberg.notifications.sdk.internal.model.*

@Dao
abstract class ActionDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertActions(actions: Collection<ActionModel>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertMappings(mappings: Collection<TriggerActionMap>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertTimePeriods(timePeriods: Collection<TimePeriod>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertStatistics(vararg statistics: Statistics)

	@Query("DELETE FROM table_action") abstract fun clearActions()
	@Query("DELETE FROM table_trigger_action_map") abstract fun clearMappings()
	@Query("DELETE FROM table_time_period") abstract fun clearTimePeriods()

	@Query("SELECT table_action.*, table_trigger_action_map.triggerBackendMeta FROM table_action " +
		   "INNER JOIN table_trigger_action_map ON table_trigger_action_map.actionId = table_action.id " +
		   "WHERE table_action.id IN (SELECT actionId FROM table_trigger_action_map WHERE (triggerId = :triggerId AND type IN (:types))) AND (deliverAt = 0 OR deliverAt > :now)")
	abstract fun getActionsForTrigger(triggerId: String, now: Long, vararg types: Trigger.Type): List<ActionQueryModel>

	@Query("SELECT * FROM table_statistics WHERE actionId = :actionId")
	abstract fun getStatisticsForAction(actionId: String): Statistics?

	@Query("SELECT COUNT(*) FROM table_time_period WHERE actionId = :actionId AND startsAt < :now  AND endsAt > :now")
	abstract fun getTimePeriodsForAction(actionId: String, now: Long): Long

	@Query("SELECT * FROM table_action_history")
	abstract fun getActionHistory(): List<ActionHistory>

	@Insert
	abstract fun insertActionHistory(vararg action: ActionHistory)

	@Delete
	abstract fun clearActionHistory(actions: List<ActionHistory>)

	@Query("SELECT * FROM table_action_conversion")
	abstract fun getActionConversion(): List<ActionConversion>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertActionConversion(vararg action: ActionConversion)

	@Delete
	abstract fun clearActionConversion(actions: List<ActionConversion>)
}

@Dao
abstract class GeofenceDao {

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
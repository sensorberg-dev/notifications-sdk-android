package com.sensorberg.notifications.sdk.internal.storage

import android.app.Application
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import com.sensorberg.notifications.sdk.internal.model.*

@Database(version = 1,
		  exportSchema = false,
		  entities = [
			  ActionModel::class,
			  ActionHistory::class,
			  ActionConversion::class,
			  TriggerActionMap::class,
			  GeofenceQuery::class,
			  Statistics::class,
			  TimePeriod::class,
			  RegisteredGeoFence::class,
			  BeaconEvent::class,
			  VisibleBeacons::class])
@TypeConverters(DatabaseConverters::class)
internal abstract class SdkDatabase : RoomDatabase() {

	abstract fun actionDao(): ActionDao
	abstract fun geofenceDao(): GeofenceDao
	abstract fun beaconDao(): BeaconDao

	fun insertData(timePeriods: List<TimePeriod>, actions: List<ActionModel>, mappings: List<TriggerActionMap>, geofences: List<Trigger.Geofence>) {
		runInTransaction {
			with(actionDao()) {
				clearActions()
				clearMappings()
				clearTimePeriods()
				insertActions(actions)
				insertMappings(mappings)
				insertTimePeriods(timePeriods)
			}
			with(geofenceDao()) {
				clearGeofences()
				insertGeofences(geofences.map { GeofenceMapper.mapInsert(it) })
			}
		}
	}

	companion object {
		fun createDatabase(app: Application): SdkDatabase {
			return Room.databaseBuilder(app, SdkDatabase::class.java, "notifications-sdk")
				.build()
		}
	}
}


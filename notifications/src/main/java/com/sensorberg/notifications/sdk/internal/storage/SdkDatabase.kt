package com.sensorberg.notifications.sdk.internal.storage

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.internal.model.*

@Database(version = 2,
		  exportSchema = true,
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
			  VisibleBeacons::class,
			  BeaconStorage::class,
			  DelayedActionModel::class])
@TypeConverters(DatabaseConverters::class)
internal abstract class SdkDatabase : RoomDatabase() {

	abstract fun delayedActionDao(): DelayedActionDao
	abstract fun actionDao(): ActionDao
	abstract fun geofenceDao(): GeofenceDao
	abstract fun beaconDao(): BeaconDao
	abstract fun beaconRegistrationDao(): BeaconRegistrationDao

	fun insertData(timePeriods: List<TimePeriod>, actions: List<ActionModel>, mappings: List<TriggerActionMap>, triggers: List<Trigger>) {
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
				insertGeofences(triggers.mapNotNull { if (it is Trigger.Geofence) GeofenceMapper.mapInsert(it) else null })
			}
			with(beaconRegistrationDao()) {
				delete()
				insert(triggers.mapNotNull { if (it is Trigger.Beacon) BeaconStorage.from(it) else null })
			}
		}
	}

	companion object {
		fun createDatabase(app: Application): SdkDatabase {
			return Room
				.databaseBuilder(app, SdkDatabase::class.java, "notifications-sdk")
				.addMigrations(*DatabaseMigrations.migrations)
				.build()
		}
	}
}


package com.sensorberg.notifications.sdk.internal.storage

import androidx.room.*
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.BeaconStorage
import com.sensorberg.notifications.sdk.internal.model.VisibleBeacons

@Dao
internal abstract class BeaconDao {

	@Query("SELECT * FROM table_visible_beacons WHERE id = :id") abstract fun getVisibleBeacon(id: String): VisibleBeacons?
	@Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun addBeaconVisible(beacon: VisibleBeacons)
	@Delete abstract fun removeBeaconVisible(beacon: VisibleBeacons)

	@Query("SELECT * FROM table_beacon_events WHERE beaconKey = :beaconKey")
	abstract fun getLastEventForBeacon(beaconKey: String): BeaconEvent?

	@Query("DELETE FROM table_beacon_events WHERE beaconKey = :beaconKey AND timestamp = :timestamp")
	abstract fun deleteEventForBeacon(beaconKey: String, timestamp: Long)

	@Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun addBeaconEvent(event: BeaconEvent)

}
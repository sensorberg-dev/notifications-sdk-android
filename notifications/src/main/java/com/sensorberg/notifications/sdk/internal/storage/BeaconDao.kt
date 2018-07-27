package com.sensorberg.notifications.sdk.internal.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.VisibleBeacons

@Dao
internal abstract class BeaconDao {

	fun isBeaconVisible(id: String): Boolean {
		return getBeaconCount(id) == 1
	}

	@Query("SELECT count(*) FROM table_visible_beacons WHERE id = :id") abstract fun getBeaconCount(id: String): Int
	@Insert abstract fun addBeaconVisible(beacon: VisibleBeacons)
	@Delete abstract fun removeBeaconVisible(beacon: VisibleBeacons)

	@Query("SELECT * FROM table_beacon_events WHERE beaconKey = :beaconKey ORDER BY timestamp ASC")
	abstract fun getBeaconEvents(beaconKey: String): List<BeaconEvent>

	@Insert abstract fun addBeaconEvent(event: BeaconEvent)
	@Delete abstract fun deleteBeaconEvents(events: List<BeaconEvent>)

}
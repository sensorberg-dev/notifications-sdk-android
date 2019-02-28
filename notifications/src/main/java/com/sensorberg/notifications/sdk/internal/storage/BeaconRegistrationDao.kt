package com.sensorberg.notifications.sdk.internal.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sensorberg.notifications.sdk.internal.model.BeaconStorage

@Dao
internal abstract class BeaconRegistrationDao {
	@Query("DELETE FROM table_beacons_registration") abstract fun delete()
	@Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun insert(beacons: List<BeaconStorage>)
	@Query("SELECT * FROM table_beacons_registration") abstract fun get(): List<BeaconStorage>
}
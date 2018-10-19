package com.sensorberg.notifications.sdk.internal.storage

import android.arch.persistence.room.*
import com.sensorberg.notifications.sdk.internal.model.DelayedActionModel

@Dao internal abstract class DelayedActionDao {
	@Query("SELECT * FROM table_delayed_actions WHERE instanceId = :instanceId") abstract fun get(instanceId: String): DelayedActionModel
	@Insert(onConflict = OnConflictStrategy.REPLACE) abstract fun insert(action: DelayedActionModel)
	@Delete abstract fun delete(action: DelayedActionModel)
}
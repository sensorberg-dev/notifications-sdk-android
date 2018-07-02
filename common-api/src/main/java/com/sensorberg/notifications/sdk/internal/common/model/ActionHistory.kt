package com.sensorberg.notifications.sdk.internal.common.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.location.Location
import com.sensorberg.notifications.sdk.Action
import java.util.*

@Entity(tableName = "table_action_history")
data class ActionHistory(
	var actionId: String,
	var timestamp: Long,
	@PrimaryKey var instanceId: String,
	var trigger: Trigger.Type, // trigger type is only here for compatibility with V2, it shouldn't exist
	var latitude: Double?,
	var longitude: Double?,
	var radius: Float?,
	var locationTimeStamp: Long?,
	var actionBackendMeta: String?,
	var triggerBackendMeta: String?)

fun Action.toActionHistory(type: Trigger.Type, location: Location?): ActionHistory {
	return ActionHistory(id,
						 System.currentTimeMillis(),
						 instanceId,
						 type,
						 location?.latitude,
						 location?.longitude,
						 location?.accuracy,
						 location?.time,
						 backendMeta,
						 triggerBackendMeta)
}

fun ActionQueryModel.toActionHistory(type: Trigger.Type, location: Location?): ActionHistory {
	return ActionHistory(id,
						 System.currentTimeMillis(),
						 UUID.randomUUID().toString(),
						 type,
						 location?.latitude,
						 location?.longitude,
						 location?.accuracy,
						 location?.time,
						 backendMeta,
						 triggerBackendMeta)
}
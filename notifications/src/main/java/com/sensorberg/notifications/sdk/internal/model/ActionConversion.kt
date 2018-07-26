package com.sensorberg.notifications.sdk.internal.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.location.Location
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.Conversion

@Entity(tableName = "table_action_conversion")
data class ActionConversion(
	@PrimaryKey var instanceId: String,
	var timestamp: Long,
	var value: Conversion,
	var latitude: Double?,
	var longitude: Double?,
	var radius: Float?,
	var locationTimeStamp: Long?)

fun Action.toActionConversion(conversion: Conversion, location: Location?): ActionConversion {
	return ActionConversion(instanceId,
							System.currentTimeMillis(),
							conversion,
							location?.latitude,
							location?.longitude,
							location?.accuracy,
							location?.time)
}
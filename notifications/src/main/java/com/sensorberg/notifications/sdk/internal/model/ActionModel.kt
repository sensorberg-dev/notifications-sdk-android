package com.sensorberg.notifications.sdk.internal.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "table_action")
internal data class ActionModel(
	@PrimaryKey var id: String,

		// raw data
	var backendMeta: String?,

		// data
	var subject: String?,
	var body: String?,
	var url: String?,
	var payload: String?,

		// timings
	var reportImmediately: Boolean,
	var delay: Long,
	var deliverAt: Long,

		// suppression
	var suppressionTime: Long,
	var maxCount: Int,

	var silent: Boolean)

internal data class ActionQueryModel(
	var id: String,
	var backendMeta: String?,
	var subject: String?,
	var body: String?,
	var url: String?,
	var payload: String?,
	var reportImmediately: Boolean,
	var delay: Long,
	var deliverAt: Long,
	var suppressionTime: Long,
	var maxCount: Int,
	var triggerBackendMeta: String?,
	var silent: Boolean)

@Entity(tableName = "table_trigger_action_map", indices = [(Index("triggerId"))])
internal data class TriggerActionMap(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	var triggerId: String,
	var type: Trigger.Type,
	var actionId: String,
	var triggerBackendMeta: String?)

@Entity(tableName = "table_statistics") internal data class Statistics(
	@PrimaryKey var actionId: String,
	var count: Int = 0,
	var lastExecuted: Long)

@Entity(tableName = "table_time_period")
internal data class TimePeriod(
	@PrimaryKey(autoGenerate = true) var id: Long = 0,
	var actionId: String,
	var startsAt: Long,
	var endsAt: Long)
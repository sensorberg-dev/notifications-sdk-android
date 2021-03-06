package com.sensorberg.notifications.sdk.internal.backend.backendsdkv2

import ch.hsr.geohash.GeoHash
import com.sensorberg.notifications.sdk.internal.backend.Backend
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.model.ResolveAction
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.model.ResolveResponse
import com.sensorberg.notifications.sdk.internal.model.ActionModel
import com.sensorberg.notifications.sdk.internal.model.TimePeriod
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.model.TriggerActionMap
import org.json.JSONObject
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoField
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*

private const val DEFAULT_SUPPRESSION_TIME = 0L //1L * 60L * 60L * 1000L

internal class TriggerMapper(private val callback: Backend.NotificationTriggers) : Callback<ResolveResponse> {

	override fun onResponse(call: Call<ResolveResponse>, response: Response<ResolveResponse>) {
		if (response.isSuccessful) {
			map(response.body()!!, callback)
		} else {
			Timber.e("getNotificationTriggers failed with code ${response.code()}")
			callback.onFail()
		}
	}

	override fun onFailure(call: Call<ResolveResponse>, t: Throwable) {
		Timber.e(t, "getNotificationTriggers failed")
		callback.onFail()
	}

	private fun map(response: ResolveResponse, callback: Backend.NotificationTriggers) {
		val triggers = mutableListOf<Trigger>()
		val timePeriods = mutableListOf<TimePeriod>()
		val actions = mutableListOf<ActionModel>()
		val mappings = mutableListOf<TriggerActionMap>()

		response.actions?.forEach { resolveAction ->
			val action = mapAction(resolveAction, timePeriods)
			actions.add(action)
			resolveAction.beacons.forEach { triggerId ->
				mapTrigger(triggerId, resolveAction, action)?.let {
					triggers.add(it)
					mappings.add(TriggerActionMap(
							triggerId = it.getTriggerId(),
							type = resolveAction.getTriggerType(),
							actionId = action.id,
							triggerBackendMeta = triggerId))
				}
			}
		}
		callback.onSuccess(triggers, timePeriods, actions, mappings)
	}

	private fun mapTrigger(triggerId: String, resolveAction: ResolveAction, action: ActionModel): Trigger? {
		return when (triggerId.length) {
			BEACON_ID_LENGTH -> mapBeacon(triggerId, resolveAction.getTriggerType())
			GEOFENCE_ID_LENGTH -> mapGeofence(triggerId, resolveAction.getTriggerType())
			else -> {
				Timber.w("Invalid trigger ID: $triggerId")
				null
			}
		}
	}

	private fun mapBeacon(triggerId: String, type: Trigger.Type): Trigger.Beacon {
		return Trigger.Beacon(extractUuid(triggerId),
							  extractMajor(triggerId),
							  extractMinor(triggerId),
							  type)
	}

	private fun mapGeofence(triggerId: String, type: Trigger.Type): Trigger.Geofence {
		val point = GeoHash.fromGeohashString(triggerId.substring(0, 8)).point
		val radius = triggerId.substring(8, 14).toFloat()
		return Trigger.Geofence(point.latitude, point.longitude, radius, type)
	}

	companion object {

		private const val META_PREFIX = "com.sensorberg.notifications.sdk.backend.v2.meta."
		internal const val META_ACTION_TYPE = META_PREFIX + "action_type"
		internal const val META_ACTION_TRIGGER = META_PREFIX + "action_trigger"

		private const val BEACON_ID_LENGTH = 42
		private const val GEOFENCE_ID_LENGTH = 14

		private val formatter = DateTimeFormatter.ISO_INSTANT

		fun extractUuid(triggerId: String): UUID {
			return UUID.fromString(
					triggerId.substring(0, 8) + "-" +
					triggerId.substring(8, 12) + "-" +
					triggerId.substring(12, 16) + "-" +
					triggerId.substring(16, 20) + "-" +
					triggerId.substring(20, 32))
		}

		fun extractMajor(triggerId: String): Short {
			return stringToShort(triggerId.substring(32, 37))
		}

		fun extractMinor(triggerId: String): Short {
			return stringToShort(triggerId.substring(37, 42))
		}

		fun stringToShort(value: String): Short {
			return value.toInt().toChar().toShort()
		}

		internal fun mapAction(action: ResolveAction, timePeriods: MutableList<TimePeriod>): ActionModel {
			return ActionModel(action.eid,
							   null,
							   action.content?.subject,
							   action.content?.body,
							   action.content?.url,
							   injectBackendV2MetaIntoPayload(action.content?.payload, action),
							   action.reportImmediately == true,
							   if (action.delay == null) 0 else action.delay * 1000,
							   action.deliverAt ?: 0,
							   if (action.suppressionTime == null) DEFAULT_SUPPRESSION_TIME else action.suppressionTime * 1000,
							   if (action.sendOnlyOnce == true) 1 else 0,
							   action.type == ResolveAction.TYPE_SILENT)
				.also {

					if (action.timeframes == null || action.timeframes.isEmpty()) {
						timePeriods.add(TimePeriod(
								actionId = action.eid,
								startsAt = 0,
								endsAt = Long.MAX_VALUE))
					} else {
						timePeriods.addAll(action.timeframes.mapNotNull {
							if (it.start == null || it.end == null) null
							else TimePeriod(
									actionId = action.eid,
									startsAt = it.start!!.fromIso8601(),
									endsAt = it.end!!.fromIso8601())
						})
					}
				}
		}

		private fun injectBackendV2MetaIntoPayload(payload: JSONObject?, action: ResolveAction): String {
			val json = payload ?: JSONObject()
			json.put(META_ACTION_TYPE, action.type)
			json.put(META_ACTION_TRIGGER, action.trigger)
			return json.toString()
		}

		private fun String.fromIso8601(): Long {
			return formatter
					   .parse(this)
					   .getLong(ChronoField.INSTANT_SECONDS) * 1000
		}
	}
}
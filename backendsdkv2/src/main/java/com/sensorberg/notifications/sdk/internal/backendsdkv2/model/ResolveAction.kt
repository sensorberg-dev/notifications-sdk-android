package com.sensorberg.notifications.sdk.internal.backendsdkv2.model

import com.sensorberg.notifications.sdk.internal.common.model.Trigger
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import org.json.JSONArray
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class ResolveAction(
	val eid: String,
	val trigger: Int,
	val type: Int,
	val name: String? = null,
	val beacons: List<String>,
	val suppressionTime: Long? = null, //in seconds
	val sendOnlyOnce: Boolean? = null,
	val delay: Long? = null,
	val reportImmediately: Boolean? = null,
	val content: Content? = null,
	val timeframes: List<Timeframe>? = null,
	val deliverAt: Long? = null) {

	fun getTriggerType(): Trigger.Type {
		return when (trigger) {
			ENTER -> Trigger.Type.Enter
			EXIT -> Trigger.Type.Exit
			ENTER_EXIT -> Trigger.Type.EnterOrExit
			else -> Trigger.Type.Enter // fuck it
		}
	}

	companion object {
		const val TYPE_NOTIFICATION = 1
		const val TYPE_WEBSITE = 2
		const val TYPE_IN_APP = 3
		const val TYPE_SILENT = 4

		const val ENTER = 1
		const val EXIT = 2
		const val ENTER_EXIT = 3
	}
}

internal fun Trigger.Type.getType(): Int {
	return when (this) {
		Trigger.Type.Enter -> ResolveAction.ENTER
		Trigger.Type.Exit -> ResolveAction.EXIT
		Trigger.Type.EnterOrExit -> ResolveAction.ENTER_EXIT
	}
}

@JsonClass(generateAdapter = true)
data class Content(
	val subject: String? = null,
	val body: String? = null,
	val payload: JSONObject? = null,
	val url: String? = null
				  )

internal object JsonObjectAdapter {
	@FromJson fun fromJsonString(reader: JsonReader): JSONObject {
		return jsonObject(reader.readJsonValue() as Map<String, Any>)
	}

	fun jsonObject(map: Map<String, Any>): JSONObject {
		val json = JSONObject()
		map.entries.forEach {
			when (it.value) {
				is Map<*, *> -> json.put(it.key, jsonObject(it.value as Map<String, Any>))
				is List<*> -> json.put(it.key, jsonList(it.value as List<Any>))
				else -> json.put(it.key, it.value)
			}
		}
		return json
	}

	fun jsonList(list: List<Any>): JSONArray {
		val array = JSONArray()
		list.forEach {
			when (it) {
				is Map<*, *> -> array.put(jsonObject(it as Map<String, Any>))
				is List<*> -> array.put(jsonList(it as List<Any>))
				else -> array.put(it)
			}
		}
		return array
	}

	@ToJson fun toJsonString(value: JSONObject): String {
		return value.toString()
	}
}
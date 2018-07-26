package com.sensorberg.notifications.sdk.internal.backendsdkv2.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BeaconAction(
	@Json(name = "eid") var actionId: String? = null,
	@Json(name = "dt") var timeOfPresentation: Long = 0,
	@Json(name = "trigger") var trigger: Int = 0,
	@Json(name = "pid") var pid: String? = null,
		//@Json(name = "location") var geohash: String? = null,
	@Json(name = "uuid") var actionInstanceUuid: String? = null)



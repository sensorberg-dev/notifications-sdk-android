package com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class ResolveResponse(
		//var accountProximityUUIDs: List<String>? = null,
	val actions: List<ResolveAction>? = null
		//var instantActions: List<ResolveAction>? = null,
		//@Json(name = "reportTrigger") var reportTriggerSeconds: Long? = null
								   )
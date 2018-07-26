package com.sensorberg.notifications.sdk.internal.backendsdkv2.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResolveResponse(
		//var accountProximityUUIDs: List<String>? = null,
	val actions: List<ResolveAction>? = null
		//var instantActions: List<ResolveAction>? = null,
		//@Json(name = "reportTrigger") var reportTriggerSeconds: Long? = null
						  )
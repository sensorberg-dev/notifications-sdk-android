package com.sensorberg.notifications.sdk.internal.backendsdkv2.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Timeframe(
	var start: String? = null,
	var end: String? = null)
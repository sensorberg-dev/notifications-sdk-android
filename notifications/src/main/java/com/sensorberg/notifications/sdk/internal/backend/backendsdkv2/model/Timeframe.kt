package com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Timeframe(
	var start: String? = null,
	var end: String? = null)
package com.sensorberg.notifications.sdk.internal.backendsdkv2.interceptors

import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.Transport
import okhttp3.Request

internal class ApiKey(private val apiKey: String) : AddHeader {
	override fun onHeader(builder: Request.Builder) {
		builder.header(Transport.HEADER_XAPIKEY, apiKey)
	}
}
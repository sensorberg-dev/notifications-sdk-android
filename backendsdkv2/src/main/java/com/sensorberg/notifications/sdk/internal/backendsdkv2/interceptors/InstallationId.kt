package com.sensorberg.notifications.sdk.internal.backendsdkv2.interceptors

import com.sensorberg.notifications.sdk.internal.backendsdkv2.Transport
import okhttp3.Request

class InstallationId(private val installId: String) : AddHeader {
	override fun onHeader(builder: Request.Builder) {
		builder.header(Transport.HEADER_INSTALLATION_IDENTIFIER, installId)
	}
}
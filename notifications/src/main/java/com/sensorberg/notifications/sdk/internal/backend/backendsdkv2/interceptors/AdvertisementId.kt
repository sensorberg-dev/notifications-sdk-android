package com.sensorberg.notifications.sdk.internal.backendsdkv2.interceptors

import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.Transport
import okhttp3.Request

class AdvertisementId : AddHeader {

	internal var adId: String? = null

	override fun onHeader(builder: Request.Builder) {
		adId?.let {
			builder.header(Transport.HEADER_ADVERTISER_IDENTIFIER, it)
		}
	}
}
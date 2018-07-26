package com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.interceptors

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class HeadersInterceptor(private val addHeaders: List<AddHeader>) : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val builder: Request.Builder = chain.request().newBuilder()
		addHeaders.forEach { it.onHeader(builder) }
		return chain.proceed(builder.build())
	}
}

internal interface AddHeader {
	fun onHeader(builder: Request.Builder)
}
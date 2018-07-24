package com.sensorberg.notifications.sdk.internal.backendsdkv2

import android.content.Context
import com.sensorberg.timberextensions.interceptor.CurlCommandLogger
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Transport {

	private const val CONNECTION_TIMEOUT = 12 //seconds
	private const val HTTP_RESPONSE_DISK_CACHE_MAX_SIZE = 10 * 1024L * 1024L //10MB
	internal const val HEADER_INSTALLATION_IDENTIFIER = "X-iid"
	internal const val HEADER_ADVERTISER_IDENTIFIER = "X-aid"
	internal const val HEADER_USER_AGENT = "User-Agent"
	internal const val HEADER_XAPIKEY = "X-Api-Key"

	fun createInterface(baseUrl: String, client: OkHttpClient, converter: Converter.Factory): BackendApi {
		return Retrofit.Builder()
			.baseUrl(baseUrl)
			.client(client)
			.addConverterFactory(converter)
			.callbackExecutor(Executors.newSingleThreadExecutor()) // runs on single background thread
			.build().create<BackendApi>(BackendApi::class.java)
	}

	fun createClient(context: Context, log: Boolean, interceptors: List<Interceptor>): OkHttpClient {

		val okClientBuilder = OkHttpClient.Builder()
		okClientBuilder.retryOnConnectionFailure(true)

		for (i in interceptors) {
			okClientBuilder.addNetworkInterceptor(i)
		}

		if (log) {
			val httpLog = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message ->
				Timber.v(message)
			})
			httpLog.level = HttpLoggingInterceptor.Level.BODY
			okClientBuilder.addNetworkInterceptor(httpLog)
			okClientBuilder.addNetworkInterceptor(CurlCommandLogger())
		}

		val cacheDir = File(context.cacheDir, "HttpResponseCache")
		okClientBuilder.cache(Cache(cacheDir, HTTP_RESPONSE_DISK_CACHE_MAX_SIZE))
		okClientBuilder.connectTimeout(CONNECTION_TIMEOUT.toLong(), TimeUnit.SECONDS)
		okClientBuilder.readTimeout(CONNECTION_TIMEOUT.toLong(), TimeUnit.SECONDS)
		okClientBuilder.writeTimeout(CONNECTION_TIMEOUT.toLong(), TimeUnit.SECONDS)

		return okClientBuilder.build()
	}

	fun createParser(moshi: Moshi): Converter.Factory {
		return MoshiConverterFactory.create(moshi)
	}
}
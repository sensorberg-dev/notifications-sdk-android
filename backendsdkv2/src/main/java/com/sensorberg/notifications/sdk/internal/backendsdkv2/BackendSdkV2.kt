package com.sensorberg.notifications.sdk.internal.backendsdkv2

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import com.sensorberg.notifications.sdk.internal.backendsdkv2.interceptors.*
import com.sensorberg.notifications.sdk.internal.backendsdkv2.model.HistoryBody
import com.sensorberg.notifications.sdk.internal.backendsdkv2.model.JsonObjectAdapter
import com.sensorberg.notifications.sdk.internal.common.Backend
import com.sensorberg.notifications.sdk.internal.common.model.ActionConversion
import com.sensorberg.notifications.sdk.internal.common.model.ActionHistory
import com.squareup.moshi.Moshi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class BackendSdkV2(application: Application,
				   private val apiKey: String,
				   installId: String,
				   log: Boolean) : Backend {

	private val api: BackendApi
	private val adHeader = AdvertisementId()
	private var sortedAttributes: Map<String, String> = emptyAttributes
	private val moshi: Moshi = Moshi.Builder().add(JsonObjectAdapter).build()

	init {
		AndroidThreeTen.init(application)
		val headers = listOf(
				adHeader,
				ApiKey(apiKey),
				UserAgent(application),
				InstallationId(installId))
		val interceptors = listOf(HeadersInterceptor(headers))
		val client = Transport.createClient(application, log, interceptors)
		val parser = Transport.createParser(moshi)
		api = Transport.createInterface(client, parser)
	}

	override fun getNotificationTriggers(callback: Backend.NotificationTriggers) {
		api.getTriggers(apiKey, sortedAttributes).enqueue(TriggerMapper(callback))
	}

	override fun publishHistory(actions: List<ActionHistory>, conversions: List<ActionConversion>, callback: (Boolean) -> Unit) {
		api.publishHistory(apiKey, HistoryBody.fromImplementation(actions, conversions)).enqueue(object : Callback<Void> {
			override fun onResponse(call: Call<Void>, response: Response<Void>) {
				callback.invoke(response.isSuccessful)
			}

			override fun onFailure(call: Call<Void>, t: Throwable) {
				callback.invoke(false)
			}
		})
	}

	override fun setAdvertisementId(adId: String?) {
		adHeader.adId = adId
	}

	override fun setAttributes(attributes: Map<String, String>?) {
		sortedAttributes = if (attributes == null || attributes.isEmpty()) {
			emptyAttributes
		} else {
			TreeMap<String, String>(attributes)
		}

	}

	companion object {
		private val emptyAttributes = mapOf<String, String>()
	}
}
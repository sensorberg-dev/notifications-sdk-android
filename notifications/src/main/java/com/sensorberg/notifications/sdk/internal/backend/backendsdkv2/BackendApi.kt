package com.sensorberg.notifications.sdk.internal.backend.backendsdkv2

import com.sensorberg.notifications.sdk.internal.backendsdkv2.model.HistoryBody
import com.sensorberg.notifications.sdk.internal.backendsdkv2.model.ResolveResponse
import retrofit2.Call
import retrofit2.http.*

interface BackendApi {
/*	@GET("/api/v2/sdk/gateways/{apiKey}/interactions.json")
	@Headers("Cache-Control: max-age=0")
	fun updateBeaconLayout(@Path("apiKey") apiKey: String, @QueryMap attributes: SortedMap<String, String>): Call<ResolveResponse>*/

	@GET("/api/v2/sdk/gateways/{apiKey}/interactions.json")
	@Headers("Cache-Control: max-age=0")
	fun getTriggers(@Path("apiKey") apiKey: String, @QueryMap attributes: Map<String, String>): Call<ResolveResponse>

	@POST("/api/v2/sdk/gateways/{apiKey}/analytics.json")
	fun publishHistory(@Path("apiKey") apiKey: String, @Body body: HistoryBody): Call<Void>
}
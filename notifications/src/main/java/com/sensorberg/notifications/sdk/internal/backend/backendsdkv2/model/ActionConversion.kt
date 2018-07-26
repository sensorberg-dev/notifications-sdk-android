package com.sensorberg.notifications.sdk.internal.backendsdkv2.model

import com.sensorberg.notifications.sdk.Conversion
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActionConversion(
	@Json(name = "action") var actionInstanceUuid: String? = null,
	@Json(name = "dt") var date: Long? = null,
	@Json(name = "type") var type: Int? = null
		//@Json(name = "location") var geohash: String? = null
						   ) {

	companion object {
		/**
		 * App has confirmed via [com.sensorberg.SensorbergSdk.notifyActionSuccess]  SensorbergSdk.notifyActionSuccess}
		 * that the user acknowledged the action (e.g. user opened notification).
		 */
		const val TYPE_SUCCESS = 1

		/**
		 * App has confirmed via [SensorbergSdk.notifyActionShowAttempt][com.sensorberg.SensorbergSdk.notifyActionShowAttempt]
		 * that the action was shown to  the user by notification or otherwise.
		 */
		const val TYPE_IGNORED = 0

		/**
		 * ActionModel was given to the app, but app did not return cofirmation
		 * that it made attempt to show it to the user. This is the situation where e.g.
		 * app delays showing notification to the user for whatever reason.
		 */
		const val TYPE_SUPPRESSED = -1

		/**
		 * Host app tried/wanted to show notification,
		 * but user disabled notifications for the host app
		 */
		const val TYPE_NOTIFICATION_DISABLED = -2

		fun getConversionType(type: Conversion): Int {
			return when (type) {
				Conversion.NotificationDisabled -> TYPE_NOTIFICATION_DISABLED
				Conversion.Suppressed -> TYPE_SUPPRESSED
				Conversion.Ignored -> TYPE_IGNORED
				Conversion.Success -> TYPE_SUCCESS
			}
		}
	}
}
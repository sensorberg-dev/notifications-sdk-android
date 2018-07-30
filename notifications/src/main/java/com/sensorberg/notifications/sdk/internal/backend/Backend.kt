package com.sensorberg.notifications.sdk.internal.backend

import com.sensorberg.notifications.sdk.internal.model.*

internal interface Backend {

	fun getNotificationTriggers(callback: NotificationTriggers)
	fun publishHistory(actions: List<ActionHistory>, conversions: List<ActionConversion>, callback: (Boolean) -> Unit)

	fun setAdvertisementId(adId: String?)
	fun setAttributes(attributes: Map<String, String>?)

	interface NotificationTriggers {
		fun onSuccess(triggers: List<Trigger>, timePeriods: List<TimePeriod>, actions: List<ActionModel>, mappings: List<TriggerActionMap>)
		fun onFail()
	}
}
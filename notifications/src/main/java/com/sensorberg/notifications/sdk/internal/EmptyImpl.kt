package com.sensorberg.notifications.sdk.internal

import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.Conversion
import com.sensorberg.notifications.sdk.NotificationsSdk

/**
 * Used on devices that do not support bt-le
 */
internal class EmptyImpl : NotificationsSdk {
	override fun printWorkerStates() {}

	override fun setConversion(action: Action, conversion: Conversion) {}

	override fun setAdvertisementId(adId: String?) {}

	override fun setAttributes(attributes: Map<String, String>?) {}
}
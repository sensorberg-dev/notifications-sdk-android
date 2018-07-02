package com.sensorberg.notifications.sdk

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Action(
	val id: String,
	internal val instanceId: String,
		// data
	val subject: String?,
	val body: String?,
	val url: String?,
	val payload: String?,
		// raw data
	internal var backendMeta: String?,
	internal var triggerBackendMeta: String?) : Parcelable

enum class Conversion {
	NotificationDisabled,
	Suppressed,
	Ignored,
	Success
}
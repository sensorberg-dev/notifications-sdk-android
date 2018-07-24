package com.sensorberg.notifications.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sensorberg.notifications.sdk.internal.toAction
import timber.log.Timber

abstract class AbstractActionReceiver : BroadcastReceiver() {

	abstract fun onAction(context: Context, action: Action)

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == NotificationsSdk.ACTION_RECEIVER) {
			try {
				val action = intent.toAction()
				Timber.d("Sending action to host app: $action")
				onAction(context, action)
			} catch (e: IllegalArgumentException) {
				Timber.e(e, "Failed to send action to host app, invalid data")
			}
		}
	}
}

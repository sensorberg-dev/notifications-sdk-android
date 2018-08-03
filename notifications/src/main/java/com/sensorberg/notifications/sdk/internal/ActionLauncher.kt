package com.sensorberg.notifications.sdk.internal

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.Conversion
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.model.ActionHistory
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.model.toActionConversion
import com.sensorberg.notifications.sdk.internal.model.toActionHistory
import com.sensorberg.notifications.sdk.internal.storage.ActionDao
import timber.log.Timber

internal class ActionLauncher(private val app: Application,
							  private val dao: ActionDao,
							  private val prefs: SharedPreferences) {

	private val permissionName: String = app.packageName + SDK_PERMISSION

	fun launchAction(action: Action, type: Trigger.Type) {

		val history: ActionHistory = action.toActionHistory(type, app.getLastLocation())
		dao.insertActionHistory(history)

		if (!prefs.getBoolean(NotificationsSdkImpl.PREF_ENABLED, true)) {
			dao.insertActionConversion(action.toActionConversion(Conversion.NotificationDisabled, app.getLastLocation()))
			Timber.d("Action won't be launched. Notifications SDK is disabled")
			return
		}

		Timber.d("action received to launch with ActionLauncher: $action")

		val queryResult = app.packageManager.queryBroadcastReceivers(newIntent(app), 0)
		if (queryResult != null && queryResult.isNotEmpty()) {

			val intent = newIntent(app, queryResult[0].activityInfo.name)
			action.writeToIntent(intent)
			app.sendBroadcast(intent, permissionName)
		}
	}

	companion object {
		private const val SDK_PERMISSION = ".permission.notification.sdk"

		private fun newIntent(app: Application, className: String? = null): Intent {
			return Intent().apply {
				action = NotificationsSdk.ACTION_RECEIVER
				`package` = app.packageName
				className?.let { setClassName(app, it) }
			}
		}
	}
}

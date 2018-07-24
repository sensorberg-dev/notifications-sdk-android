package com.sensorberg.notifications.sdk.internal

import android.app.Application
import android.content.Intent
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.common.model.ActionHistory
import com.sensorberg.notifications.sdk.internal.common.model.Trigger
import com.sensorberg.notifications.sdk.internal.common.model.toActionHistory
import com.sensorberg.notifications.sdk.internal.common.storage.ActionDao
import timber.log.Timber

class ActionLauncher(private val app: Application, private val dao: ActionDao) {

	private val permissionName: String = app.packageName + SDK_PERMISSION

	fun launchAction(action: Action, type: Trigger.Type) {

		val history: ActionHistory = action.toActionHistory(type, app.getLastLocation())
		dao.insertActionHistory(history)

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

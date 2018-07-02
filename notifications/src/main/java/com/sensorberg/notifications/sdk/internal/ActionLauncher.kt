package com.sensorberg.notifications.sdk.internal

import android.app.Application
import android.content.Intent
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.common.model.ActionHistory
import com.sensorberg.notifications.sdk.internal.common.model.Trigger
import com.sensorberg.notifications.sdk.internal.common.model.toActionHistory
import com.sensorberg.notifications.sdk.internal.common.storage.ActionDao

class ActionLauncher(private val app: Application, private val dao: ActionDao) {

	fun launchAction(action: Action, type: Trigger.Type) {

		val history: ActionHistory = action.toActionHistory(type, app.getLastLocation())
		dao.insertActionHistory(history)

		val intent = Intent(NotificationsSdk.ACTION_PRESENT)
		intent.setPackage(app.packageName)
		action.writeToIntent(intent)
		app.sendBroadcast(intent)

	}
}

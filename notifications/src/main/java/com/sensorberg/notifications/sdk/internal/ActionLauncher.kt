package com.sensorberg.notifications.sdk.internal

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.common.model.ActionHistory
import com.sensorberg.notifications.sdk.internal.common.model.Trigger
import com.sensorberg.notifications.sdk.internal.common.model.toActionHistory
import com.sensorberg.notifications.sdk.internal.common.storage.ActionDao
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class ActionLauncher(private val app: Application, private val dao: ActionDao) : KoinComponent {

	private val actionListener: NotificationsSdk.OnActionListener by inject(InjectionModule.actionListenerBean)

	fun launchAction(action: Action, type: Trigger.Type) {

		val history: ActionHistory = action.toActionHistory(type, app.getLastLocation())
		dao.insertActionHistory(history)

		Timber.d("action received to launch with ActionLauncher: $action")
		Handler(Looper.getMainLooper()).post {
			actionListener.onActionReceived(action)
		}

	}
}

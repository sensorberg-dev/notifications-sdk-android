package com.sensorberg.notifications.sdk.sample

import android.content.Context
import android.widget.Toast
import com.sensorberg.notifications.sdk.AbstractActionReceiver
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.Conversion
import timber.log.Timber

class ActionReceiver : AbstractActionReceiver() {

	override fun onAction(context: Context, action: Action) {
		Timber.i("Action received by the application. $action")
		Toast.makeText(context, action.subject ?: action.toString(), Toast.LENGTH_SHORT).show()
		(context.applicationContext as App).sdk.setConversion(action, Conversion.Ignored)

	}
}
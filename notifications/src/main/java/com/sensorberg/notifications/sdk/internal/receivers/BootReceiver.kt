package com.sensorberg.notifications.sdk.internal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sensorberg.notifications.sdk.internal.work.GeofenceWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

	private val workUtils: WorkUtils by inject()

	// TODO: add to mainfest on boot
	override fun onReceive(context: Context, intent: Intent) {
		workUtils.execute(GeofenceWork::class.java, WorkUtils.FENCE_WORK)
	}
}
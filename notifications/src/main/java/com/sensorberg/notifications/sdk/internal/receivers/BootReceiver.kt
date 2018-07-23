package com.sensorberg.notifications.sdk.internal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.common.storage.ActionDao
import com.sensorberg.notifications.sdk.internal.work.GeofenceWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor

class BootReceiver : BroadcastReceiver(), KoinComponent {

	private val dao: ActionDao by inject()
	private val workUtils: WorkUtils by inject()
	private val executor: Executor by inject(InjectionModule.executorBean)

	override fun onReceive(context: Context, intent: Intent) {
		if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
			"android.intent.action.QUICKBOOT_POWERON" == intent.action) {
			Log.i("NotificationSDK", "On Boot received")
			val pending = goAsync() // using async because of the DB operation
			executor.execute {
				// TODO: clear current registered fences
				workUtils.execute(GeofenceWork::class.java)
				pending.finish()
			}
		}
	}
}
package com.sensorberg.notifications.sdk.internal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.storage.GeofenceDao
import com.sensorberg.notifications.sdk.internal.work.GeofenceWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor

class BootReceiver : BroadcastReceiver(), KoinComponent {

	private val dao: GeofenceDao by inject()
	private val workUtils: WorkUtils by inject()
	private val executor: Executor by inject(InjectionModule.executorBean)

	override fun onReceive(context: Context, intent: Intent) {
		if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
			"android.intent.action.QUICKBOOT_POWERON" == intent.action) {
			Timber.i("On Boot received")
			val pending = goAsync() // using async because of the DB operation
			executor.execute {
				dao.clearAllAndInstertNewRegisteredGeoFences(null)
				workUtils.execute(GeofenceWork::class.java)
				pending.finish()
			}
		}
	}
}
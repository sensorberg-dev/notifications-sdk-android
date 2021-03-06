package com.sensorberg.notifications.sdk.internal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.async
import com.sensorberg.notifications.sdk.internal.storage.GeofenceDao
import com.sensorberg.notifications.sdk.internal.work.GeofenceWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.sensorberg.notifications.sdk.internal.NotificationSdkComponent
import org.koin.core.inject
import timber.log.Timber
import java.util.concurrent.Executor

class BootReceiver : BroadcastReceiver(), NotificationSdkComponent {

	private val dao: GeofenceDao by inject()
	private val workUtils: WorkUtils by inject()
	private val executor: Executor by inject()
	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun onReceive(context: Context, intent: Intent) {
		if (!sdkEnableHandler.isEnabled()) return
		if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
			"android.intent.action.QUICKBOOT_POWERON" == intent.action) {
			Timber.i("On Boot received. $intent")
			async(executor) {
				dao.clearAllAndInstertNewRegisteredGeoFences(null)
				workUtils.execute(GeofenceWork::class.java)
			}
		}
	}
}
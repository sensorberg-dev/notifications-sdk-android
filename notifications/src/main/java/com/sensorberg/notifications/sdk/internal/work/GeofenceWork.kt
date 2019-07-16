package com.sensorberg.notifications.sdk.internal.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.work.delegate.GeofenceRegistration
import com.sensorberg.notifications.sdk.internal.NotificationSdkComponent
import org.koin.core.inject

internal class GeofenceWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), NotificationSdkComponent {

	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun doWork(): Result {
		if (!sdkEnableHandler.isEnabled()) return Result.failure()
		logStart()
		return if (GeofenceRegistration().execute() == Result.success()) {
			Result.success()
		} else {
			// for geofence re-registration we want this to keep retrying until it succeeds
			Result.retry()
		}
	}
}
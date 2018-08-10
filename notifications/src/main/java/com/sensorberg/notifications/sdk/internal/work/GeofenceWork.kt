package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.work.delegate.GeofenceRegistration
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class GeofenceWork : Worker(), KoinComponent {

	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun doWork(): Result {
		if (!sdkEnableHandler.isEnabled()) return Result.FAILURE
		logStart()
		return if (GeofenceRegistration().execute() == Worker.Result.SUCCESS) {
			Worker.Result.SUCCESS
		} else {
			// for geofence re-registration we want this to keep retrying until it succeeds
			Worker.Result.RETRY
		}
	}
}
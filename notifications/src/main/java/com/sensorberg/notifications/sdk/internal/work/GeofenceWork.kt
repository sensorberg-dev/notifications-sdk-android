package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.registration.GeofenceRegistration
import org.koin.standalone.KoinComponent
import timber.log.Timber

class GeofenceWork : Worker(), KoinComponent {
	override fun doWork(): Result {
		logStart()
		return if (GeofenceRegistration().execute() == Worker.Result.SUCCESS) {
			Worker.Result.SUCCESS
		} else {
			// for geofence re-registration we want this to keep retrying until it succeeds
			Worker.Result.RETRY
		}
	}
}
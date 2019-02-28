package com.sensorberg.notifications.sdk.internal.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.work.delegate.BeaconRegistration
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class BeaconWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), KoinComponent {

	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun doWork(): Result {
		if (!sdkEnableHandler.isEnabled()) return Result.failure()
		logStart()
		return if (BeaconRegistration().execute() == Result.success()) {
			Result.success()
		} else {
			// for beacon registration we want this to keep retrying until it succeeds
			Result.retry()
		}
	}
}
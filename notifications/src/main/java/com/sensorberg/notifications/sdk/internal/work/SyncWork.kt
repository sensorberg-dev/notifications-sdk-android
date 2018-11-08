package com.sensorberg.notifications.sdk.internal.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.logResult
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.work.delegate.SyncDelegate
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class SyncWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), KoinComponent {

	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun doWork(): Result {
		if (!sdkEnableHandler.isEnabled()) return Result.FAILURE
		logStart()
		val result = SyncDelegate().execute()
		return logResult(result)
	}
}
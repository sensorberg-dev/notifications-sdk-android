package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.logResult
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.work.delegate.SyncDelegate
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class SyncWork : Worker(), KoinComponent {

	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun doWork(): Result {
		if (!sdkEnableHandler.isEnabled()) return Result.FAILURE
		logStart()
		val result = SyncDelegate().execute()
		return logResult(result)
	}
}
package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.ActionLauncher
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.model.DelayedActionModel
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class FireActionWork : Worker(), KoinComponent {

	internal val database: SdkDatabase by inject()
	private val workUtils: WorkUtils by inject()
	private val actionLauncher: ActionLauncher by inject()
	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun doWork(): Result {
		val action = getAction()
		if (!sdkEnableHandler.isEnabled()) {
			database.delayedActionDao().delete(DelayedActionModel.fromAction(action))
			return Result.FAILURE
		}
		val triggerType = getTriggerType()
		val reportImmediate = isReportImmediate()
		actionLauncher.launchAction(action, triggerType)
		if (reportImmediate) {
			workUtils.executeAndSchedule(UploadWork::class.java)
		}
		database.delayedActionDao().delete(DelayedActionModel.fromAction(action))
		return Result.SUCCESS
	}
}
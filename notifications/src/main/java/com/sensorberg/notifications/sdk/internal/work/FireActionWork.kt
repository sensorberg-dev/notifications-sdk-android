package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.ActionLauncher
import com.squareup.moshi.Moshi
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class FireActionWork : Worker(), KoinComponent {

	private val moshi: Moshi by inject()
	private val workUtils: WorkUtils by inject()
	private val actionLauncher: ActionLauncher by inject()

	override fun doWork(): Result {
		val action = getAction(WorkUtils.createAction(moshi))
		val triggerType = getTriggerType()
		val reportImmediate = isReportImmediate()
		actionLauncher.launchAction(action, triggerType)
		if (reportImmediate) {
			workUtils.execute(UploadWork::class.java, WorkUtils.UPLOAD_WORK)
		}
		return Result.SUCCESS
	}
}
package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.ActionDao
import com.sensorberg.notifications.sdk.internal.backend.Backend
import com.sensorberg.notifications.sdk.internal.model.ActionConversion
import com.sensorberg.notifications.sdk.internal.model.ActionHistory
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Exchanger
import java.util.concurrent.TimeUnit

class UploadWork : Worker(), KoinComponent {

	private val dao: ActionDao by inject()
	private val backend: Backend by inject()
	private val backendExecution = Exchanger<Result>()

	override fun doWork(): Result {

		val actions: List<ActionHistory> = dao.getActionHistory()
		val conversions: List<ActionConversion> = dao.getActionConversion()
		if (actions.isEmpty() && conversions.isEmpty()) {
			Timber.d("Nothing to upload")
			return Result.SUCCESS
		}

		Timber.d("Executing upload work for ${actions.size} actions triggered and ${conversions.size} conversion data")

		backend.publishHistory(actions, conversions) {
			val result = if (it) Result.SUCCESS else Result.RETRY
			backendExecution.exchange(result)
		}

		return try {
			val result = backendExecution.exchange(null, 30, TimeUnit.SECONDS)
			if (result == Result.SUCCESS) {
				dao.clearActionHistory(actions)
				dao.clearActionConversion(conversions)
			}
			Timber.d("Upload work execution ${result.name}")
			result
		} catch (e: Exception) {
			Timber.w(e, "Upload work execution failed")
			Result.RETRY
		}
	}
}
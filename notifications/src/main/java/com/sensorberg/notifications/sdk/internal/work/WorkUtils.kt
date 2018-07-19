package com.sensorberg.notifications.sdk.internal.work

import android.app.Application
import androidx.work.*
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.BuildConfig
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.common.model.Trigger
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.isGooglePlayServicesAvailable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WorkUtils(private val workManager: WorkManager, private val app: Application, moshi: Moshi) {

	private val actionAdapter: JsonAdapter<Action> by lazy { createAction(moshi) }

	fun sendDelayedAction(action: Action, type: Trigger.Type, reportImmediately: Boolean, delay: Long) {
		Timber.d("Scheduling execution of action in ${delay / 1000L} seconds. ${action.subject}")
		val data = Data.Builder()
			.putString(ACTION_STRING, actionAdapter.toJson(action))
			.putString(TRIGGER_TYPE, type.name)
			.putBoolean(REPORT_IMMEDIATE, reportImmediately)
			.build()
		val request = createDelayedActionRequest(delay, data)
		workManager.enqueue(request)
	}

	private fun createDelayedActionRequest(delay: Long, data: Data): OneTimeWorkRequest {
		return OneTimeWorkRequestBuilder<FireActionWork>()
			.setInitialDelay(delay, TimeUnit.MILLISECONDS)
			.setInputData(data)
			.addTag(WORKER_TAG) //only to get the workers states later
			.addTag(FIRE_ACTION_WORK)
			.build()
	}

	fun execute(klazz: Class<out Worker>) {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return
		}

		val request = createExecuteRequest(klazz)
		Timber.d("Enqueueing for immediate execution of ${klazz.simpleName}")
		workManager.enqueue(request)
	}

	private fun createExecuteRequest(klazz: Class<out Worker>): OneTimeWorkRequest {
		return OneTimeWorkRequest.Builder(klazz)
			.setConstraints(getConstraints())
			.addTag(WORKER_TAG) //only to get the workers states later
			.build()
	}

	fun schedule(klazz: Class<out Worker>): Worker.Result {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return Worker.Result.RETRY
		}

		val request = createScheduleRequest(klazz)

		Timber.d("Enqueueing periodic sync of ${klazz.simpleName} with id: ${request.id}")
		workManager.enqueueUniquePeriodicWork(klazz.canonicalName, ExistingPeriodicWorkPolicy.KEEP, request)

		return Worker.Result.SUCCESS
	}

	private fun createScheduleRequest(klazz: Class<out Worker>): PeriodicWorkRequest {
		val interval = if (BuildConfig.DEBUG) PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS else TimeUnit.HOURS.toMillis(3)
		return PeriodicWorkRequest.Builder(klazz, interval, TimeUnit.MILLISECONDS)
			.addTag(WORKER_TAG) //only to get the workers states later
			.setConstraints(getConstraints())
			.build()
	}

	private fun getConstraints(): Constraints {
		return Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
	}

	fun cancelAllWorkByTag(tag: String) {
		workManager.cancelAllWorkByTag(tag)
	}

	companion object {
		internal val ACTION_STRING = NotificationsSdk.ACTION_PRESENT
		internal val FIRE_ACTION_WORK = "${FireActionWork::class.java.canonicalName!!}.ACTION"
		internal val REPORT_IMMEDIATE = "${FireActionWork::class.java.canonicalName!!}.REPORT_IMMEDIATE"
		internal val TRIGGER_TYPE = "${FireActionWork::class.java.canonicalName!!}.TRIGGER_TYPE"

		internal const val WORKER_TAG = "com.sensorberg.notifications.sdk.internal.work.WORKER_TAG"

		fun createAction(moshi: Moshi): JsonAdapter<Action> {
			return moshi.adapter<Action>(Action::class.java)
		}
	}
}

internal fun FireActionWork.getAction(actionAdapter: JsonAdapter<Action>): Action {
	return actionAdapter.fromJson(inputData.getString(WorkUtils.ACTION_STRING, null)!!)!!
}

internal fun FireActionWork.getTriggerType(): Trigger.Type {
	return Trigger.Type.valueOf(inputData.getString(WorkUtils.TRIGGER_TYPE, null)!!)
}

internal fun FireActionWork.isReportImmediate(): Boolean {
	return inputData.getBoolean(WorkUtils.REPORT_IMMEDIATE, false)
}
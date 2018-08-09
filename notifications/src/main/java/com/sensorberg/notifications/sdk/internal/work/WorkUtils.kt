package com.sensorberg.notifications.sdk.internal.work

import android.app.Application
import android.content.SharedPreferences
import androidx.work.*
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.BuildConfig
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.isGooglePlayServicesAvailable
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.set
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import timber.log.Timber
import java.util.concurrent.TimeUnit

internal class WorkUtils(private val workManager: WorkManager, private val app: Application, moshi: Moshi, private val prefs: SharedPreferences) {

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

	fun executeBeaconWorkFor(key: String, type: Trigger.Type) {
		val time = if (type == Trigger.Type.Enter) 10L else 180L
		Timber.d("Scheduling execution of the beacon work in $time seconds for beacon $key")
		val data = Data.Builder()
			.putString(BEACON_STRING, key)
			.build()
		val request = OneTimeWorkRequestBuilder<BeaconProcessingWork>()
			.setInitialDelay(time, TimeUnit.SECONDS)
			.setInputData(data)
			.addTag(WORKER_TAG) //only to get the workers states later
			.build()
		workManager.beginUniqueWork("beacon_work_$key", ExistingWorkPolicy.REPLACE, request).enqueue()
	}

	fun execute(klazz: Class<out Worker>, extras: String? = null) {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return
		}

		val data = extras?.let {
			Data.Builder()
				.putString(EXTRA_DATA, extras)
				.build()
		}

		val request = createExecuteRequest(klazz, data)
		Timber.d("Enqueueing for immediate execution of ${klazz.simpleName}")
		workManager.beginUniqueWork(klazz.canonicalName, ExistingWorkPolicy.REPLACE, request).enqueue()
	}

	private fun createExecuteRequest(klazz: Class<out Worker>, data: Data?): OneTimeWorkRequest {
		return OneTimeWorkRequest.Builder(klazz)
			.setConstraints(getConstraints())
			.apply { data?.let { setInputData(data) } }
			.addTag(WORKER_TAG) //only to get the workers states later
			.build()
	}

	fun schedule(klazz: Class<out Worker>) {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return
		}

		val request = createScheduleRequest(klazz)

		Timber.d("Enqueueing periodic sync of ${klazz.simpleName} with id: ${request.id}")
		val policy = if (prefs.set("sdkVersion_${klazz.canonicalName}", BuildConfig.VERSION_NAME)) {
			// if new SDK version, replace previous work
			ExistingPeriodicWorkPolicy.REPLACE
		} else {
			ExistingPeriodicWorkPolicy.KEEP
		}
		workManager.enqueueUniquePeriodicWork(klazz.canonicalName, policy, request)
	}

	private fun createScheduleRequest(klazz: Class<out Worker>): PeriodicWorkRequest {
		return PeriodicWorkRequest
			.Builder(klazz, 8, TimeUnit.HOURS)
			.addTag(WORKER_TAG) //only to get the workers states later
			.setConstraints(getConstraints())
			.build()
	}

	private fun getConstraints(): Constraints {
		return Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
	}

	fun disableAll() {
		workManager.cancelAllWorkByTag(WORKER_TAG)
	}

	companion object {
		internal const val ACTION_STRING = "com.sensorberg.notifications.sdk.internal.work.ACTION_STRING"
		internal const val FIRE_ACTION_WORK = "com.sensorberg.notifications.sdk.internal.work.fireAction.ACTION"
		internal const val REPORT_IMMEDIATE = "com.sensorberg.notifications.sdk.internal.work.fireAction.REPORT_IMMEDIATE"
		internal const val TRIGGER_TYPE = "com.sensorberg.notifications.sdk.internal.work.fireAction.TRIGGER_TYPE"
		internal const val EXTRA_DATA = "com.sensorberg.notifications.sdk.internal.work.EXTRA_DATA"
		internal const val WORKER_TAG = "com.sensorberg.notifications.sdk.internal.work.WORKER_TAG"
		internal const val BEACON_STRING = "com.sensorberg.notifications.sdk.internal.work.BEACON_STRING"

		fun createAction(moshi: Moshi): JsonAdapter<Action> {
			return moshi.adapter<Action>(Action::class.java)
		}
	}
}

internal fun BeaconProcessingWork.getBeaconKey(): String {
	return inputData.getString(WorkUtils.BEACON_STRING)!!
}

internal fun Data.getExtras(): String {
	return getString(WorkUtils.EXTRA_DATA)!!
}

internal fun FireActionWork.getAction(actionAdapter: JsonAdapter<Action>): Action {
	return actionAdapter.fromJson(inputData.getString(WorkUtils.ACTION_STRING)!!)!!
}

internal fun FireActionWork.getTriggerType(): Trigger.Type {
	return Trigger.Type.valueOf(inputData.getString(WorkUtils.TRIGGER_TYPE)!!)
}

internal fun FireActionWork.isReportImmediate(): Boolean {
	return inputData.getBoolean(WorkUtils.REPORT_IMMEDIATE, false)
}
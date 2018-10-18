package com.sensorberg.notifications.sdk.internal.work

import android.app.Application
import android.content.SharedPreferences
import androidx.work.*
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.isGooglePlayServicesAvailable
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
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

	fun executeBeaconWork(beaconKey: String) {
		val time = 3L
		Timber.d("Scheduling execution of the beacon work in $time minutes for beacon $beaconKey")
		val data = Data.Builder()
			.putString(BEACON_STRING, beaconKey)
			.build()
		val request = OneTimeWorkRequestBuilder<BeaconProcessingWork>()
			.setInitialDelay(time, TimeUnit.MINUTES)
			.setInputData(data)
			.addTag(WORKER_TAG) //only to get the workers states later
			.build()
		workManager.beginUniqueWork("beacon_work_$beaconKey", ExistingWorkPolicy.REPLACE, request).enqueue()
	}

	fun cancelBeaconWork(beaconKey: String) {
		workManager.cancelUniqueWork("beacon_work_$beaconKey")
	}

	fun execute(klazz: Class<out Worker>) {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return
		}

		val request = createExecuteRequest(klazz, false)
		Timber.d("Enqueueing for immediate execution of ${klazz.simpleName}")
		workManager.beginUniqueWork("execute_${klazz.canonicalName}", ExistingWorkPolicy.REPLACE, request).enqueue()
	}

	private fun createExecuteRequest(klazz: Class<out Worker>, needsNetwork: Boolean): OneTimeWorkRequest {
		return OneTimeWorkRequest.Builder(klazz)
			.setConstraints(if (needsNetwork) getConstraints() else Constraints.NONE)
			.addTag(WORKER_TAG) //only to get the workers states later
			.build()
	}

	fun executeAndSchedule(klazz: Class<out Worker>) {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return
		}
		Timber.d("Enqueueing for immediate execution of ${klazz.simpleName} and then schedule for periodic")
		workManager
			.beginUniqueWork(klazz.canonicalName, ExistingWorkPolicy.REPLACE, createExecuteRequest(klazz, true))
			.then(reschedule(klazz))
			.enqueue()
	}

	fun schedule(klazz: Class<out Worker>) {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return
		}
		val request = createScheduleRequest(klazz)
		Timber.d("Enqueueing periodic sync of ${klazz.simpleName} with id: ${request.id}")
		workManager.enqueueUniquePeriodicWork(klazz.canonicalName, ExistingPeriodicWorkPolicy.REPLACE, request)
	}

	private fun createScheduleRequest(klazz: Class<out Worker>): PeriodicWorkRequest {
		return PeriodicWorkRequest
			.Builder(klazz, 12, TimeUnit.HOURS, 4, TimeUnit.HOURS)
			.addTag(WORKER_TAG) //only to get the workers states later
			.setConstraints(getConstraints())
			.build()
	}

	private fun getConstraints(): Constraints {
		return Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
	}

	private fun reschedule(klazz: Class<out Worker>): OneTimeWorkRequest {
		return OneTimeWorkRequest
			.Builder(Rescheduler::class.java)
			.setConstraints(Constraints.NONE)
			.setInputData(Data.Builder().putString("klazz", klazz.canonicalName).build())
			.addTag(WORKER_TAG)
			.build()
	}

	class Rescheduler : Worker(), KoinComponent {

		private val workUtils: WorkUtils by inject()
		override fun doWork(): Result {
			val className = inputData.getString("klazz")!!
			workUtils.schedule(Class.forName(className) as Class<out Worker>)
			return Result.SUCCESS
		}
	}

	fun disableAll() {
		disableAlLWorkers()
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

		fun disableAlLWorkers() {
			WorkManager.getInstance().cancelAllWorkByTag(WORKER_TAG)
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
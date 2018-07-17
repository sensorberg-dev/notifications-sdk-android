package com.sensorberg.notifications.sdk.internal.work

import android.app.Application
import android.os.Build
import androidx.work.*
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.common.model.Trigger
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.isGooglePlayServicesAvailable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WorkUtils(private val workManager: WorkManager, private val app: Application, moshi: Moshi) {

	private val actionAdapter: JsonAdapter<Action> by lazy { createAction(moshi) }

	fun fireDelayedAction(action: Action, type: Trigger.Type, reportImmediately: Boolean, delay: Long) {
		Timber.d("Scheduling execution of action in ${delay / 1000L} seconds. ${action.subject}")
		val data = Data.Builder()
			.putString(ACTION_STRING, actionAdapter.toJson(action))
			.putString(TRIGGER_TYPE, type.name)
			.putBoolean(REPORT_IMMEDIATE, reportImmediately)
			.build()
		val request = OneTimeWorkRequestBuilder<FireActionWork>()
			.setInitialDelay(delay, TimeUnit.MILLISECONDS)
			.setInputData(data)
			.addTag(WORKER_TAG) //only to get the workers states later
			.addTag(FIRE_ACTION_WORK)
			.build()
		workManager.enqueue(request)
	}

	fun execute(klazz: Class<out Worker>, name: String) {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return
		}
		Timber.d("Enqueueing for immediate execution of ${klazz.simpleName}")
		val constraint = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
		val request = OneTimeWorkRequest.Builder(klazz)
			.setConstraints(constraint)
			.addTag(WORKER_TAG) //only to get the workers states later
			.build()

		workManager
			.beginUniqueWork(name, ExistingWorkPolicy.KEEP, request) // run now
			.then(reschedule(klazz, name)) // then run every once in a while
			.enqueue()
	}

	fun schedule(klazz: Class<out Worker>, name: String): Worker.Result {
		if (!app.isGooglePlayServicesAvailable() || !app.haveLocationPermission()) {
			return Worker.Result.RETRY
		}
		val constraint = Constraints.Builder().apply {
			setRequiredNetworkType(NetworkType.UNMETERED)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
				setRequiresDeviceIdle(true)
		}.build()
		val request = PeriodicWorkRequest.Builder(
				klazz,
				PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.HOURS,
				PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.HOURS)
			.setConstraints(constraint)
			.addTag(WORKER_TAG) //only to get the workers states later
			.build()

		Timber.d("Enqueueing periodic sync of ${klazz.simpleName} with id: ${request.id}")
		workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.KEEP, request)

		return Worker.Result.SUCCESS
	}

	private fun reschedule(klazz: Class<out Worker>, name: String): OneTimeWorkRequest {
		val data = Data.Builder()
			.putString(RESCHEDULE_CLASS, klazz.canonicalName)
			.putString(RESCHEDULE_NAME, name)
			.build()
		return OneTimeWorkRequest.Builder(RescheduleWorker::class.java)
			.setConstraints(Constraints.NONE)
			// TODO whenever WorkManager is updated, test this again
			// it was not working without the delay https://stackoverflow.com/questions/51078090/workmanager-chained-work-not-running#
			.setInitialDelay(1, TimeUnit.SECONDS)
			.setInputData(data)
			.addTag(WORKER_TAG) //only to get the workers states later
			.build()
	}

	companion object {
		val SYNC_WORK = "${SyncWork::class.java.canonicalName!!}.SYNC"
		val UPLOAD_WORK = "${UploadWork::class.java.canonicalName!!}.UPLOAD"
		val FENCE_WORK = "${UploadWork::class.java.canonicalName!!}.FENCE"
		internal val ACTION_STRING = NotificationsSdk.ACTION_PRESENT
		internal val FIRE_ACTION_WORK = "${FireActionWork::class.java.canonicalName!!}.ACTION"
		internal val REPORT_IMMEDIATE = "${FireActionWork::class.java.canonicalName!!}.REPORT_IMMEDIATE"
		internal val TRIGGER_TYPE = "${FireActionWork::class.java.canonicalName!!}.TRIGGER_TYPE"
		internal val RESCHEDULE_CLASS = "${RescheduleWorker::class.java.canonicalName!!}.CLASS"
		internal val RESCHEDULE_NAME = "${RescheduleWorker::class.java.canonicalName!!}.NAME"

		internal const val WORKER_TAG = "com.sensorberg.notifications.sdk.internal.work.WORKER_TAG"

		fun createAction(moshi: Moshi): JsonAdapter<Action> {
			return moshi.adapter<Action>(Action::class.java)
		}
	}

	class RescheduleWorker : Worker(), KoinComponent {

		private val workManager: WorkUtils by inject()

		override fun doWork(): Result {
			val className = inputData.getString(WorkUtils.RESCHEDULE_CLASS, null)!!
			val name = inputData.getString(WorkUtils.RESCHEDULE_NAME, null)!!
			val klazz = Class.forName(className) as Class<out Worker>
			Timber.i("Rescheduling ${klazz.simpleName}")
			return workManager.schedule(klazz, name)
		}
	}

	fun printWorkerStates() {
		val statusesForUniqueWork = workManager.getStatusesByTag(WORKER_TAG)
		statusesForUniqueWork.observeForever { workStatuses ->
			workStatuses?.forEach { workStatus ->
				Timber.d(workStatus.toString())
			}
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
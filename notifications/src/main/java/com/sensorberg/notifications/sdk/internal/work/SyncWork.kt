package com.sensorberg.notifications.sdk.internal.work

import android.app.Application
import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.backend.Backend
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.logResult
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.model.ActionModel
import com.sensorberg.notifications.sdk.internal.model.TimePeriod
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.model.TriggerActionMap
import com.sensorberg.notifications.sdk.internal.registration.BeaconRegistration
import com.sensorberg.notifications.sdk.internal.registration.GeofenceRegistration
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Exchanger
import java.util.concurrent.Executor

class SyncWork : Worker(), KoinComponent {

	private val app: Application by inject(InjectionModule.appBean)
	private val database: SdkDatabase by inject()
	private val backend: Backend by inject()
	private val executor: Executor by inject(InjectionModule.executorBean)

	// this work uses several async tasks,
	// so the following exchangers force the worker thread to await them
	private val exchanger = Exchanger<Worker.Result>()

	override fun doWork(): Result {

		logStart()

		if (!app.haveLocationPermission()) {
			Timber.w("SyncWork FAILURE. User revoked location permission")
			return Result.FAILURE
		}

		getTriggersFromBackend()
		return logResult(exchanger.exchange(null))
	}

	private fun getTriggersFromBackend() {
		Timber.d("Requesting list of triggers from backend")
		backend.getNotificationTriggers(object : Backend.NotificationTriggers {
			override fun onSuccess(triggers: List<Trigger>, timePeriods: List<TimePeriod>, actions: List<ActionModel>, mappings: List<TriggerActionMap>) {
				// run all of it on another thread
				executor.execute {
					Timber.d("Successfully got ${triggers.size} triggers and ${actions.size} actions from backend")

					val beacons = triggers.mapNotNull { it as? Trigger.Beacon }
					val geofences = triggers.mapNotNull { it as? Trigger.Geofence }

					database.insertData(timePeriods, actions, mappings, geofences)

					val beaconResult = BeaconRegistration().execute(beacons)
					val fencesResult = GeofenceRegistration().execute()

					exchanger.exchange(combineResult(beaconResult, fencesResult))
				}
			}

			override fun onFail() {
				Timber.w("SyncWork RETRY. Fail to get triggers from backend")
				exchanger.exchange(Worker.Result.RETRY)
			}
		})
	}

	companion object {
		fun combineResult(vararg results: Worker.Result): Worker.Result {
			var fail = false
			var retry = false

			results.forEach {
				fail = fail || (it == Worker.Result.FAILURE)
				retry = retry || (it == Worker.Result.RETRY)
			}
			return when {
				fail -> Worker.Result.FAILURE
				retry -> Worker.Result.RETRY
				else -> Worker.Result.SUCCESS
			}
		}
	}

}
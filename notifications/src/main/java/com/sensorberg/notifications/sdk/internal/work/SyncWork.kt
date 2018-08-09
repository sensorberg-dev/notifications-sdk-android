package com.sensorberg.notifications.sdk.internal.work

import android.app.Application
import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.*
import com.sensorberg.notifications.sdk.internal.backend.Backend
import com.sensorberg.notifications.sdk.internal.model.ActionModel
import com.sensorberg.notifications.sdk.internal.model.TimePeriod
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.model.TriggerActionMap
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Exchanger
import java.util.concurrent.Executor

internal class SyncWork : Worker(), KoinComponent {

	private val sdkEnableHandler: SdkEnableHandler by inject()
	private val app: Application by inject(InjectionModule.appBean)
	private val database: SdkDatabase by inject()
	private val backend: Backend by inject()
	private val workUtils: WorkUtils by inject()
	private val executor: Executor by inject(InjectionModule.executorBean)
	private val moshi: Moshi by inject(InjectionModule.moshiBean)
	private val beaconsAdapter: JsonAdapter<List<Trigger.Beacon>> by lazy {
		val listMyData = Types.newParameterizedType(List::class.java, Trigger.Beacon::class.java)
		moshi.adapter<List<Trigger.Beacon>>(listMyData)
	}

	// this work uses several async tasks,
	// so the following exchangers force the worker thread to await them
	private val exchanger = Exchanger<Worker.Result>()

	override fun doWork(): Result {
		if (!sdkEnableHandler.isEnabled()) return Result.FAILURE
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

					workUtils.execute(BeaconWork::class.java, beaconsAdapter.toJson(beacons))
					workUtils.execute(GeofenceWork::class.java)

					exchanger.exchange(Worker.Result.SUCCESS)
				}
			}

			override fun onFail() {
				Timber.w("SyncWork RETRY. Fail to get triggers from backend")
				exchanger.exchange(Worker.Result.RETRY)
			}
		})
	}
}
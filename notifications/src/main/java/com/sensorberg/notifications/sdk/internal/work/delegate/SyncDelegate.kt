package com.sensorberg.notifications.sdk.internal.work.delegate

import android.app.Application
import androidx.work.ListenableWorker
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.backend.Backend
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.model.ActionModel
import com.sensorberg.notifications.sdk.internal.model.TimePeriod
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.model.TriggerActionMap
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import com.sensorberg.notifications.sdk.internal.work.BeaconWork
import com.sensorberg.notifications.sdk.internal.work.GeofenceWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.sensorberg.notifications.sdk.internal.NotificationSdkComponent
import org.koin.core.inject
import timber.log.Timber
import java.util.concurrent.Exchanger
import java.util.concurrent.Executor

class SyncDelegate : NotificationSdkComponent {

	private val database: SdkDatabase by inject()
	private val backend: Backend by inject()
	private val workUtils: WorkUtils by inject()
	private val executor: Executor by inject()
	private val exchanger = Exchanger<ListenableWorker.Result>()
	private val app: Application by inject()

	fun execute(): ListenableWorker.Result {

		if (!app.haveLocationPermission()) {
			Timber.w("SyncWork FAILURE. User revoked location permission")
			return ListenableWorker.Result.failure()
		}

		getTriggersFromBackend()
		return exchanger.exchange(null)
	}

	private fun getTriggersFromBackend() {
		Timber.d("Requesting list of triggers from backend")
		backend.getNotificationTriggers(object : Backend.NotificationTriggers {
			override fun onSuccess(triggers: List<Trigger>, timePeriods: List<TimePeriod>, actions: List<ActionModel>, mappings: List<TriggerActionMap>) {
				// run all of it on another thread
				executor.execute {
					Timber.d("Successfully got ${triggers.size} triggers and ${actions.size} actions from backend")

					database.insertData(timePeriods, actions, mappings, triggers)

					workUtils.execute(BeaconWork::class.java)
					workUtils.execute(GeofenceWork::class.java)

					exchanger.exchange(ListenableWorker.Result.success())
				}
			}

			override fun onFail() {
				Timber.w("SyncWork RETRY. Fail to get triggers from backend")
				exchanger.exchange(ListenableWorker.Result.retry())
			}
		})
	}

}
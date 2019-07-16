package com.sensorberg.notifications.sdk.internal.work.delegate

import android.app.Application
import androidx.work.ListenableWorker
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.*
import com.google.android.gms.tasks.Tasks
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.receivers.BeaconReceiver
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import com.sensorberg.notifications.sdk.internal.NotificationSdkComponent
import org.koin.core.inject
import timber.log.Timber

internal class BeaconRegistration : NotificationSdkComponent {

	private val app: Application by inject()
	private val apis: GoogleApiAvailability by inject()
	private val database: SdkDatabase by inject()

	fun execute(): ListenableWorker.Result {

		if (!app.haveLocationPermission()) {
			Timber.w("Beacon registration FAILURE. User revoked location permission")
			return ListenableWorker.Result.failure()
		}

		val beacons = database.beaconRegistrationDao().get()

		Timber.d("Start to register ${beacons.size} beacons to Google Play Services")

		val nearby = Nearby.getMessagesClient(app, MessagesOptions.Builder()
			.setPermissions(NearbyPermissions.BLE)
			.build())

		val task = apis
			.checkApiAvailability(nearby)
			.onSuccessTask { nearby.unsubscribe(BeaconReceiver.generateUnsubscribePendingIntent(app)) }
			.onSuccessTask {
				return@onSuccessTask if (beacons.isEmpty()) {
					// if beacons is empty, I don't have to register anything,
					Tasks.forResult(null as Void?)
				} else {
					val messageFilter = MessageFilter.Builder()
					beacons.forEach {
						messageFilter.includeIBeaconIds(it.proximityUuid, it.major, it.minor)
					}
					val options = SubscribeOptions.Builder()
						.setStrategy(Strategy.BLE_ONLY)
						.setFilter(messageFilter.build())
						.build()

					nearby.subscribe(BeaconReceiver.generateSubscribePendingIntent(app), options)
				}
			}
		val result = RegistrationHelper.awaitResult("Beacon", 30, task)
		if (result == ListenableWorker.Result.success()) {
			database.beaconRegistrationDao().delete()
		}
		return result
	}
}
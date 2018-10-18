package com.sensorberg.notifications.sdk.internal.work.delegate

import android.app.Application
import androidx.work.Worker
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.*
import com.google.android.gms.tasks.Tasks
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.receivers.BeaconReceiver
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

internal class BeaconRegistration : KoinComponent {

	private val app: Application by inject(InjectionModule.appBean)
	private val apis: GoogleApiAvailability by inject(InjectionModule.googleApiAvailabilityBean)
	private val database: SdkDatabase by inject()

	fun execute(): Worker.Result {

		if (!app.haveLocationPermission()) {
			Timber.w("Beacon registration FAILURE. User revoked location permission")
			return Worker.Result.FAILURE
		}

		val beacons = database.beaconDao().getBeaconsForRegistration()

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
		if (result == Worker.Result.SUCCESS) {
			database.beaconDao().clearBeaconsForRegistration()
		}
		return result
	}
}
package com.sensorberg.notifications.sdk.internal.registration

import android.app.Application
import androidx.work.Worker
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.*
import com.google.android.gms.tasks.Tasks
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.NotificationsSdkImpl
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.receivers.BeaconReceiver
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class BeaconRegistration : KoinComponent {

	private val app: Application by inject(InjectionModule.appBean)
	private val apis: GoogleApiAvailability by inject()

	fun execute(beacons: List<Trigger.Beacon>): Worker.Result {

		if (!app.haveLocationPermission()) {
			Timber.w("Beacon registration FAILURE. User revoked location permission")
			return Worker.Result.FAILURE
		}

		Timber.d("Start to register ${beacons.size} beacons to Google Play Services")

		val nearby = Nearby.getMessagesClient(app, MessagesOptions.Builder()
			.setPermissions(NearbyPermissions.BLE)
			.build())

		val task = apis
			// is nearby available ?
			.checkApiAvailability(nearby)
			// remove preview registration
			.continueWithTask {
				nearby.unsubscribe(BeaconReceiver.generatePendingIntent(app))
			}
			// register
			.continueWithTask {
				return@continueWithTask if (beacons.isEmpty()) {
					// if beacons is empty, I don't have to register anything,
					Tasks.forResult(null as Void?) // LOLs, the result have to be void
				} else {
					val messageFilter = MessageFilter.Builder()
					beacons.forEach {
						messageFilter.includeIBeaconIds(it.proximityUuid, it.major, it.minor)
					}

					val options = SubscribeOptions.Builder()
						.setStrategy(Strategy.BLE_ONLY)
						.setFilter(messageFilter.build())
						.build()

					NotificationsSdkImpl.BeaconRegistrationHack.onRegistration()
					nearby.subscribe(BeaconReceiver.generatePendingIntent(app), options)
				}
			}

		try {
			// await synchronously to completion
			Tasks.await(task, 30, TimeUnit.SECONDS)
			return if (task.isSuccessful) {
				Timber.d("Beacon registration SUCCESS")
				Worker.Result.SUCCESS
			} else {
				Timber.w("Beacon registration fail. RETRY. ${task.exception}")
				Worker.Result.RETRY
			}
		} catch (e: Exception) {
			Timber.e(e, "Beacon registration timeout. RETRY. $e")
			return Worker.Result.RETRY
		}
	}
}
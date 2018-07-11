package com.sensorberg.notifications.sdk.internal.registration

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.work.Worker
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.common.storage.ActionDao
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.haveLocationProvider
import com.sensorberg.notifications.sdk.internal.receivers.GeofenceReceiver
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class GeofenceRegistration : KoinComponent {

	private val app: Application by inject(InjectionModule.notificationApp)
	private val apis: GoogleApiAvailability by inject()
	private val dao: ActionDao by inject()
	private val executor: Executor by inject()

	@SuppressLint("MissingPermission")
	fun execute(): Worker.Result {

		if (!app.haveLocationPermission()) {
			Timber.w("Fences registration FAILURE. User revoked location permission")
			return Worker.Result.FAILURE
		}

		if (!app.haveLocationProvider()) {
			Timber.w("Fences registration FAILURE. Location providers are turned off")
			return Worker.Result.FAILURE
		}

		val locationApi = LocationServices.getFusedLocationProviderClient(app)
		val geofenceApi = GeofencingClient(app)

		val task = apis.checkApiAvailability(locationApi, geofenceApi)
			// remove preview registration
			.continueWithTask { geofenceApi.removeGeofences(GeofenceReceiver.generatePendingIntent(app)) }
			// get current location
			.continueWithTask { locationApi.lastLocation } // TODO: maybe get a new location ?
			// register on background thread
			.continueWithTask(executor, Continuation<Location, Task<Void>> { locationTask ->
				val location = locationTask.result
				val fencesPair = dao.findClosestGeofences(location)
				val fences = fencesPair.fences
				val maxDistance = fencesPair.maxDistance
				return@Continuation if (fences.isEmpty()) {
					Tasks.forResult(null)
				} else {
					val updateGeofencesFence = Geofence.Builder() // when user moved away from current fences, reprocess them
						.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
						.setCircularRegion(location.latitude, location.longitude, maxDistance)
						.setRequestId(GeofenceReceiver.EXIT_CURRENT_LOCATION_FENCE)
						.setNotificationResponsiveness(20 * 60 * 1000) // 20 minutes, no need to burn battery for that
						.build()
					val request = GeofencingRequest.Builder()
						.addGeofences(fences) // add all from database
						.addGeofence(updateGeofencesFence) // add one to update the fences when user moves away from here
						.build()
					geofenceApi.addGeofences(request, GeofenceReceiver.generatePendingIntent(app))
				}
			})

		try {
			// await synchronously to completion
			Tasks.await(task, 5, TimeUnit.SECONDS)
			return if (task.isSuccessful) {
				Timber.d("Fences registration SUCCESS")
				Worker.Result.SUCCESS
			} else {
				Timber.d("Fences registration RETRY")
				Worker.Result.RETRY
			}
		} catch (e: Exception) {
			Timber.d("Fences registration RETRY")
			return Worker.Result.RETRY
		}
	}
}

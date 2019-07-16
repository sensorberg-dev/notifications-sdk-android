package com.sensorberg.notifications.sdk.internal.work.delegate

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.work.ListenableWorker
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.haveLocationProvider
import com.sensorberg.notifications.sdk.internal.model.RegisteredGeoFence
import com.sensorberg.notifications.sdk.internal.receivers.GeofenceReceiver
import com.sensorberg.notifications.sdk.internal.storage.GeofenceDao
import com.sensorberg.notifications.sdk.internal.storage.GeofenceQueryResult
import com.sensorberg.notifications.sdk.internal.NotificationSdkComponent
import org.koin.core.inject
import timber.log.Timber
import java.util.concurrent.Executor

internal class GeofenceRegistration : NotificationSdkComponent {

	private val app: Application by inject()
	private val googleApi: GoogleApiAvailability by inject()
	private val fenceDao: GeofenceDao by inject()
	private val executor: Executor by inject()

	@SuppressLint("MissingPermission")
	fun execute(): ListenableWorker.Result {

		if (!app.haveLocationPermission()) {
			Timber.w("Fences registration FAILURE. User revoked location permission")
			return ListenableWorker.Result.failure()
		}

		if (!app.haveLocationProvider()) {
			Timber.w("Fences registration FAILURE. Location providers are turned off")
			return ListenableWorker.Result.failure()
		}

		Timber.d("Start to register geofences to Google Play Services")

		val locationClient = LocationServices.getFusedLocationProviderClient(app)
		val geofenceClient = GeofencingClient(app)

		var location: Location? = null
		var query: GeofenceQueryResult? = null

		val task = googleApi.checkApiAvailability(locationClient, geofenceClient)
			.onSuccessTask { locationClient.lastLocation }
			.onSuccessTask(executor, SuccessContinuation<Location, Void> {
				if (it == null) {
					return@SuccessContinuation Tasks.forException(IllegalStateException("location was null"))
				}

				location = it
				Timber.d("Location is: $location")
				query = fenceDao.findMostRelevantGeofences(location!!)
				Timber.d("Found ${query!!.fencesToAdd.size} most relevant geofences and ${query!!.fencesToRemove.size} no longer needed")
				if (query!!.fencesToRemove.isNotEmpty()) {
					geofenceClient.removeGeofences(query!!.fencesToRemove)
				} else {
					Tasks.forResult(null)
				}
			})
			.onSuccessTask { getRegisterGeofenceTask(geofenceClient, query!!, location!!) }
			.onSuccessTask(executor, SuccessContinuation<Void, Void> {
				Timber.d("Updating geofence database with ${query!!.fencesToAdd.size} registered fences")
				fenceDao.clearAllAndInstertNewRegisteredGeoFences(query!!.fencesToAdd.map { RegisteredGeoFence(it.requestId) })
				Tasks.forResult(null)
			})

		return RegistrationHelper.awaitResult("Fences", 30, task)
	}

	@SuppressLint("MissingPermission")
	private fun getRegisterGeofenceTask(geofenceClient: GeofencingClient, geofencesQueryResult: GeofenceQueryResult, location: Location): Task<Void> {
		val fences = geofencesQueryResult.fencesToAdd
		val maxDistance = geofencesQueryResult.maxDistance
		return if (fences.isEmpty()) {
			Tasks.forResult(null)
		} else {
			val updateGeofencesFence = Geofence.Builder() // when user moved away from current fencesToAdd, reprocess them
				.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
				.setCircularRegion(location.latitude, location.longitude, maxDistance)
				.setRequestId(GeofenceReceiver.EXIT_CURRENT_LOCATION_FENCE)
				.setExpirationDuration(Geofence.NEVER_EXPIRE)
				.build()
			val request = GeofencingRequest.Builder()
				.addGeofences(fences) // add all from database
				.addGeofence(updateGeofencesFence) // add one to update the fencesToAdd when user moves away from here
				.setInitialTrigger(0) //disable initial trigger
				.build()
			geofenceClient.addGeofences(request, GeofenceReceiver.generatePendingIntent(app))
		}
	}
}

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
import com.sensorberg.notifications.sdk.internal.*
import com.sensorberg.notifications.sdk.internal.model.RegisteredGeoFence
import com.sensorberg.notifications.sdk.internal.receivers.GeofenceReceiver
import com.sensorberg.notifications.sdk.internal.storage.GeofenceDao
import com.sensorberg.notifications.sdk.internal.storage.GeofenceQueryResult
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class GeofenceRegistration : KoinComponent {

	private val app: Application by inject(InjectionModule.appBean)
	private val googleApi: GoogleApiAvailability by inject(InjectionModule.googleApiAvailabilityBean)
	private val fenceDao: GeofenceDao by inject()
	private val executor: Executor by inject(InjectionModule.executorBean)

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

		Timber.d("Start to register geofences to Google Play Services")

		val locationClient = LocationServices.getFusedLocationProviderClient(app)
		val geofenceClient = GeofencingClient(app)

		var geofenceQueryResult: GeofenceQueryResult? = null
		var location: Location? = null

		val task = googleApi.checkApiAvailability(locationClient, geofenceClient)
			// get current location
			.continueWithTask { locationClient.lastLocation }
			// sanitize location client, because GPS sucks
			.continueWithTask { locationTask ->
				location = locationTask.result
				Timber.d("Got location: $location")
				if (location == null) {
					Tasks.forException(IllegalStateException("location was null"))
				} else {
					Tasks.forResult(location!!)
				}
			}
			// get fences data from data base in background thread
			.continueWithTask(executor, queryGeofenceData())
			.continueWithTask { task ->
				// capture result
				geofenceQueryResult = task.result
				Tasks.forResult(true)
			}
			//remove GeoFences
			.continueWithTask {
				Timber.d("Remove ${geofenceQueryResult!!.fencesToRemove.size} geofences from Google Play")
				geofenceClient.removeGeofences(geofenceQueryResult!!.fencesToRemove)
			}
			//register GeoFences
			.continueWithTask { getRegisterGeofenceTask(geofenceClient, geofenceQueryResult!!, location!!) }
			//remove all registered GeoFences from DB and add the newly registered ones
			.continueWithTask(executor, Continuation<Void, Task<Boolean>> {
				Timber.d("Updating geofence database with ${geofenceQueryResult!!.fencesToAdd.size} registered fences")
				fenceDao.clearAllAndInstertNewRegisteredGeoFences(geofenceQueryResult!!.fencesToAdd.map { RegisteredGeoFence(it.requestId) })
				Tasks.forResult(true)
			})

		return try {
			// await synchronously to completion
			Tasks.await(task, 30, TimeUnit.SECONDS)
			if (task.isSuccessful) {
				Timber.d("Fences registration SUCCESS")
				Worker.Result.SUCCESS
			} else {
				Timber.w("Fences registration fail. RETRY. ${task.exception}")
				Worker.Result.RETRY
			}
		} catch (e: Exception) {
			Timber.e(e, "Fences registration timeout. RETRY. $e")
			Worker.Result.RETRY
		}
	}

	private fun queryGeofenceData(): Continuation<Location, Task<GeofenceQueryResult>> {
		return Continuation { locationTask ->
			val location = locationTask.result
			val geofenceQueryResult = fenceDao.findMostRelevantGeofences(location)
			Timber.d("Found ${geofenceQueryResult.fencesToAdd.size} most relevant geofences")
			Tasks.forResult(geofenceQueryResult)
		}
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

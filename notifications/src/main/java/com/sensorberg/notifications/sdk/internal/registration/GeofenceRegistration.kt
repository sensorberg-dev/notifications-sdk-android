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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.common.model.RegisteredGeoFence
import com.sensorberg.notifications.sdk.internal.common.storage.ActionDao
import com.sensorberg.notifications.sdk.internal.common.storage.NearbyGeofencesResult
import com.sensorberg.notifications.sdk.internal.haveLocationPermission
import com.sensorberg.notifications.sdk.internal.haveLocationProvider
import com.sensorberg.notifications.sdk.internal.receivers.GeofenceReceiver
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class GeofenceRegistration : KoinComponent {

	private val app: Application by inject(InjectionModule.appBean)
	private val googleApi: GoogleApiAvailability by inject()
	private val actionDao: ActionDao by inject()
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

		val locationClient = LocationServices.getFusedLocationProviderClient(app)
		val geofenceClient = GeofencingClient(app)

		var relevantGeoFences: NearbyGeofencesResult? = null
		var location: Location? = null
		val task = googleApi.checkApiAvailability(locationClient, geofenceClient)
			// get current location
			.continueWithTask { locationClient.lastLocation }
			// get relevant 100
			.continueWithTask { locationTask ->
				Timber.d("Getting current location")
				location = locationTask.result
				if (location == null) {
					Tasks.forException<IllegalStateException>(IllegalStateException("location was null"))
				}
				relevantGeoFences = actionDao.findMostRelevantGeofences(location!!)
				Timber.d("found ${relevantGeoFences!!.fences.size} most relevant geofences")
				Tasks.forResult(true)
			}
			//remove GeoFences
			.continueWithTask {
				Timber.d("remove geofences")
				val deletableGeofences = actionDao.getRemovableGeofences(relevantGeoFences!!.fences.map { it.requestId })
				Timber.d("Remove ${deletableGeofences.size} geofences from Google Play")
				geofenceClient.removeGeofences(deletableGeofences.map { it.id })
			}
			//register GeoFences
			.continueWithTask { getRegisterGeofenceTask(geofenceClient, relevantGeoFences!!, location!!) }
			//remove all registered GeoFences from DB and add the newly registered ones
			.continueWithTask {
				Timber.d("Deleting all (${relevantGeoFences!!.fences.size}) registered GeoFences from DB and add most relevant ones to the DB")
				actionDao.clearAllAndInstertNewRegisteredGeoFences(relevantGeoFences!!.fences.map { RegisteredGeoFence(it.requestId) })
				Tasks.forResult(true)
			}

		return try {
			// await synchronously to completion
			Tasks.await(task, 30, TimeUnit.SECONDS)
			if (task.isSuccessful) {
				Timber.d("Fences registration SUCCESS")
				Worker.Result.SUCCESS
			} else {
				Timber.d("Fences registration RETRY")
				Worker.Result.RETRY
			}
		} catch (e: Exception) {
			Timber.d("Fences registration failed with exception RETRY")
			Timber.e(e)
			Worker.Result.RETRY
		}
	}

	@SuppressLint("MissingPermission")
	private fun getRegisterGeofenceTask(geofenceClient: GeofencingClient, closestGeoFences: NearbyGeofencesResult, location: Location): Task<Void> {
		val fences = closestGeoFences.fences
		val maxDistance = closestGeoFences.maxDistance
		return if (fences.isEmpty()) {
			Tasks.forResult(null)
		} else {
			val updateGeofencesFence = Geofence.Builder() // when user moved away from current fences, reprocess them
				.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
				.setCircularRegion(location.latitude, location.longitude, maxDistance)
				.setRequestId(GeofenceReceiver.EXIT_CURRENT_LOCATION_FENCE)
				.setExpirationDuration(Geofence.NEVER_EXPIRE)
				.build()
			val request = GeofencingRequest.Builder()
				.addGeofences(fences) // add all from database
				.addGeofence(updateGeofencesFence) // add one to update the fences when user moves away from here
				.setInitialTrigger(0) //disable initial trigger
				.build()
			geofenceClient.addGeofences(request, GeofenceReceiver.generatePendingIntent(app))
		}
	}
}

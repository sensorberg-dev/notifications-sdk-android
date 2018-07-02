package com.sensorberg.notifications.sdk.internal.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.sensorberg.notifications.sdk.internal.TriggerProcessor
import com.sensorberg.notifications.sdk.internal.common.model.Trigger
import com.sensorberg.notifications.sdk.internal.work.GeofenceWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor

class GeofenceReceiver : BroadcastReceiver(), KoinComponent {

	private val executor: Executor by inject()
	private val triggerProcessor: TriggerProcessor by inject()
	private val workUtils: WorkUtils by inject()

	override fun onReceive(context: Context, intent: Intent) {
		val event = GeofencingEvent.fromIntent(intent)
		if (event.hasError()) {
			if (event.errorCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
				reprocessGeofences()
			}
			val errorMessage = GeofenceStatusCodes.getStatusCodeString(event.errorCode)
			Timber.e("Received geofence error: $errorMessage")
		} else {
			val fences = event.triggeringGeofences
			fences.forEach {
				if (it.requestId == EXIT_CURRENT_LOCATION_FENCE &&
					event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
					reprocessGeofences()
				} else if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
					processTrigger(it.requestId, Trigger.Type.Enter)
				} else if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
					processTrigger(it.requestId, Trigger.Type.Exit)
				}
			}
		}
	}

	private fun reprocessGeofences() {
		workUtils.execute(GeofenceWork::class.java, WorkUtils.FENCE_WORK)
	}

	private fun processTrigger(triggerId: String, type: Trigger.Type) {
		val pending = goAsync() // process this trigger asynchronously
		executor.execute {
			triggerProcessor.process(triggerId, type)
			pending.finish()
		}
	}

	companion object {

		val EXIT_CURRENT_LOCATION_FENCE = "${GeofenceReceiver::class.java.canonicalName}.EXIT"

		fun generatePendingIntent(context: Context): PendingIntent {
			val i = Intent(context, GeofenceReceiver::class.java)
			return PendingIntent.getBroadcast(context, 2, i, PendingIntent.FLAG_CANCEL_CURRENT)
		}
	}

}
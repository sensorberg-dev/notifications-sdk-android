package com.sensorberg.notifications.sdk.internal.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.TriggerProcessor
import com.sensorberg.notifications.sdk.internal.async
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.storage.GeofenceDao
import com.sensorberg.notifications.sdk.internal.work.GeofenceWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.sensorberg.notifications.sdk.internal.NotificationSdkComponent
import org.koin.core.inject
import timber.log.Timber
import java.util.concurrent.Executor

class GeofenceReceiver : BroadcastReceiver(), NotificationSdkComponent {

	private val dao: GeofenceDao by inject()
	private val executor: Executor by inject()
	private val triggerProcessor: TriggerProcessor by inject()
	private val workUtils: WorkUtils by inject()
	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun onReceive(context: Context, intent: Intent) {
		if (!sdkEnableHandler.isEnabled()) return
		val event = GeofencingEvent.fromIntent(intent)
		if (event == null) return // do not replace with elvis
		if (event.hasError()) {
			if (event.errorCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
				val pending = goAsync()
				executor.execute {
					dao.clearAllAndInstertNewRegisteredGeoFences(null)
					workUtils.execute(GeofenceWork::class.java)
					pending.finish()
				}
			}
			val errorMessage = GeofenceStatusCodes.getStatusCodeString(event.errorCode)
			Timber.e("Received geofence error: $errorMessage")
		} else {
			val fences = event.triggeringGeofences
			if (fences == null) {
				workUtils.execute(GeofenceWork::class.java)
				return
			} // do not replace with elvis
			var reprocessFences = false

			val triggerIds = fences.mapNotNull {
				return@mapNotNull if (it.requestId == EXIT_CURRENT_LOCATION_FENCE) {
					reprocessFences = event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
					null
				} else it.requestId
			}

			if (reprocessFences) {
				workUtils.execute(GeofenceWork::class.java)
			}

			if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
				processTrigger(triggerIds, Trigger.Type.Enter)
			} else if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
				processTrigger(triggerIds, Trigger.Type.Exit)
			}
		}
	}

	private fun processTrigger(triggerIds: List<String>, type: Trigger.Type) {
		async(executor) {
			triggerIds.forEach { triggerProcessor.process(it, type) }
		}
	}

	companion object {

		private const val GEOFENCE_REQUEST_CODE = 1339

		val EXIT_CURRENT_LOCATION_FENCE = "${GeofenceReceiver::class.java.canonicalName}.EXIT"

		fun generatePendingIntent(context: Context): PendingIntent {
			val intent = Intent(context, GeofenceReceiver::class.java)
			return PendingIntent.getBroadcast(context, GEOFENCE_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
		}
	}

}
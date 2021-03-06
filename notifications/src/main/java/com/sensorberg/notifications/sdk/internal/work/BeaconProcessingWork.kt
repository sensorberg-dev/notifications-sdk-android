package com.sensorberg.notifications.sdk.internal.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.logResult
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.work.delegate.BeaconProcessingDelegate
import com.sensorberg.notifications.sdk.internal.NotificationSdkComponent
import org.koin.core.inject

internal class BeaconProcessingWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), NotificationSdkComponent {

	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun doWork(): Result {
		if (!sdkEnableHandler.isEnabled()) return Result.failure()
		logStart()
		val beaconKey = getBeaconKey()
		val result = BeaconProcessingDelegate().execute(beaconKey)
		return logResult(result)
	}

	companion object {

		private const val ENTER_EVENT_TIMEOUT = 24 * 60 * 60 * 1000L

		internal data class ProcessResult(val workerResult: Result,
										  val event: BeaconEvent?,
										  val msg: String,
										  val deleteFromDbTimeStamp: Long? = null)

		fun processData(bluetoothOn: Boolean,
						haveLocationProvider: Boolean,
						beaconKey: String,
						visibleBeaconTimeStamp: Long?,
						lastEvent: BeaconEvent?): ProcessResult {

			if (!bluetoothOn) {
				return ProcessResult(Result.retry(), null, "BeaconProcessingWork can't proceed. Bluetooth is off.")
			}

			if (!haveLocationProvider) {
				return ProcessResult(Result.retry(), null, "BeaconProcessingWork can't proceed. Location is off.")
			}

			if (lastEvent == null) {
				// this is a weird edge case in case there's a duplicate work running and one deleted it already from DB
				// in reality it should never happen, but I rather check it, then crash the app
				return ProcessResult(Result.success(), null, "There was no lastEvent for beacon $beaconKey")
			}

			val isBeaconVisible = if (visibleBeaconTimeStamp != null) {
				val elapsedTime = System.currentTimeMillis() - visibleBeaconTimeStamp
				elapsedTime < ENTER_EVENT_TIMEOUT
			} else {
				false
			}

			val result = when {
				(lastEvent.type == Trigger.Type.Enter && !isBeaconVisible) -> lastEvent.copy(type = Trigger.Type.Enter)
				(lastEvent.type == Trigger.Type.Exit && isBeaconVisible) -> lastEvent.copy(type = Trigger.Type.Exit)
				else -> null
			}
			val message = if (result == null) "Beacon $beaconKey no changes" else "Beacon ${result.type} lastEvent for $beaconKey"

			return ProcessResult(Result.success(), result, message, lastEvent.timestamp)

		}
	}
}
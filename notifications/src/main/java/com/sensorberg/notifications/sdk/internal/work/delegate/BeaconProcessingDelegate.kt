package com.sensorberg.notifications.sdk.internal.work.delegate

import android.app.Application
import android.bluetooth.BluetoothAdapter
import androidx.work.ListenableWorker
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.TriggerProcessor
import com.sensorberg.notifications.sdk.internal.haveLocationProvider
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.model.VisibleBeacons
import com.sensorberg.notifications.sdk.internal.storage.BeaconDao
import com.sensorberg.notifications.sdk.internal.work.BeaconProcessingWork
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

internal class BeaconProcessingDelegate : KoinComponent {

	private val app: Application by inject(InjectionModule.appBean)
	private val dao: BeaconDao by inject()
	private val triggerProcessor: TriggerProcessor by inject()

	fun execute(beaconKey: String): ListenableWorker.Result {

		val result = BeaconProcessingWork.processData(isBluetoothOn(),
													  app.haveLocationProvider(),
													  beaconKey,
													  dao.getVisibleBeacon(beaconKey)?.timestamp,
													  dao.getLastEventForBeacon(beaconKey))

		result.event?.let { event ->
			if (event.type == Trigger.Type.Enter) {
				dao.addBeaconVisible(VisibleBeacons(beaconKey, System.currentTimeMillis()))
			} else if (event.type == Trigger.Type.Exit) {
				dao.removeBeaconVisible(VisibleBeacons(beaconKey, System.currentTimeMillis()))
			}
			triggerProcessor.process(Trigger.Beacon.getTriggerId(event.proximityUuid, event.major, event.minor, event.type), event.type)
		}
		result.deleteFromDbTimeStamp?.let { timestamp ->
			dao.deleteEventForBeacon(beaconKey, timestamp)
		}
		Timber.d(result.msg)
		return result.workerResult
	}

	companion object {
		private fun isBluetoothOn(): Boolean {
			val adapter = BluetoothAdapter.getDefaultAdapter()
			return adapter?.isEnabled ?: false
		}
	}
}
package com.sensorberg.notifications.sdk.internal.work

import android.app.Application
import android.bluetooth.BluetoothAdapter
import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.TriggerProcessor
import com.sensorberg.notifications.sdk.internal.haveLocationProvider
import com.sensorberg.notifications.sdk.internal.logResult
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.model.VisibleBeacons
import com.sensorberg.notifications.sdk.internal.storage.BeaconDao
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

internal class BeaconProcessingWork : Worker(), KoinComponent {

	private val app: Application by inject()
	private val dao: BeaconDao by inject()
	private val triggerProcessor: TriggerProcessor by inject()

	override fun doWork(): Result {

		logStart()

		if (!isBluetoothOn()) {
			Timber.w("${javaClass.simpleName} can't proceed. Bluetooth is off. RETRY")
			return Worker.Result.RETRY
		}

		if (!app.haveLocationProvider()) {
			Timber.w("${javaClass.simpleName} can't proceed. Location is off. RETRY")
			return Worker.Result.RETRY
		}

		val beaconKey = getBeaconKey()
		val isVisible = dao.isBeaconVisible(beaconKey)
		val events = dao.getBeaconEvents(beaconKey)

		Timber.d("Processing ${events.size} events for currently ${if (isVisible) "" else "not "}visible beacon $beaconKey")

		val result = processData(isVisible, events)
		if (result != null) {
			Timber.d("Beacon $beaconKey event ${result.type}")

			if (result.type == Trigger.Type.Enter) {
				dao.addBeaconVisible(VisibleBeacons(beaconKey, System.currentTimeMillis()))
			} else if (result.type == Trigger.Type.Exit) {
				dao.removeBeaconVisible(VisibleBeacons(beaconKey, System.currentTimeMillis()))
			}

			triggerProcessor.process(Trigger.Beacon.getTriggerId(result.proximityUuid, result.major, result.minor, result.type), result.type)
		} else {
			Timber.d("Beacon $beaconKey no changes")
		}

		dao.deleteBeaconEvents(events)

		return logResult(Worker.Result.SUCCESS)
	}

	companion object {

		private fun isBluetoothOn(): Boolean {
			val adapter = BluetoothAdapter.getDefaultAdapter()
			return adapter?.isEnabled ?: false
		}

		fun processData(visible: Boolean, events: List<BeaconEvent>): BeaconEvent? {

			if (events.isEmpty()) {
				return null
			}

			val event = events[0] // used for copy
			var isVisibleState = visible
			events.forEach {

				if (isVisibleState) {
					if (it.type == Trigger.Type.Exit) {
						isVisibleState = false
					}
				} else {
					if (it.type == Trigger.Type.Enter) {
						isVisibleState = true
					}
				}
			}

			return if (isVisibleState != visible) {
				val eventType = if (isVisibleState) Trigger.Type.Enter else Trigger.Type.Exit
				event.copy(type = eventType)
			} else {
				null
			}
		}
	}
}
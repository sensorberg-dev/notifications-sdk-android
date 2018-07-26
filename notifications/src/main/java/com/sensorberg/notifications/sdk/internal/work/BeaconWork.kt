package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.TriggerProcessor
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.storage.BeaconDao
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class BeaconWork : Worker(), KoinComponent {

	private val dao: BeaconDao by inject()
	private val triggerProcessor: TriggerProcessor by inject()

	override fun doWork(): Result {

		val beaconId = getBeaconId()
		val isVisible = dao.isBeaconVisible(beaconId)
		val events = dao.getBeaconEvents(beaconId)
		processData(isVisible, events)?.let {
			triggerProcessor.process(Trigger.Beacon.getTriggerId(it.proximityUuid, it.major, it.minor, it.type), it.type)
		}

		return Worker.Result.SUCCESS
	}

	companion object {
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
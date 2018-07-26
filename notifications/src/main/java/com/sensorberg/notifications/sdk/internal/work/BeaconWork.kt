package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.storage.BeaconDao
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class BeaconWork : Worker(), KoinComponent {

	private val dao: BeaconDao by inject()

	override fun doWork(): Result {

		val beaconId = getBeaconId()
		val isVisible = dao.isBeaconVisible(beaconId)
		val events = dao.getBeaconEvents(beaconId)
		processData(isVisible, events)

		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	companion object {
		fun processData(visible: Boolean, events: List<BeaconEvent>) {

		}
	}
}
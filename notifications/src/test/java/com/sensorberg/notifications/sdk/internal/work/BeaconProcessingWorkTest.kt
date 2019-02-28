package com.sensorberg.notifications.sdk.internal.work

import androidx.work.ListenableWorker
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.*

class BeaconProcessingWorkTest {

	@Test fun single_enter_event_pass_enters() {
		val result = processInner(null, enter())
		assertEquals(result!!.type, Trigger.Type.Enter)
	}

	@Test fun enter_again_pass_null() {
		val result = processInner(seenLately(), enter())
		assertNull(result)
	}

	@Test fun single_exit_event_pass_exits() {
		val result = processInner(seenLately(), exit())
		assertEquals(result!!.type, Trigger.Type.Exit)
	}

	@Test fun enter_again_after_long_time_pass_reenter() {
		val result = processInner(seenLately() - (24 * 60 * 60 * 1000L), enter())
		assertEquals(result!!.type, Trigger.Type.Enter)
	}

	@Test fun exit_again_pass_null() {
		val result = processInner(null, exit())
		assertNull(result)
	}

	@Test fun no_bt_should_retry() {
		val event = enter()
		var result = BeaconProcessingWork.processData(false, true, event.beaconKey, null, event)
		assertEquals(result.workerResult, ListenableWorker.Result.retry())
		result = BeaconProcessingWork.processData(false, false, event.beaconKey, null, event)
		assertEquals(result.workerResult, ListenableWorker.Result.retry())
		result = BeaconProcessingWork.processData(false, false, event.beaconKey, seenLately(), event)
		assertEquals(result.workerResult, ListenableWorker.Result.retry())
	}

	@Test fun no_location_should_retry() {
		val event = enter()
		var result = BeaconProcessingWork.processData(true, false, event.beaconKey, null, event)
		assertEquals(result.workerResult, ListenableWorker.Result.retry())
		result = BeaconProcessingWork.processData(false, false, event.beaconKey, null, event)
		assertEquals(result.workerResult, ListenableWorker.Result.retry())
		result = BeaconProcessingWork.processData(true, false, event.beaconKey, seenLately(), event)
		assertEquals(result.workerResult, ListenableWorker.Result.retry())
	}

	@Test fun no_event_should_success() {
		var result = BeaconProcessingWork.processData(true, true, "b", null, null)
		assertEquals(result.workerResult, ListenableWorker.Result.success())
		result = BeaconProcessingWork.processData(true, true, "b", seenLately(), null)
		assertEquals(result.workerResult, ListenableWorker.Result.success())

	}

	companion object {

		private fun processInner(visibleBeaconTimeStamp: Long?, event: BeaconEvent): BeaconEvent? {
			return BeaconProcessingWork.processData(true, true, event.beaconKey, visibleBeaconTimeStamp, event).event
		}

		private fun enter(): BeaconEvent {
			return BeaconEvent("b", 1, UUID.randomUUID(), 0, 0, Trigger.Type.Enter)
		}

		private fun exit(): BeaconEvent {
			return BeaconEvent("b", 1, UUID.randomUUID(), 0, 0, Trigger.Type.Exit)
		}

		private fun seenLately(): Long {
			return System.currentTimeMillis() - 1000L
		}
	}

}
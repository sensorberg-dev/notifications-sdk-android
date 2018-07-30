package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.*

class BeaconProcessingWorkTest {

	@Test fun single_enter_event_pass_enters() {
		val result = processInner(false, enter())
		assertEquals(result!!.type, Trigger.Type.Enter)
	}

	@Test fun enter_again_pass_null() {
		val result = processInner(true, enter())
		assertNull(result)
	}

	@Test fun single_exit_event_pass_exits() {
		val result = processInner(true, exit())
		assertEquals(result!!.type, Trigger.Type.Exit)
	}

	@Test fun exit_again_pass_null() {
		val result = processInner(false, exit())
		assertNull(result)
	}

	@Test fun no_bt_should_retry() {
		val event = enter()
		var result = BeaconProcessingWork.processData(false, true, event.beaconKey, false, event)
		assertEquals(result.workerResult, Worker.Result.RETRY)
		result = BeaconProcessingWork.processData(false, false, event.beaconKey, false, event)
		assertEquals(result.workerResult, Worker.Result.RETRY)
		result = BeaconProcessingWork.processData(false, false, event.beaconKey, true, event)
		assertEquals(result.workerResult, Worker.Result.RETRY)
	}

	@Test fun no_location_should_retry() {
		val event = enter()
		var result = BeaconProcessingWork.processData(true, false, event.beaconKey, false, event)
		assertEquals(result.workerResult, Worker.Result.RETRY)
		result = BeaconProcessingWork.processData(false, false, event.beaconKey, false, event)
		assertEquals(result.workerResult, Worker.Result.RETRY)
		result = BeaconProcessingWork.processData(true, false, event.beaconKey, true, event)
		assertEquals(result.workerResult, Worker.Result.RETRY)
	}

	@Test fun no_event_should_success() {
		var result = BeaconProcessingWork.processData(true, true, "b", false, null)
		assertEquals(result.workerResult, Worker.Result.SUCCESS)
		result = BeaconProcessingWork.processData(true, true, "b", true, null)
		assertEquals(result.workerResult, Worker.Result.SUCCESS)

	}

	companion object {

		private fun processInner(visible: Boolean, event: BeaconEvent): BeaconEvent? {
			return BeaconProcessingWork.processData(true, true, event.beaconKey, visible, event).event
		}

		private fun enter(): BeaconEvent {
			return BeaconEvent("b", 1, UUID.randomUUID(), 0, 0, Trigger.Type.Enter)
		}

		private fun exit(): BeaconEvent {
			return BeaconEvent("b", 1, UUID.randomUUID(), 0, 0, Trigger.Type.Exit)
		}
	}

}
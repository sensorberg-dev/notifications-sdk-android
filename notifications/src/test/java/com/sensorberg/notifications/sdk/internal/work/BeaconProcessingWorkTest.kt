package com.sensorberg.notifications.sdk.internal.work

import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.*

class BeaconProcessingWorkTest {

	@Test fun single_enter_event_pass_enters() {
		val result = BeaconProcessingWork.processData(false, listOf(enter()))
		assertEquals(result!!.type, Trigger.Type.Enter)
	}

	@Test fun enter_again_pass_null() {
		val result = BeaconProcessingWork.processData(true, listOf(enter()))
		assertNull(result)
	}

	@Test fun single_exit_event_pass_exits() {
		val result = BeaconProcessingWork.processData(true, listOf(exit()))
		assertEquals(result!!.type, Trigger.Type.Exit)
	}

	@Test fun exit_again_pass_null() {
		val result = BeaconProcessingWork.processData(false, listOf(exit()))
		assertNull(result)
	}

	@Test fun multiple_enter_event_pass_enters() {
		val result = BeaconProcessingWork.processData(false, list(10, Trigger.Type.Enter))
		assertEquals(result!!.type, Trigger.Type.Enter)
	}

	@Test fun multiple_exit_event_pass_exits() {
		val result = BeaconProcessingWork.processData(true, list(10, Trigger.Type.Exit))
		assertEquals(result!!.type, Trigger.Type.Exit)
	}

	@Test fun visible_pingpong_then_enter_pass_null() {
		val result = BeaconProcessingWork.processData(true, pingpong(5, Trigger.Type.Enter).plus(enter()))
		assertNull(result)
	}

	@Test fun visible_pingpong_then_exit_pass_exit() {
		val result = BeaconProcessingWork.processData(true, pingpong(5, Trigger.Type.Enter).plus(enter()))
		assertNull(result)
	}

	@Test fun not_visible_pingpong_then_enter_pass_enter() {
		val result = BeaconProcessingWork.processData(false, pingpong(5, Trigger.Type.Enter).plus(enter()))
		assertEquals(result!!.type, Trigger.Type.Enter)
	}

	@Test fun not_visible_pingpong_then_exit_pass_null() {
		val result = BeaconProcessingWork.processData(false, pingpong(5, Trigger.Type.Enter).plus(exit()))
		assertNull(result)
	}

	private fun <T> MutableList<T>.plus(t: T): MutableList<T> {
		add(t)
		return this
	}

	companion object {
		private fun enter(): BeaconEvent {
			return BeaconEvent("b", 1, UUID.randomUUID(), 0, 0, Trigger.Type.Enter)
		}

		private fun exit(): BeaconEvent {
			return BeaconEvent("b", 1, UUID.randomUUID(), 0, 0, Trigger.Type.Exit)
		}

		private fun list(number: Int, type: Trigger.Type): MutableList<BeaconEvent> {
			val e = BeaconEvent("b", 1, UUID.randomUUID(), 0, 0, type)
			val list = mutableListOf<BeaconEvent>()
			for (i in 0 until number) list.add(e.copy(timestamp = i + 1L))
			return list
		}

		private fun pingpong(number: Int, initial: Trigger.Type): MutableList<BeaconEvent> {
			val e = BeaconEvent("b", 1, UUID.randomUUID(), 0, 0, initial)
			var current = initial
			val list = mutableListOf<BeaconEvent>()
			for (i in 0 until number) {
				if (current == Trigger.Type.Exit) current = Trigger.Type.Enter
				else current = Trigger.Type.Exit
				list.add(e.copy(timestamp = i + 1L, type = current))
			}
			return list
		}
	}

}
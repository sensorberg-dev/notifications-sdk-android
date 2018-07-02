package com.sensorberg.notifications.sdk.internal.testbackend

import android.util.SparseArray
import com.sensorberg.notifications.sdk.internal.common.Backend
import com.sensorberg.notifications.sdk.internal.common.model.*
import java.util.*

class TestBackend(private val apiKey: Int) : Backend {

	override fun publishHistory(actions: List<ActionHistory>, conversions: List<ActionConversion>, callback: (Boolean) -> Unit) {
		callback.invoke(true)
	}

	override fun getNotificationTriggers(callback: Backend.NotificationTriggers) {
		val period = TimePeriod(
				actionId = "1",
				startsAt = 0,
				endsAt = Long.MAX_VALUE)
		val testCase = testCases.get(apiKey)
		callback.onSuccess(listOf(testCase.trigger),
						   listOf(period),
						   listOf(testCase.action),
						   listOf(testCase.mapping))
	}

	override fun setAdvertisementId(adId: String?) {

	}

	override fun setAttributes(attributes: Map<String, String>?) {

	}

	companion object {

		const val ENTER_IMMEDIATE = 1
		const val EXIT_IMMEDIATE = 2
		const val ENTER_DELAY = 3
		const val EXIT_DELAY = 4
		const val ENTER_DELIVER_AT = 5
		const val EXIT_DELIVER_AT = 6
		const val ENTER_SUPPRESSION = 7
		const val ENTER_MAX_COUNT = 8

		const val ENTER_EXIT_IMMEDIATE = 9
		const val ENTER_EXIT_DELAY = 10
		const val ENTER_EXIT_SUPPRESSION = 11

		private val testCases: SparseArray<TestSet> = SparseArray()
		private const val DELAY = 20 * 1000L
		private const val DELIVER = 60 * 1000L
		private const val SUPPRESS = 60 * 1000L

		init {
			val now = System.currentTimeMillis()
			testCases.put(ENTER_IMMEDIATE, testCase(action(0, 0, 0, 0), Trigger.Type.Enter))
			testCases.put(EXIT_IMMEDIATE, testCase(action(0, 0, 0, 0), Trigger.Type.Exit))
			testCases.put(ENTER_DELAY, testCase(action(DELAY, 0, 0, 0), Trigger.Type.Enter))
			testCases.put(EXIT_DELAY, testCase(action(DELAY, 0, 0, 0), Trigger.Type.Exit))
			testCases.put(ENTER_DELIVER_AT, testCase(action(0, now + DELIVER, 0, 0), Trigger.Type.Enter))
			testCases.put(EXIT_DELIVER_AT, testCase(action(0, now + DELIVER, 0, 0), Trigger.Type.Exit))
			testCases.put(ENTER_SUPPRESSION, testCase(action(0, 0, SUPPRESS, 0), Trigger.Type.Enter))
			testCases.put(ENTER_MAX_COUNT, testCase(action(0, 0, 0, 3), Trigger.Type.Enter))

			testCases.put(ENTER_EXIT_IMMEDIATE, testCase(action(0, 0, 0, 0), Trigger.Type.EnterOrExit))
			testCases.put(ENTER_EXIT_DELAY, testCase(action(DELAY, 0, 0, 0), Trigger.Type.EnterOrExit))
			testCases.put(ENTER_EXIT_SUPPRESSION, testCase(action(0, 0, SUPPRESS, 0), Trigger.Type.EnterOrExit))
		}

		private fun action(delay: Long, deliver: Long, supress: Long, max: Int): ActionModel {
			return ActionModel("1",
							   "meta",
							   "Hello World",
							   "This is an automated message",
							   "http://www.google.com", "{ }",
							   false, delay, deliver, supress, max, false)
		}

		private fun testCase(action: ActionModel, type: Trigger.Type): TestSet {
			val trigger = Trigger.Beacon(UUID.fromString("73676723-7400-0000-FFFF-0000FFFF0006"),
										 19319,
										 51765.toChar().toShort(),
										 type)
			val mapping = TriggerActionMap(0, trigger.getTriggerId(), type, action.id, null)
			return TestSet(trigger, action, mapping)

		}
	}

	private data class TestSet(val trigger: Trigger, val action: ActionModel, val mapping: TriggerActionMap)
}
package com.sensorberg.notifications.sdk.internal.backend.backendsdkv2

import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.TriggerMapper.Companion.META_ACTION_TRIGGER
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.TriggerMapper.Companion.META_ACTION_TYPE
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.model.Content
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.model.ResolveAction
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.model.Timeframe
import com.sensorberg.notifications.sdk.internal.model.TimePeriod
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*

class TriggerMapperTest {

	// 7367672374000000ffff0000ffff0004 00016 00040
	private val uuid = "7367672374000000ffff0000ffff0004"

	// Short.MIN_VALUE: -32768;
	// Short.MAX_VALUE: 32767;
	// Range: 65535

	@Test fun test_all_values() {
		for (i in 0..65535) {
			val expected = if (i > Short.MAX_VALUE) {
				i.toShort()
			} else {
				(i - Short.MAX_VALUE + Short.MIN_VALUE - 1).toShort()
			}
			assertEquals(expected, TriggerMapper.stringToShort(i.toString()))
		}
	}

	// that's the original value that we had a crash with,
	// I'll leave a test specia;l for it then
	@Test fun test_string_to_short_with_client_value() {
		val expected = (56973 - Short.MAX_VALUE + Short.MIN_VALUE - 1).toShort()
		assertEquals(expected, TriggerMapper.stringToShort("56973"))
	}

	@Test fun test_string_to_short() {
		assertEquals(0.toShort(), TriggerMapper.stringToShort("00000"))
		assertEquals((-1).toShort(), TriggerMapper.stringToShort("65535"))
		assertEquals((32767).toShort(), TriggerMapper.stringToShort("32767"))
		assertEquals((-32768).toShort(), TriggerMapper.stringToShort("32768"))
	}

	@Test fun test_major_extraction() {
		assertEquals(0.toShort(), TriggerMapper.extractMajor(major("00000")))
		assertEquals((-1).toShort(), TriggerMapper.extractMajor(major("65535")))
		assertEquals((32767).toShort(), TriggerMapper.extractMajor(major("32767")))
		assertEquals((-32768).toShort(), TriggerMapper.extractMajor(major("32768")))
	}

	@Test fun test_minor_extraction() {
		assertEquals(0.toShort(), TriggerMapper.extractMinor(minor("00000")))
		assertEquals((-1).toShort(), TriggerMapper.extractMinor(minor("65535")))
		assertEquals((32767).toShort(), TriggerMapper.extractMinor(minor("32767")))
		assertEquals((-32768).toShort(), TriggerMapper.extractMinor(minor("32768")))
	}

	@Test fun map_action_is_properly_mapped() {
		val resolveAction = ResolveAction(
				"eid",
				0,
				1,
				"name",
				listOf("beacon1", "beacon2"),
				2,
				false,
				3,
				false,
				Content("subject", "body", null, "url"),
				listOf(Timeframe("2018-10-09T14:00:00Z", "2018-11-09T14:00:00Z")))
		val timePeriods = mutableListOf<TimePeriod>()
		val action = TriggerMapper.mapAction(resolveAction, timePeriods)
		assertEquals("eid", action.id)
		assertEquals(0, JSONObject(action.payload!!)[META_ACTION_TRIGGER])
		assertEquals(1, JSONObject(action.payload!!)[META_ACTION_TYPE])
		assertEquals(2000, action.suppressionTime)
		assertEquals(3000, action.delay)
		assertEquals(0, action.maxCount)
		assertEquals("subject", action.subject)
		assertEquals("body", action.body)
		assertEquals("url", action.url)
		assertEquals(false, action.reportImmediately)
		assertEquals("eid", timePeriods[0].actionId)
		assertEquals(1539093600000, timePeriods[0].startsAt)
		assertEquals(1541772000000, timePeriods[0].endsAt)
	}

	@Test fun map_action_is_properly_mapped_2() {
		val resolveAction = ResolveAction(
				"eid",
				0,
				1,
				"name",
				listOf("beacon1", "beacon2"),
				2,
				true,
				3,
				true,
				Content("subject", "body", null, "url"),
				listOf(Timeframe("2018-10-09T14:00:00Z", "2018-11-09T14:00:00Z")))
		val timePeriods = mutableListOf<TimePeriod>()
		val action = TriggerMapper.mapAction(resolveAction, timePeriods)

		assertEquals(1, action.maxCount)
		assertEquals(true, action.reportImmediately)

	}

	fun major(value: String): String {
		return "${uuid}${value}00000"
	}

	fun minor(value: String): String {
		return "${uuid}00000${value}"
	}
}
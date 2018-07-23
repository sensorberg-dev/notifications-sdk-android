package com.sensorberg.notifications.sdk.internal

import com.sensorberg.notifications.sdk.internal.common.model.*
import com.sensorberg.notifications.sdk.internal.common.storage.ActionDao
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class TriggerProcessorTest {

	private var dao = DaoAdapter()

	@Before
	fun setup() {
		dao.getActionsForTrigger = mutableListOf()
		dao.statistics = null
		dao.timePeriodCount = 1
	}

	@Test
	fun max_fire_limit_pass() {
		val action = newAction().copy(maxCount = 1)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.size)
	}

	@Test
	fun max_fire_limit_fail() {
		val action = newAction().copy(maxCount = 1)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 1, 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(0, result.size)
	}

	@Test
	fun max_fire_limit_disabled() {
		val action = newAction().copy(maxCount = 0)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 50, 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.size)
	}

	@Test
	fun suppression_time_pass() {
		val action = newAction().copy(suppressionTime = 1000)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, System.currentTimeMillis() - 2000)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.size)
	}

	@Test
	fun suppression_time_fail() {
		val action = newAction().copy(suppressionTime = 1000)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, System.currentTimeMillis())

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(0, result.size)
	}

	@Test
	fun suppression_time_disabled() {
		val action = newAction().copy(suppressionTime = 0)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, System.currentTimeMillis())

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.size)
	}

	@Test
	fun if_available_now_pass() {
		val action = newAction()
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, 0)
		dao.timePeriodCount = 1

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.size)
	}

	@Test
	fun if_available_now_fail() {
		val action = newAction()
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, 0)
		dao.timePeriodCount = 0

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(0, result.size)
	}

	private fun newAction(): ActionQueryModel {
		return ActionQueryModel(string(),
								string(),
								string(),
								string(),
								string(),
								"{ }",
								false,
								0,
								0,
								0,
								0,
								string(),
								false)
	}

	private fun string(): String {
		return UUID.randomUUID().toString().replace("-", "")
	}

	class DaoAdapter : ActionDao() {
		override fun getGeofences(): List<GeofenceQuery> {

		}

		override fun insertRegisteredGeoFence(registeredGeoFence: List<RegisteredGeoFence>) {
		}

		override fun clearAllRegisteredGeoFences() {
		}

		override fun getRemovableGeofences(list: List<String>): List<RegisteredGeoFence> {
		}

		var getActionsForTrigger = mutableListOf<ActionQueryModel>()
		var statistics: Statistics? = null
		var timePeriodCount: Long = 0

		override fun getActionsForTrigger(triggerId: String, now: Long, vararg types: Trigger.Type): List<ActionQueryModel> {
			return getActionsForTrigger
		}

		override fun getStatisticsForAction(actionId: String): Statistics? {
			return statistics
		}

		override fun getTimePeriodsForAction(actionId: String, now: Long): Long {
			return timePeriodCount
		}

		override fun insertActions(actions: Collection<ActionModel>) {}
		override fun insertMappings(mappings: Collection<TriggerActionMap>) {}
		override fun insertTimePeriods(timePeriods: Collection<TimePeriod>) {}
		override fun insertStatistics(vararg statistics: Statistics) {}
		override fun clearActions() {}
		override fun clearMappings() {}
		override fun clearTimePeriods() {}
		override fun getActionHistory(): List<ActionHistory> {
			TODO("Not part of this test")
		}

		override fun insertActionHistory(vararg action: ActionHistory) {}

		override fun clearActionHistory(actions: List<ActionHistory>) {}

		override fun getActionConversion(): List<ActionConversion> {
			TODO("Not part of this test")
		}

		override fun insertActionConversion(vararg action: ActionConversion) {}
		override fun clearActionConversion(actions: List<ActionConversion>) {}
		override fun findClosestGeofences(in_sin_lat_rad: Double, in_cos_lat_rad: Double, in_sin_lon_rad: Double, in_cos_lon_rad: Double): List<GeofenceQuery> {
			TODO("Not part of this test")
		}

		override fun insertGeofences(geofences: List<GeofenceQuery>) {}
		override fun clearGeofences() {}
	}

}
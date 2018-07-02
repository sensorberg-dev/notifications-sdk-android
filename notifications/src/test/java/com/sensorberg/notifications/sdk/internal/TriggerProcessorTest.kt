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
		dao.statistics = Statistics(action.id, 0, 0, 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.first.size)
	}

	@Test
	fun max_fire_limit_fail() {
		val action = newAction().copy(maxCount = 1)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 1, 0, 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(0, result.first.size)
	}

	@Test
	fun max_fire_limit_disabled() {
		val action = newAction().copy(maxCount = 0)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 50, 0, 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.first.size)
	}

	@Test
	fun suppression_time_pass() {
		val action = newAction().copy(suppressionTime = 1000)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, System.currentTimeMillis() - 2000, 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.first.size)
	}

	@Test
	fun suppression_time_fail() {
		val action = newAction().copy(suppressionTime = 1000)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, System.currentTimeMillis(), 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(0, result.first.size)
	}

	@Test
	fun suppression_time_disabled() {
		val action = newAction().copy(suppressionTime = 0)
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, System.currentTimeMillis(), 0)

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.first.size)
	}

	@Test
	fun if_available_now_pass() {
		val action = newAction()
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, 0, 0)
		dao.timePeriodCount = 1

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(1, result.first.size)
	}

	@Test
	fun if_available_now_fail() {
		val action = newAction()
		dao.getActionsForTrigger.add(action)
		dao.statistics = Statistics(action.id, 0, 0, 0)
		dao.timePeriodCount = 0

		val result = TriggerProcessor.findActionsToFire(dao, "", Trigger.Type.Enter)
		assertEquals(0, result.first.size)
	}

	private fun newAction(): ActionModel {
		return ActionModel(string(),
						   string(),
						   string(),
						   string(),
						   string(),
						   "{ }",
						   false,
						   0,
						   0,
						   0,
						   0)
	}

	private fun string(): String {
		return UUID.randomUUID().toString().replace("-", "")
	}

	class DaoAdapter : ActionDao {

		var getActionsForTrigger = mutableListOf<ActionModel>()
		var statistics: Statistics? = null
		var timePeriodCount: Long = 0

		override fun getActionsForTrigger(triggerId: String, now: Long, vararg types: Trigger.Type): List<ActionModel> {
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
	}

}
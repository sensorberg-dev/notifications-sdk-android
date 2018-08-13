package com.sensorberg.notifications.sdk.internal

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.sensorberg.notifications.sdk.internal.model.*
import com.sensorberg.notifications.sdk.internal.storage.ActionDao
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class DatabaseTests {

	companion object {
		// this repeat value is only used to profile execution time
		// do not commit to git with a value different than 1
		const val REPEAT = 1
	}

	@get:Rule val repeatRule = RepeatTest.RepeatRule()

	private val random = Random()
	private var actionDao: ActionDao? = null
	private var database: SdkDatabase? = null

	@Before
	fun createDb() {
		val context = InstrumentationRegistry.getTargetContext()
		database = Room.inMemoryDatabaseBuilder(context, SdkDatabase::class.java).build()
		actionDao = database!!.actionDao()
	}

	@After
	fun closeDb() {
		database!!.clearAllTables()
		database!!.close()
	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun findActionForEnterTrigger() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.Enter)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Enter)

		assertEquals(1, query.size)
		assertEquals(actionId, query[0].id)

	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun findActionForExitTrigger() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.Exit)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Exit)

		assertEquals(1, query.size)
		assertEquals(actionId, query[0].id)

	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun findActionForEnterOrExitTrigger() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.EnterOrExit)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Enter, Trigger.Type.EnterOrExit)

		assertEquals(1, query.size)
		assertEquals(actionId, query[0].id)

	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun dontFindActionForOppositeTrigger() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.Enter)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Exit, Trigger.Type.EnterOrExit)

		assertEquals(0, query.size)
	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun dontFindActionForOppositeTrigger2() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.Exit)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Enter, Trigger.Type.EnterOrExit)

		assertEquals(0, query.size)
	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun findMultipleTriggersForSameAction() {
		val dao = actionDao!!

		val timePeriods = mutableListOf<TimePeriod>()
		val actions = mutableListOf<ActionModel>()
		val mappings = mutableListOf<TriggerActionMap>()

		val action = newAction()
		val t1 = Trigger.Beacon(uuid(), -1, -1, Trigger.Type.Enter)
		val t2 = Trigger.Beacon(uuid(), -2, -2, Trigger.Type.Exit)

		actions.add(action)
		mappings.add(TriggerActionMap(0, t1.getTriggerId(), Trigger.Type.Enter, action.id, null))
		mappings.add(TriggerActionMap(0, t2.getTriggerId(), Trigger.Type.Exit, action.id, null))
		timePeriods.add(TimePeriod(0, action.id, 0, Long.MAX_VALUE))

		for (i in 1..20) {
			val a = newAction()
			actions.add(a)
			mappings.add(TriggerActionMap(0, string(), Trigger.Type.Enter, a.id, null))
			timePeriods.add(TimePeriod(0, a.id, 0, Long.MAX_VALUE))
		}

		database!!.insertData(timePeriods, actions, mappings, listOf())

		val q1: List<ActionQueryModel> = dao.getActionsForTrigger(t1.getTriggerId(), System.currentTimeMillis(), Trigger.Type.Enter, Trigger.Type.EnterOrExit)
		val q2: List<ActionQueryModel> = dao.getActionsForTrigger(t2.getTriggerId(), System.currentTimeMillis(), Trigger.Type.Exit, Trigger.Type.EnterOrExit)

		assertEquals(action.id, q1[0].id)
		assertEquals(action.id, q2[0].id)

	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun findMultipleActionsForSameTrigger() {
		val dao = actionDao!!

		val timePeriods = mutableListOf<TimePeriod>()
		val actions = mutableListOf<ActionModel>()
		val mappings = mutableListOf<TriggerActionMap>()

		val a1 = newAction()
		val a2 = newAction()
		val a3 = newAction()
		val t1 = Trigger.Beacon(uuid(), -1, -2, Trigger.Type.EnterOrExit)
		actions.add(a1); actions.add(a2); actions.add(a3)
		timePeriods.add(TimePeriod(0, a1.id, 0, Long.MAX_VALUE))
		timePeriods.add(TimePeriod(0, a2.id, 0, Long.MAX_VALUE))
		timePeriods.add(TimePeriod(0, a3.id, 0, Long.MAX_VALUE))

		mappings.add(TriggerActionMap(0, t1.getTriggerId(), Trigger.Type.Enter, a1.id, null))
		mappings.add(TriggerActionMap(0, t1.getTriggerId(), Trigger.Type.Enter, a2.id, null))
		mappings.add(TriggerActionMap(0, t1.getTriggerId(), Trigger.Type.EnterOrExit, a3.id, null))


		for (i in 1..20) {
			val a = newAction()
			actions.add(a)
			mappings.add(TriggerActionMap(0, string(), Trigger.Type.Enter, a.id, null))
			timePeriods.add(TimePeriod(0, a.id, 0, Long.MAX_VALUE))
		}
		database!!.insertData(timePeriods, actions, mappings, listOf())

		var query: List<ActionQueryModel> = dao.getActionsForTrigger(t1.getTriggerId(), System.currentTimeMillis(), Trigger.Type.Enter, Trigger.Type.EnterOrExit)
		assertEquals(3, query.size)

		query = dao.getActionsForTrigger(t1.getTriggerId(), System.currentTimeMillis(), Trigger.Type.Exit, Trigger.Type.EnterOrExit)
		assertEquals(1, query.size)
	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun findMultipleActionsForMultipleTrigger() {

		// 5 triggers
		val t1 = Trigger.Beacon(uuid(), -1, -2, Trigger.Type.Enter)
		val t2 = Trigger.Beacon(uuid(), -1, -3, Trigger.Type.Exit)
		val t3 = Trigger.Beacon(uuid(), -1, -4, Trigger.Type.Enter)
		val t4 = Trigger.Beacon(uuid(), -1, -5, Trigger.Type.Exit)
		val t5 = Trigger.Beacon(uuid(), -1, -6, Trigger.Type.EnterOrExit)
		val testTriggers = listOf(t1, t2, t3, t4, t5)

		// 5 actions
		val a1 = newAction()
		val a2 = newAction()
		val a3 = newAction()
		val a4 = newAction()
		val a5 = newAction()
		val testActions = listOf(a1, a2, a3, a4, a5)

		// all actions have all triggers
		val timePeriods = mutableListOf<TimePeriod>()
		val mappings = mutableListOf<TriggerActionMap>()
		for (action in testActions) {
			timePeriods.add(TimePeriod(0, action.id, 0, Long.MAX_VALUE))
			for (trigger in testTriggers) {
				mappings.add(TriggerActionMap(0, trigger.getTriggerId(), trigger.type, action.id, null))
			}
		}

		val actions = mutableListOf<ActionModel>()
		actions.addAll(testActions)

		// add a bunch or random stuff just to have stuff on DB
		for (i in 1..90) {
			val a = newAction()
			actions.add(a)
			mappings.add(TriggerActionMap(0, string(), if (random.nextBoolean()) Trigger.Type.Enter else Trigger.Type.Exit, a.id, null))
			timePeriods.add(TimePeriod(0, a.id, 0, Long.MAX_VALUE))
		}

		// add all to DB
		database!!.insertData(timePeriods, actions, mappings, listOf())

		val dao = actionDao!!
		assertEquals(5, dao.getActionsForTrigger(t1.getTriggerId(), 1, Trigger.Type.Enter, Trigger.Type.EnterOrExit).size)
		assertEquals(0, dao.getActionsForTrigger(t2.getTriggerId(), 1, Trigger.Type.Enter, Trigger.Type.EnterOrExit).size)
		assertEquals(5, dao.getActionsForTrigger(t3.getTriggerId(), 1, Trigger.Type.Enter, Trigger.Type.EnterOrExit).size)
		assertEquals(0, dao.getActionsForTrigger(t4.getTriggerId(), 1, Trigger.Type.Enter, Trigger.Type.EnterOrExit).size)
		assertEquals(5, dao.getActionsForTrigger(t5.getTriggerId(), 1, Trigger.Type.Enter, Trigger.Type.EnterOrExit).size)

		assertEquals(0, dao.getActionsForTrigger(t1.getTriggerId(), 1, Trigger.Type.Exit, Trigger.Type.EnterOrExit).size)
		assertEquals(5, dao.getActionsForTrigger(t2.getTriggerId(), 1, Trigger.Type.Exit, Trigger.Type.EnterOrExit).size)
		assertEquals(0, dao.getActionsForTrigger(t3.getTriggerId(), 1, Trigger.Type.Exit, Trigger.Type.EnterOrExit).size)
		assertEquals(5, dao.getActionsForTrigger(t4.getTriggerId(), 1, Trigger.Type.Exit, Trigger.Type.EnterOrExit).size)
		assertEquals(5, dao.getActionsForTrigger(t5.getTriggerId(), 1, Trigger.Type.Exit, Trigger.Type.EnterOrExit).size)

	}

	@Test @RepeatTest.Repeat(REPEAT)
	fun actions_that_deliver_at_is_have_passed_shouldnt_trigger() {
		val timePeriods = mutableListOf<TimePeriod>()
		val mappings = mutableListOf<TriggerActionMap>()
		val actions = mutableListOf<ActionModel>()

		val action = newAction().copy(deliverAt = 100)
		val trigger = Trigger.Beacon(uuid(), -1, -2, Trigger.Type.Enter)

		actions.add(action)
		mappings.add(TriggerActionMap(0, trigger.getTriggerId(), Trigger.Type.Enter, action.id, null))
		timePeriods.add(TimePeriod(0, action.id, 0, Long.MAX_VALUE))

		// add a bunch or random stuff just to have stuff on DB
		val random = Random()
		for (i in 1..90) {
			val a = newAction()
			actions.add(a)
			mappings.add(TriggerActionMap(0, string(), if (random.nextBoolean()) Trigger.Type.Enter else Trigger.Type.Exit, a.id, null))
			timePeriods.add(TimePeriod(0, a.id, 0, Long.MAX_VALUE))
		}

		database!!.insertData(timePeriods, actions, mappings, listOf())

		val dao = actionDao!!
		val passQuery = dao.getActionsForTrigger(trigger.getTriggerId(), 10, Trigger.Type.Enter, Trigger.Type.EnterOrExit)
		assertEquals(1, passQuery.size)
		assertEquals(action.id, passQuery[0].id)
		assertEquals(0, dao.getActionsForTrigger(trigger.getTriggerId(), 200, Trigger.Type.Enter, Trigger.Type.EnterOrExit).size)
	}

	private fun fillDb(type: Trigger.Type): Pair<String, String> {

		val action = newAction()
		val trigger = Trigger.Beacon(uuid(), -1, -2, Trigger.Type.EnterOrExit)

		val timePeriods = mutableListOf<TimePeriod>()
		val actions = mutableListOf<ActionModel>()
		val mappings = mutableListOf<TriggerActionMap>()

		actions.add(action)
		mappings.add(TriggerActionMap(0, trigger.getTriggerId(), type, action.id, null))
		timePeriods.add(TimePeriod(0, action.id, 0, Long.MAX_VALUE))

		for (i in 1..20) {
			val a = newAction()
			actions.add(a)
			mappings.add(TriggerActionMap(0, string(), Trigger.Type.Enter, a.id, null))
			timePeriods.add(TimePeriod(0, a.id, 0, Long.MAX_VALUE))
		}

		database!!.insertData(timePeriods, actions, mappings, listOf())
		return Pair(action.id, trigger.getTriggerId())
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
						   0,
						   false)
	}

	private fun uuid(): UUID {
		return UUID.randomUUID()
	}

	private fun string(): String {
		return uuid().toString().replace("-", "")
	}
}
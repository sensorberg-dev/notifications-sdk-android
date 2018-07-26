package com.sensorberg.notifications.sdk.internal

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.sensorberg.notifications.sdk.internal.model.*
import com.sensorberg.notifications.sdk.internal.storage.ActionDao
import com.sensorberg.notifications.sdk.internal.storage.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class DatabaseTests {

	private val random = Random()
	private var actionDao: ActionDao? = null
	private var db: AppDatabase? = null

	@Before
	fun createDb() {
		val context = InstrumentationRegistry.getTargetContext()
		db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
		actionDao = db!!.actionDao()
	}

	@After
	fun closeDb() {
		db!!.clearAllTables()
		db!!.close()
	}

	@Test
	fun findActionForEnterTrigger() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.Enter)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Enter)

		assertEquals(1, query.size)
		assertEquals(actionId, query[0].id)

	}

	@Test
	fun findActionForExitTrigger() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.Exit)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Exit)

		assertEquals(1, query.size)
		assertEquals(actionId, query[0].id)

	}

	@Test
	fun findActionForEnterOrExitTrigger() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.EnterOrExit)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Enter, Trigger.Type.EnterOrExit)

		assertEquals(1, query.size)
		assertEquals(actionId, query[0].id)

	}

	@Test
	fun dontFindActionForOppositeTrigger() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.Enter)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Exit, Trigger.Type.EnterOrExit)

		assertEquals(0, query.size)
	}

	@Test
	fun dontFindActionForOppositeTrigger2() {
		val dao = actionDao!!

		val (actionId, triggerId) = fillDb(Trigger.Type.Exit)
		val query: List<ActionQueryModel> = dao.getActionsForTrigger(triggerId, System.currentTimeMillis(), Trigger.Type.Enter, Trigger.Type.EnterOrExit)

		assertEquals(0, query.size)
	}

	@Test
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

		db!!.insertData(timePeriods, actions, mappings, listOf())

		val q1: List<ActionQueryModel> = dao.getActionsForTrigger(t1.getTriggerId(), System.currentTimeMillis(), Trigger.Type.Enter, Trigger.Type.EnterOrExit)
		val q2: List<ActionQueryModel> = dao.getActionsForTrigger(t2.getTriggerId(), System.currentTimeMillis(), Trigger.Type.Exit, Trigger.Type.EnterOrExit)

		assertEquals(action.id, q1[0].id)
		assertEquals(action.id, q2[0].id)

	}

	@Test
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
		db!!.insertData(timePeriods, actions, mappings, listOf())

		var query: List<ActionQueryModel> = dao.getActionsForTrigger(t1.getTriggerId(), System.currentTimeMillis(), Trigger.Type.Enter, Trigger.Type.EnterOrExit)
		assertEquals(3, query.size)

		query = dao.getActionsForTrigger(t1.getTriggerId(), System.currentTimeMillis(), Trigger.Type.Exit, Trigger.Type.EnterOrExit)
		assertEquals(1, query.size)
	}

	private fun fillDb(type: Trigger.Type): Pair<String, String> {

		val dao = actionDao!!

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

		db!!.insertData(timePeriods, actions, mappings, listOf())
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

	private fun short(): Short {
		return random.nextInt(Short.MAX_VALUE.toInt()).toShort()
	}

	private fun uuid(): UUID {
		return UUID.randomUUID()
	}

	private fun string(): String {
		return uuid().toString().replace("-", "")
	}
}
package com.sensorberg.notifications.sdk.internal

import androidx.room.Room
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.internal.model.DelayedActionModel
import com.sensorberg.notifications.sdk.internal.storage.DelayedActionDao
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class DelayedActionDaoTest {

	private lateinit var database: SdkDatabase
	private lateinit var dao: DelayedActionDao
	private lateinit var action: Action
	@Before
	fun createDb() {
		val context = InstrumentationRegistry.getTargetContext()
		database = Room.inMemoryDatabaseBuilder(context, SdkDatabase::class.java).build()
		dao = database.delayedActionDao()
		action = Action(UUID.randomUUID().toString(),
						UUID.randomUUID().toString(),
						UUID.randomUUID().toString(),
						UUID.randomUUID().toString(),
						UUID.randomUUID().toString(),
						UUID.randomUUID().toString(),
						UUID.randomUUID().toString(),
						UUID.randomUUID().toString())
	}

	@Test fun insert_and_retrieve_test() {
		dao.insert(DelayedActionModel.fromAction(action))
		val retrieved = dao.get(action.instanceId)
		assertEquals(action, DelayedActionModel.toAction(retrieved))
	}

	@Test fun insert_and_delete_test() {
		dao.insert(DelayedActionModel.fromAction(action))
		dao.delete(DelayedActionModel.fromAction(action))
		assertNull(dao.get(action.instanceId))
	}

}
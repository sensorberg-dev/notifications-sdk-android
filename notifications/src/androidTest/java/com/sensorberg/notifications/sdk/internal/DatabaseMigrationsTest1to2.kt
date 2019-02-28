package com.sensorberg.notifications.sdk.internal

import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.room.testing.MigrationTestHelper
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.sensorberg.notifications.sdk.internal.storage.DatabaseMigrations
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationsTest1to2 {

	private val TEST_DB = "migration-test"
	@get:Rule val helper: MigrationTestHelper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
																	SdkDatabase::class.java.canonicalName,
																	FrameworkSQLiteOpenHelperFactory())

	@Test fun migrate1To2() {
		var db = helper.createDatabase(TEST_DB, 1)
		// 1 to 2 is just adding a new table, so we're mostly looking for non-crashes
		// we'll add data to some simple table just for the sake of it
		val id = UUID.randomUUID().toString()
		val timestamp = System.currentTimeMillis()
		db.execSQL("INSERT INTO table_visible_beacons (id, timestamp) VALUES ('$id', $timestamp)")
		db.close()
		db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.migration1to2)
		val cursor = db.query("SELECT * FROM table_visible_beacons")
		assertEquals(1, cursor.count)
		assertTrue(cursor.moveToFirst())
		assertEquals(id, cursor.getString(cursor.getColumnIndex("id")))
		assertEquals(timestamp, cursor.getLong(cursor.getColumnIndex("timestamp")))
	}
}
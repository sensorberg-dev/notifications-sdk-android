package com.sensorberg.notifications.sdk.internal.storage

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.migration.Migration

object DatabaseMigrations {

	internal val migrations by lazy { arrayOf(migration1_2) }

	/**
	 * Migration 1 -> 2
	 * added table_beacons_registration to the database
	 */
	private val migration1_2: Migration by lazy {
		object : Migration(1, 2) {
			override fun migrate(database: SupportSQLiteDatabase) {
				database.execSQL("CREATE TABLE IF NOT EXISTS `table_beacons_registration` (`id` TEXT NOT NULL, `proximityUuid` TEXT NOT NULL, `major` INTEGER NOT NULL, `minor` INTEGER NOT NULL, `type` INTEGER NOT NULL, PRIMARY KEY(`id`))")
			}
		}
	}
}
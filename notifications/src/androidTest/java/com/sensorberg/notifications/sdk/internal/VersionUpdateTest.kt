package com.sensorberg.notifications.sdk.internal

import android.content.Context
import androidx.test.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test

class VersionUpdateTest {

	@Test fun on_version_update_updates() {
		val context = InstrumentationRegistry.getContext()
		val prefs = context.getSharedPreferences("prefs_encryptor_test", Context.MODE_PRIVATE)
		prefs.edit().clear().commit()

		val update1 = VersionUpdate.check(prefs, "2.7.6")
		Assert.assertTrue(update1.shouldMigrateSetEnabled)

		val update2 = VersionUpdate.check(prefs, "2.7.6")
		Assert.assertFalse(update2.shouldMigrateSetEnabled)

		val update3 = VersionUpdate.check(prefs, "2.7.7")
		Assert.assertFalse(update3.shouldMigrateSetEnabled)

		prefs.edit().clear().commit()
	}

}
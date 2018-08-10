package com.sensorberg.notifications.sdk.internal

import android.content.SharedPreferences
import timber.log.Timber

internal class VersionUpdate
private constructor(val shouldMigrateSetEnabled: Boolean) {

	companion object {
		private const val KEY_VERSION = "com.sensorberg.notifications.sdk.pref_sdk_version"
		fun check(prefs: SharedPreferences, currentVersion: String): VersionUpdate {
			val savedVersion = prefs.getString(KEY_VERSION, null)
			Timber.v("Current version $currentVersion. Saved version $savedVersion")
			prefs.edit().putString(KEY_VERSION, currentVersion).apply()
			return VersionUpdate(savedVersion == null)
		}
	}
}
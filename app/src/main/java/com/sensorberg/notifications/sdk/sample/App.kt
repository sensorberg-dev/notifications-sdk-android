package com.sensorberg.notifications.sdk.sample

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.sensorberg.notifications.sdk.NotificationsSdk
import timber.log.Timber

class App : Application() {

	lateinit var sdk: NotificationsSdk

	override fun onCreate() {
		super.onCreate()

		Timber.plant(
				Timber.DebugTree()/*,
				DebugTree("NotificationsSdk"),
				FileLogTree(File(filesDir, "logs/").absolutePath, 1, 3)*/)

		getSharedPreferences("notifications-sdk", Context.MODE_PRIVATE)
			.registerOnSharedPreferenceChangeListener(preferencesChange)

		sdk = NotificationsSdk.with(this).run {
			return@run if (true) {
				enableHttpLogs()
				setApiKey(BuildConfig.API_KEY)
				setBaseUrl(BuildConfig.BASE_URL)
				build()
			} else { // use this to manual test the empty() SDK
				empty()
			}
		}
	}

	private val preferencesChange = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
		Timber.d("OnPreferencesChanged. $key:${sharedPreferences.all[key]}")
	}
}
package com.sensorberg.notifications.sdk.sample

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.timberextensions.tree.DebugTree
import com.sensorberg.timberextensions.tree.FileLogTree
import timber.log.Timber
import java.io.File

class App : Application() {

	lateinit var sdk: NotificationsSdk

	override fun onCreate() {
		super.onCreate()

		Timber.plant(
				/*Timber.DebugTree(),*/
				DebugTree("NotificationsSdk"),
				FileLogTree(File(filesDir, "logs/").absolutePath, 1, 3))

		getSharedPreferences("notifications-sdk", Context.MODE_PRIVATE)
			.registerOnSharedPreferenceChangeListener(preferencesChange)

		sdk = NotificationsSdk.with(this)
			.enableHttpLogs()
			.setApiKey(KEY)
			.setBaseUrl(STAGING)
			.build()

	}

	companion object {
		private const val KEY = "67eebdd2cda9bcda000ea32c980599116c3e7621072564e18830a8cdb0528411"
		private const val STAGING = "https://staging.sensorberg-cdn.io"

		private const val KEY_PORTAL = "dd715fd0f49e6902516958b0777327b73c6a018d253f2ab436835f383b425eb4"
		private const val PORTAL_CDN = "https://portal.sensorberg-cdn.com"
		private const val PORTAL = "https://portal.sensorberg.com"
	}

	private val preferencesChange = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
		Timber.d("OnPreferencesChanged. $key:${sharedPreferences.all[key]}")
	}
}
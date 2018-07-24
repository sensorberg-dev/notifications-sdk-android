package com.sensorberg.notifications.sdk.sample

import android.app.Application
import android.content.Context
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.timberextensions.tree.DebugTree
import com.sensorberg.timberextensions.tree.FileLogTree
import timber.log.Timber

class App : Application() {

	lateinit var sdk: NotificationsSdk

	override fun onCreate() {
		super.onCreate()

		Timber.plant(
				DebugTree("NotificationsSdk"),
				FileLogTree(getDir("logs", Context.MODE_PRIVATE).absolutePath, 1, 3))

		sdk = NotificationsSdk.with(this)
			.enableHttpLogs()
			.setApiKey(KEY)
			.setBaseUrl(STAGING)
			.build()

	}

	companion object {
		private const val KEY = "67eebdd2cda9bcda000ea32c980599116c3e7621072564e18830a8cdb0528411"
		private const val STAGING = "https://staging.sensorberg-cdn.io"
	}
}
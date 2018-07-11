package com.sensorberg.notifications.sdk.internal

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.android.gms.common.GoogleApiAvailability
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.backendsdkv2.BackendSdkV2
import com.sensorberg.notifications.sdk.internal.common.Backend
import com.sensorberg.notifications.sdk.internal.common.storage.AppDatabase
import com.sensorberg.notifications.sdk.internal.common.storage.Storage
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.squareup.moshi.Moshi
import org.koin.dsl.module.applicationContext
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class InjectionModule(private val app: Application, private val apiKey: String, private val log: Boolean) {

	companion object {
		const val notificationPreferences = "com.sensorberg.notifications.sdk.Preferences"
		const val notificationApp = "com.sensorberg.notifications.sdk.App"
		const val notificationContext = "com.sensorberg.notifications.sdk.Context"
	}

	val module = listOf(applicationContext {
		context(NotificationsSdk.notificationSdkContext) {
			bean(notificationApp) { app }
			bean(notificationContext) { app as Context }
			bean { Executors.newFixedThreadPool(3) as Executor } // used for DB operations
			bean { Storage.createDatabase(get(notificationApp)) }
			bean { get<AppDatabase>().actionDao() }
			bean(notificationPreferences) { get<Application>(notificationApp).getSharedPreferences("notifications-sdk", Context.MODE_PRIVATE) }
			bean { TriggerProcessor(get(), get(), get(), get(notificationApp)) }
			bean { ActionLauncher(get(notificationApp), get()) }
			bean {
				WorkManager.initialize(get(notificationContext), Configuration.Builder().build())
				WorkUtils(WorkManager.getInstance()!!, get(notificationApp), get())
			}
			bean { GoogleApiAvailability.getInstance() }
			bean { Moshi.Builder().build() }
			bean {

				val prefs = get<SharedPreferences>(notificationPreferences)
				var installId = prefs.getString(NotificationsSdkImpl.PREF_INSTALL_ID, null)
				if (installId == null) {
					installId = UUID.randomUUID().toString().replace("-", "").toLowerCase()
					prefs.edit().putString(NotificationsSdkImpl.PREF_INSTALL_ID, installId).apply()
				}

				return@bean BackendSdkV2(get(notificationApp),
										 apiKey,
										 installId,
										 log) as Backend

//				return@bean TestBackend(TestBackend.ENTER_EXIT_IMMEDIATE) as Backend

			}
		}
	})
}
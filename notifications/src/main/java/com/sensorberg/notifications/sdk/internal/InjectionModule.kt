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
		const val preferencesBean = "com.sensorberg.notifications.sdk.Preferences"
		const val appBean = "com.sensorberg.notifications.sdk.App"
		const val contextBean = "com.sensorberg.notifications.sdk.Context"
		const val executorBean = "com.sensorberg.notifications.sdk.Executor"
	}

	val module = listOf(applicationContext {
		context(NotificationsSdk.notificationSdkContext) {
			bean(appBean) { app }
			bean(contextBean) { app as Context }
			bean(executorBean) { Executors.newFixedThreadPool(3) as Executor } // used for DB operations
			bean { Storage.createDatabase(get(appBean)) }
			bean { get<AppDatabase>().actionDao() }
			bean(preferencesBean) { get<Application>(appBean).getSharedPreferences("notifications-sdk", Context.MODE_PRIVATE) }
			bean { TriggerProcessor(get(), get(), get(), get(appBean)) }
			bean { ActionLauncher(get(appBean), get()) }
			bean {
				WorkManager.initialize(get(contextBean), Configuration.Builder().build())
				WorkUtils(WorkManager.getInstance()!!, get(appBean), get())
			}
			bean { GoogleApiAvailability.getInstance() }
			bean { Moshi.Builder().build() }
			bean {

				val prefs = get<SharedPreferences>(preferencesBean)
				var installId = prefs.getString(NotificationsSdkImpl.PREF_INSTALL_ID, null)
				if (installId == null) {
					installId = UUID.randomUUID().toString().replace("-", "").toLowerCase()
					prefs.edit().putString(NotificationsSdkImpl.PREF_INSTALL_ID, installId).apply()
				}

				return@bean BackendSdkV2(get(appBean),
										 apiKey,
										 installId,
										 log) as Backend

//				return@bean TestBackend(TestBackend.ENTER_EXIT_IMMEDIATE) as Backend

			}
		}
	})
}
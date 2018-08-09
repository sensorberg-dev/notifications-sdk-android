package com.sensorberg.notifications.sdk.internal

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.android.gms.common.GoogleApiAvailability
import com.sensorberg.notifications.sdk.BuildConfig
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.backend.Backend
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.BackendSdkV2
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.koin.dsl.module.applicationContext
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class InjectionModule(private val app: Application, private val apiKey: String, private val baseUrl: String, private val log: Boolean) {

	companion object {
		const val preferencesBean = "com.sensorberg.notifications.sdk.Preferences"
		const val appBean = "com.sensorberg.notifications.sdk.App"
		const val executorBean = "com.sensorberg.notifications.sdk.Executor"
		const val googleApiAvailabilityBean = "com.sensorberg.notifications.sdk.googleApiAvailability"
		const val moshiBean = "com.sensorberg.notifications.sdk.moshi"
	}

	internal val module = listOf(applicationContext {
		context(NotificationsSdk.notificationSdkContext) {
			bean(appBean) { app }
			bean(executorBean) { Executors.newFixedThreadPool(3) as Executor } // used for DB operations
			bean { SdkDatabase.createDatabase(get(appBean)) }
			bean { get<SdkDatabase>().actionDao() }
			bean { get<SdkDatabase>().geofenceDao() }
			bean { get<SdkDatabase>().beaconDao() }
			bean(preferencesBean) { get<Application>(appBean).getSharedPreferences("notifications-sdk", Context.MODE_PRIVATE) }
			bean { TriggerProcessor(get(), get(), get(), get(appBean)) }
			bean { ActionLauncher(get(appBean), get(), get(preferencesBean)) }
			bean { VersionUpdate.check(get(preferencesBean), BuildConfig.VERSION_NAME) }
			bean { SdkEnableHandler() }
			bean(googleApiAvailabilityBean) { GoogleApiAvailability.getInstance() }
			bean { WorkUtils(WorkManager.getInstance(), app, get(), get(preferencesBean)) }
			bean(moshiBean) {
				Moshi.Builder()
					.add(UuidObjectAdapter)
					.add(TriggerTypeObjectAdapter)
					.build()
			}
			bean {

				val prefs = get<SharedPreferences>(preferencesBean)
				var installId = prefs.getString(NotificationsSdkImpl.PREF_INSTALL_ID, null)
				if (installId == null) {
					installId = UUID.randomUUID().toString().replace("-", "").toLowerCase()
					prefs.edit().putString(NotificationsSdkImpl.PREF_INSTALL_ID, installId).apply()
				}

				return@bean BackendSdkV2(app,
										 baseUrl,
										 apiKey,
										 installId,
										 log) as Backend

			}
		}
	})
}

private object UuidObjectAdapter {
	@FromJson fun toUUID(value: String): UUID {
		return UUID.fromString(value)
	}

	@ToJson fun fromUUID(value: UUID): String {
		return value.toString()
	}
}

private object TriggerTypeObjectAdapter {
	@FromJson fun toTriggerType(value: Int): Trigger.Type {
		return when (value) {
			0 -> Trigger.Type.Enter
			1 -> Trigger.Type.Exit
			2 -> Trigger.Type.EnterOrExit
			else -> throw IllegalArgumentException("Trigger.Type cannot be $value")
		}
	}

	@ToJson fun fromTriggerType(value: Trigger.Type): Int {
		return when (value) {
			Trigger.Type.Enter -> 0
			Trigger.Type.Exit -> 1
			Trigger.Type.EnterOrExit -> 2
		}
	}
}
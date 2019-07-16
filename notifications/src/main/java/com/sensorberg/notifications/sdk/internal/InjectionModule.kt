package com.sensorberg.notifications.sdk.internal

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.work.WorkManager
import com.google.android.gms.common.GoogleApiAvailability
import com.sensorberg.notifications.sdk.BuildConfig
import com.sensorberg.notifications.sdk.internal.backend.Backend
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.BackendSdkV2
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.koin.core.module.Module
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

internal class InjectionModule(private val app: Application, private val apiKey: String, private val baseUrl: String, private val log: Boolean) {

	val module: Module = org.koin.dsl.module {
		single { app }
		single { Executors.newFixedThreadPool(3) as Executor } // used for DB operations
		single { SdkDatabase.createDatabase(get()) }
		single { get<SdkDatabase>().actionDao() }
		single { get<SdkDatabase>().geofenceDao() }
		single { get<SdkDatabase>().beaconDao() }
		single { get<Application>().getSharedPreferences("notifications-sdk", Context.MODE_PRIVATE) }
		single { TriggerProcessor(get(), get(), get(), get()) }
		single { ActionLauncher(get(), get(), get()) }
		single { VersionUpdate.check(get(), BuildConfig.VERSION_NAME) }
		single { SdkEnableHandler() }
		single { GoogleApiAvailability.getInstance() }
		single { WorkUtils(WorkManager.getInstance(), app, get()) }
		single {
			Moshi.Builder()
				.add(UuidObjectAdapter)
				.add(TriggerTypeObjectAdapter)
				.build()
		}
		single {

			val prefs: SharedPreferences = get()
			var installId = prefs.getString(NotificationsSdkImpl.PREF_INSTALL_ID, null)
			if (installId == null) {
				installId = UUID.randomUUID().toString().replace("-", "").toLowerCase()
				prefs.edit().putString(NotificationsSdkImpl.PREF_INSTALL_ID, installId).apply()
			}

			return@single BackendSdkV2(app,
									   baseUrl,
									   apiKey,
									   installId,
									   log) as Backend

		}
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
}
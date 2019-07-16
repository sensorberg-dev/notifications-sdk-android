package com.sensorberg.notifications.sdk.internal

import android.app.Application
import android.content.SharedPreferences
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import org.koin.core.inject
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.MessagesOptions
import com.google.android.gms.nearby.messages.NearbyPermissions
import com.sensorberg.notifications.sdk.internal.receivers.BeaconReceiver
import com.sensorberg.notifications.sdk.internal.receivers.GeofenceReceiver
import com.sensorberg.notifications.sdk.internal.work.delegate.RegistrationHelper
import com.sensorberg.notifications.sdk.internal.storage.SdkDatabase
import com.sensorberg.notifications.sdk.internal.work.SyncWork
import com.sensorberg.notifications.sdk.internal.work.UploadWork
import timber.log.Timber
import java.util.concurrent.Executor

internal class SdkEnableHandler : NotificationSdkComponent {

	private val app: Application by inject()
	private val sdkDatabase: SdkDatabase by inject()
	private val versionUpdate: VersionUpdate by inject()
	private val prefs: SharedPreferences by inject()
	private val workUtils: WorkUtils by inject()
	private val executor: Executor by inject()

	fun onNotificationSdkInit() {

		// the SDK ships default all components to be disabled,
		// the Builder also disable all in case of missing requirements
		// here they get enabled again
		if (isEnabled() && !isComponentsEnabled(app)) {
			Timber.d("Enabling broadcast receivers")
			setAllComponentsEnable(true, app)
		}

		// if we're migrating, but it's already enabled,
		// all workers are executing and we don't have to do anything
		// so we only call enableExecution if it's currently disabled
		if (versionUpdate.shouldMigrateSetEnabled && !isEnabled()) {
			Timber.d("Version update, enable execution")
			enableExecution(false)
		}
	}

	fun setEnabled(enabled: Boolean) {
		if (isEnabled() == enabled) {
			// no change, just return
			return
		}

		// save it
		prefs.edit().putBoolean(NotificationsSdkImpl.PREF_ENABLED, enabled).apply()

		// enable/disabe stuff
		enableExecution(enabled)
	}

	fun isEnabled(): Boolean {
		return prefs.getBoolean(NotificationsSdkImpl.PREF_ENABLED, true)
	}

	private fun enableExecution(enable: Boolean) {
		executor.execute {
			if (enable) {
				setAllComponentsEnable(true, app) // enable the manifest stuff
				if (app.haveLocationPermission()) {
					Timber.i("SDK enabled, scheduling work execution")
					workUtils.executeAndSchedule(SyncWork::class.java)
					workUtils.schedule(UploadWork::class.java)
				}
			} else {
				Timber.i("SDK disabled, stop workers, disable components, clear database and unregister from Google Play Services")
				workUtils.disableAll() // disable the workers
				setAllComponentsEnable(false, app) // disable the manifest stuff
				sdkDatabase.clearAllTables() // clear the data
				unregisterFromGooglePlayServices(app) // unregister from Google Play
			}
		}
	}

	companion object {

		private const val componentPackage = "com.sensorberg.notifications.sdk.internal.receivers."
		private val components = listOf("BeaconReceiver", "GeofenceReceiver", "BootReceiver")

		internal fun unregisterFromGooglePlayServices(app: Application) {
			val nearby = Nearby.getMessagesClient(app, MessagesOptions.Builder()
				.setPermissions(NearbyPermissions.BLE)
				.build())
			val geofence = GeofencingClient(app)
			val task = GoogleApiAvailability.getInstance().checkApiAvailability(nearby, geofence)
				.onSuccessTask { nearby.unsubscribe(BeaconReceiver.generateUnsubscribePendingIntent(app)) }
				.onSuccessTask { geofence.removeGeofences(GeofenceReceiver.generatePendingIntent(app)) }
			RegistrationHelper.awaitResult("SdkDisabled", 30, task)
		}

		internal fun setAllComponentsEnable(enabled: Boolean, context: Context) {
			components.forEach { setComponentEnable(enabled, context, "$componentPackage$it") }
		}

		internal fun isComponentsEnabled(context: Context): Boolean {
			var isEnabled = true
			components.forEach {
				val component = ComponentName(context, it)
				isEnabled = isEnabled && context.packageManager.getComponentEnabledSetting(component) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
			}
			return isEnabled
		}

		private fun setComponentEnable(enabled: Boolean, context: Context, className: String) {
			try {
				val component = ComponentName(context, className)
				context.packageManager.setComponentEnabledSetting(
						component,
						if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
						PackageManager.DONT_KILL_APP)
			} catch (e: Exception) {
				// don't care, just don't crash this
			}
		}
	}
}
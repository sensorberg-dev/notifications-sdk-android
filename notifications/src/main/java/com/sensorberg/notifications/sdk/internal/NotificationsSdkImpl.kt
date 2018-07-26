package com.sensorberg.notifications.sdk.internal

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.Conversion
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.backend.Backend
import com.sensorberg.notifications.sdk.internal.model.toActionConversion
import com.sensorberg.notifications.sdk.internal.storage.ActionDao
import com.sensorberg.notifications.sdk.internal.work.SyncWork
import com.sensorberg.notifications.sdk.internal.work.UploadWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
internal class NotificationsSdkImpl : NotificationsSdk, KoinComponent {

	private val app: Application by inject(InjectionModule.appBean)
	private val prefs: SharedPreferences by inject(InjectionModule.preferencesBean)
	private val backend: Backend by inject()
	private val moshi: Moshi by inject()
	private val workUtils: WorkUtils by inject()
	private val dao: ActionDao by inject()
	private val executor: Executor by inject(InjectionModule.executorBean)
	private val mapAdapter: JsonAdapter<Map<String, String>> = moshi.adapter<Map<String, String>>(MAP_TYPE)

	init {
		// set backend saved data
		backend.setAdvertisementId(prefs.getString(PREF_AD_ID, null))
		prefs.getString(PREF_ATTR, null)?.let {
			backend.setAttributes(mapAdapter.fromJson(it))
		}
		// await location
		awaitForLocationPermission()
	}

	private fun awaitForLocationPermission() {
		Timber.i("Awaiting for location permission")
		val callback = object : ActivityLifecycleCallbacksAdapter() {
			override fun onActivityResumed(activity: Activity) {
				if (app.haveLocationPermission()) {
					Timber.i("Location permission granted")
					app.unregisterActivityLifecycleCallbacks(this)
					// workUtils.execute(SyncWork::class.java) // for testing only
					// workUtils.execute(UploadWork::class.java) // for testing only
					workUtils.schedule(SyncWork::class.java) //schedule SyncWork for periodic work
					workUtils.schedule(UploadWork::class.java)
				}
			}
		}
		app.registerActivityLifecycleCallbacks(callback)
	}

	override fun setConversion(action: Action, conversion: Conversion) {
		executor.execute {
			dao.insertActionConversion(action.toActionConversion(conversion, app.getLastLocation()))
		}
	}

	override fun setAdvertisementId(adId: String?) {
		if (prefs.set(PREF_AD_ID, adId)) {
			backend.setAdvertisementId(adId)
			// ad-id changed and backend was already requested,
			// re-request backend to reload data
			workUtils.execute(SyncWork::class.java)
		}
	}

	override fun setAttributes(attributes: Map<String, String>?) {
		val attrs = if (attributes == null) {
			null
		} else {
			if (!Validator.isInputValid(attributes)) {
				throw IllegalArgumentException("Attributes can contain only alphanumerical characters and underscore")
			}
			mapAdapter.toJson(attributes)
		}

		if (prefs.set(PREF_ATTR, attrs)) {
			backend.setAttributes(attributes)
			// attributes changed and backend was already requested,
			// re-request backend to reload data
			workUtils.execute(SyncWork::class.java)
		}
	}

	companion object {
		internal const val PREF_INSTALL_ID = "installation_id"
		internal const val PREF_AD_ID = "advertisement_id"
		internal const val PREF_ATTR = "attributes"
		private val MAP_TYPE = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)

	}

	internal object BeaconRegistrationHack {

		private var registrationTime = 0L

		fun isRecent(): Boolean {
			return SystemClock.elapsedRealtime() - registrationTime < 5000
		}

		fun onRegistration() {
			registrationTime = SystemClock.elapsedRealtime()
		}
	}
}
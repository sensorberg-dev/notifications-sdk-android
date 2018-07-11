package com.sensorberg.notifications.sdk.internal

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.Conversion
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.notifications.sdk.internal.common.Backend
import com.sensorberg.notifications.sdk.internal.common.model.toActionConversion
import com.sensorberg.notifications.sdk.internal.common.storage.ActionDao
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
					app.unregisterActivityLifecycleCallbacks(this)
					workUtils.execute(SyncWork::class.java, WorkUtils.SYNC_WORK)
					workUtils.schedule(UploadWork::class.java, WorkUtils.UPLOAD_WORK)
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
			workUtils.execute(SyncWork::class.java, WorkUtils.SYNC_WORK)
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
			workUtils.execute(SyncWork::class.java, WorkUtils.SYNC_WORK)
		}
	}

	companion object {
		internal const val PREF_INSTALL_ID = "installation_id"
		internal const val PREF_AD_ID = "advertisement_id"
		internal const val PREF_ATTR = "attributes"
		private val MAP_TYPE = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
	}

	fun SharedPreferences.set(key: String, value: String?): Boolean {
		if (getString(key, null) == value) { // no change
			return false
		}

		if (value == null) {
			edit().remove(key).apply()
		} else {
			edit().putString(key, value).apply()
		}

		return true
	}
}
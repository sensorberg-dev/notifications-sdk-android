package com.sensorberg.notifications.sdk

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.URLUtil
import androidx.work.WorkManager
import com.sensorberg.notifications.sdk.internal.*
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import org.koin.standalone.StandAloneContext
import timber.log.Timber

interface NotificationsSdk {

	fun setAdvertisementId(adId: String?)

	@Throws(IllegalArgumentException::class) fun setAttributes(attributes: Map<String, String>?)

	fun setConversion(action: Action, conversion: Conversion)

	companion object {

		const val ACTION_RECEIVER = "com.sensorberg.notifications.sdk.ACTION_RECEIVER"

		const val notificationSdkContext = "com.sensorberg.notifications.sdk"

		fun with(context: Context): Builder {
			return Builder(context.applicationContext as Application)
		}

		fun printAllSdkWorkerStates() {
			val statusesForUniqueWork = WorkManager.getInstance()?.getStatusesByTag(WorkUtils.WORKER_TAG)
			statusesForUniqueWork?.observeForever { workStatuses ->
				workStatuses?.forEach { workStatus ->
					Timber.d(workStatus.toString())
				}
			}
		}

		fun extractAction(intent: Intent): Action {
			return intent.toAction()
		}
	}

	class Builder internal constructor(private val app: Application) {

		private var log = false
		private var apiKey: String = ""
		private var baseUrl: String = "https://portal.sensorberg-cdn.com"

		fun enableHttpLogs(): Builder {
			log = true
			return this
		}

		fun setApiKey(apiKey: String): NotificationsSdk.Builder {
			this.apiKey = apiKey
			return this
		}

		fun setBaseUrl(baseUrl: String): NotificationsSdk.Builder {
			this.baseUrl = baseUrl
			return this
		}

		fun build(): NotificationsSdk {

			if (apiKey.isEmpty()) {
				throw IllegalArgumentException("apiKey is empty - use setApiKey to provide a apiKey")
			}
			if (baseUrl.isEmpty() || !URLUtil.isNetworkUrl(baseUrl)) {
				throw IllegalArgumentException("baseUrl is invalid - use baseUrl to provide a valid baseUrl")
			}

			val osVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
			val gpsAvailable = app.isGooglePlayServicesAvailable()
			return if (osVersion && gpsAvailable) {
				StandAloneContext.loadKoinModules(InjectionModule(app, apiKey, baseUrl, log).module)
				NotificationsSdkImpl()
			} else {
				Timber.w("NotificationsSdk disabled. Android Version(${Build.VERSION.SDK_INT}). Google Play Services (${if (gpsAvailable) "" else "un"}available)")
				EmptyImpl()
			}
		}
	}
}
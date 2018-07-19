package com.sensorberg.notifications.sdk

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.work.WorkManager
import com.sensorberg.notifications.sdk.internal.EmptyImpl
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.NotificationsSdkImpl
import com.sensorberg.notifications.sdk.internal.isGooglePlayServicesAvailable
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.sensorberg.timberextensions.tree.DebugTree
import org.koin.standalone.StandAloneContext
import timber.log.Timber

interface NotificationsSdk {

	fun setAdvertisementId(adId: String?)

	@Throws(IllegalArgumentException::class) fun setAttributes(attributes: Map<String, String>?)

	fun setConversion(action: Action, conversion: Conversion)

	companion object {

		const val ACTION_PRESENT = "com.sensorberg.notifications.sdk.PRESENT_NOTIFICATION"

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
	}

	open interface OnActionListener {
		fun onActionReceived(action: Action)
	}

	class Builder internal constructor(private val app: Application) {

		private var log = false
		private var tree: DebugTree? = null
		private var apiKey: String = ""

		var actionListener = object : NotificationsSdk.OnActionListener {
			override fun onActionReceived(action: Action) {
				//default action listener does nothing
			}
		}

		fun enableLogs(): Builder {
			log = true
			tree = DebugTree("NotificationsSdk")
			Timber.plant(tree!!)
			return this
		}

		fun setApiKey(apiKey: String): NotificationsSdk.Builder {
			this.apiKey = apiKey
			return this
		}

		fun build(): NotificationsSdk {
			if (apiKey.isEmpty()) {
				throw IllegalArgumentException("apiKey is empty - use setApiKey to provide a apiKey")
			}

			val osVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
			val gpsAvailable = app.isGooglePlayServicesAvailable()
			return if (osVersion && gpsAvailable) {
				StandAloneContext.loadKoinModules(InjectionModule(app, apiKey, log, actionListener).module)
				NotificationsSdkImpl()
			} else {
				if (log) {
					Timber.w("NotificationsSdk disabled. Android Version(${Build.VERSION.SDK_INT}). Google Play Services (${if (gpsAvailable) "" else "un"}available)")
					tree?.let { Timber.uproot(it) } // logs can go away after this message
				}
				EmptyImpl()
			}
		}

		fun setOnActionListener(listener: NotificationsSdk.OnActionListener): NotificationsSdk.Builder {
			actionListener = listener
			return this
		}
	}
}
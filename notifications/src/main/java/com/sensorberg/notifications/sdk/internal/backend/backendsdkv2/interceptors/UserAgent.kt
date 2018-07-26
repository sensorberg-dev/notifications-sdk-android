package com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.interceptors

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.sensorberg.notifications.sdk.BuildConfig
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.Transport
import okhttp3.Request
import timber.log.Timber
import java.net.URLEncoder

internal class UserAgent(context: Context) : AddHeader {

	private val userAgent: String

	override fun onHeader(builder: Request.Builder) {
		builder.header(Transport.HEADER_USER_AGENT, userAgent)
	}

	init {
		val packageName = context.packageName
		val pm = context.packageManager
		val ai = context.applicationInfo
		val appLabel = URLEncoder.encode(pm.getApplicationLabel(ai).toString())
		val appVersion = getAppVersionString(context)

		userAgent = "$appLabel/$packageName/$appVersion " +
				"(Android ${Build.VERSION.RELEASE} ${Build.CPU_ABI}) " +
				"(${Build.MANUFACTURER}:${Build.MODEL}:${Build.PRODUCT}) " +
				"Sensorberg Notifications SDK ${BuildConfig.VERSION_NAME}"

		Timber.i("Backend header User-Agent: $userAgent")
	}

	private fun getAppVersionString(context: Context): String {
		return try {
			val myInfo = context.packageManager.getPackageInfo(context.packageName, 0)
			URLEncoder.encode(myInfo.versionName) + "/" + myInfo.versionCode
		} catch (e: PackageManager.NameNotFoundException) {
			"<unknown>"
		} catch (e: NullPointerException) {
			"<unknown>"
		}
	}

}
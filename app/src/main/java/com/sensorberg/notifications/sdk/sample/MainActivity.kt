package com.sensorberg.notifications.sdk.sample

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.FileProvider.getUriForFile
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.Conversion
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.permissionbitte.BitteBitte
import com.sensorberg.permissionbitte.PermissionBitte
import timber.log.Timber
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), BitteBitte {

	private lateinit var notificationsSdk: NotificationsSdk

	override fun askNicer() {
		AlertDialog.Builder(this)
			.setTitle("Do it")
			.setMessage("You really have to accept the permission")
			.setPositiveButton("Sure") { _, _ -> PermissionBitte.ask(this, this) }
			.setNegativeButton("OK") { _, _ -> PermissionBitte.ask(this, this) }
			.setCancelable(false)
			.show()
	}

	override fun noYouCant() {
		PermissionBitte.goToSettings(this)
	}

	override fun yesYouCan() {

	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		PermissionBitte.ask(this, this)
		notificationsSdk = (application as App).sdk
		updateEnabledText()
		checkActionConversion(intent)
	}

	override fun onNewIntent(intent: Intent) {
		checkActionConversion(intent)
	}

	private fun checkActionConversion(intent: Intent?) {
		intent?.extras?.getParcelable<Action>("action")?.let {
			notificationsSdk.setConversion(it, Conversion.Success)
		}
	}

	fun onShareLogs(view: View) {
		File(filesDir, "logs/").listFiles().toList().sortedBy { it.absolutePath }.lastOrNull()?.let {
			val contentUri = getUriForFile(this, "com.sensorberg.notifications.sdk.sample.fileprovider", it)
			val shareIntent: Intent = Intent().apply {
				action = Intent.ACTION_SEND
				putExtra(Intent.EXTRA_STREAM, contentUri)
				type = "*/*"
			}
			startActivity(Intent.createChooser(shareIntent, "Share to:"))
		}
	}

	fun onClickPrint(view: View) {
		NotificationsSdk.printAllSdkWorkerStates()
	}

	fun onChangeAttrs(view: View) {
		val map = mutableMapOf<String, String>().apply {
			put("blz", UUID.randomUUID().toString().replace("-", "_"))
		}
		Timber.d("Adding blz = ${map["blz"]}")
		notificationsSdk.setAttributes(map)
	}

	fun onChangeAdId(view: View) {
		val ad = UUID.randomUUID().toString()
		Timber.d("Adding ad = $ad")
		notificationsSdk.setAdvertisementId(ad)
	}

	fun onEnabledChange(view: View) {
		if (notificationsSdk.isEnabled()) {
			notificationsSdk.setEnabled(false)
		} else {
			notificationsSdk.setEnabled(true)
		}
		updateEnabledText()
	}

	private fun updateEnabledText() {
		findViewById<TextView>(R.id.btn_enable).text = if (notificationsSdk.isEnabled()) "SDK enabled" else "SDK disabled"
	}
}

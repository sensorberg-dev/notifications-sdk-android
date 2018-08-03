package com.sensorberg.notifications.sdk.sample

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.permissionbitte.BitteBitte
import com.sensorberg.permissionbitte.PermissionBitte
import timber.log.Timber
import java.util.*

class MainActivity : AppCompatActivity(), BitteBitte {

	override fun askNicer() {
		AlertDialog.Builder(this)
			.setTitle("Do it")
			.setMessage("You really have to accept the permission")
			.setPositiveButton("Sure", { _, _ -> PermissionBitte.ask(this, this) })
			.setNegativeButton("OK", { _, _ -> PermissionBitte.ask(this, this) })
			.setCancelable(false)
			.show()
	}

	override fun noYouCant() {
		PermissionBitte.goToSettings(this)
	}

	override fun yesYouCan() {
		Toast.makeText(this, "Yey \uD83C\uDF89 !!!!", Toast.LENGTH_SHORT).show()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		PermissionBitte.ask(this, this)
	}

	fun onClickPrint(view: View) {
		NotificationsSdk.printAllSdkWorkerStates()
	}

	fun onChangeAttrs(view: View) {
		val map = mutableMapOf<String, String>().apply {
			put("blz", UUID.randomUUID().toString().replace("-", "_"))
		}
		Timber.d("Adding blz = ${map["blz"]}")
		(application as App).sdk.setAttributes(map)
	}

	fun onChangeAdId(view: View) {
		val ad = UUID.randomUUID().toString()
		Timber.d("Adding ad = $ad")
		(application as App).sdk.setAdvertisementId(ad)
	}
}

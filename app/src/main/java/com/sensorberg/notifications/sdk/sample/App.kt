package com.sensorberg.notifications.sdk.sample

import android.app.Application
import android.content.Context
import com.sensorberg.notifications.sdk.NotificationsSdk
import com.sensorberg.timberextensions.tree.DebugTree
import com.sensorberg.timberextensions.tree.FileLogTree
import timber.log.Timber

class App : Application() {
	/*

	TODO: dear Mirko:
	Manual testing will be required!!!

	- reboot the device: does beacons still work? do geofences get properly re-registered?
	- put the sync time to something like a 10minutes, run, kill the VM and leave the device. Does it get properly executed?
	- test the geofences! use a mock provider, setup a fence. reboot the device, kill the VM
	- disable location, enable again
	- register geofences when location get enabled again. Is it even possible on Nougat+ ?


	 */

	public lateinit var sdk: NotificationsSdk

	override fun onCreate() {
		super.onCreate()

		Timber.plant(
				DebugTree("NotificationsSdk"),
				FileLogTree(getDir("logs", Context.MODE_PRIVATE).absolutePath, 1, 3))

		sdk = NotificationsSdk.with(this)
			.enableLogs()
			.setApiKey(KEY)
			.setBaseUrl(STAGING)
			.build()

	}

	companion object {
		private const val KEY = "67eebdd2cda9bcda000ea32c980599116c3e7621072564e18830a8cdb0528411"
		private const val STAGING = "https://staging.sensorberg-cdn.io"
	}
}
package com.sensorberg.notifications.sdk.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Process
import androidx.work.Worker
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.sensorberg.notifications.sdk.Action
import timber.log.Timber

internal fun Application.haveLocationPermission(): Boolean {
	return checkPermission(
			Manifest.permission.ACCESS_FINE_LOCATION,
			android.os.Process.myPid(),
			Process.myUid()) == PackageManager.PERMISSION_GRANTED
}

internal fun Application.isGooglePlayServicesAvailable(): Boolean {
	return GoogleApiAvailability.getInstance()
		.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
}

internal fun Application.haveLocationProvider(): Boolean {
	if (!haveLocationPermission()) {
		return false
	}
	val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
	return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
		   lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

@SuppressLint("MissingPermission")
internal fun Application.getLastLocation(): Location? {
	if (!haveLocationPermission()) {
		return null
	}
	val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
	val providers = lm.getProviders(true)
	val locations = providers.mapNotNull { lm.getLastKnownLocation(it) }
	return if (locations.isEmpty()) {
		null
	} else {
		locations.sortedBy { it.accuracy }[0]
	}
}

private const val KEY_ACTION = "com.sensorberg.notifications.sdk.Action.key"
internal fun Action.writeToIntent(intent: Intent) {
	intent.putExtra(KEY_ACTION, this)
}

internal fun Intent.toAction(): Action {
	if (hasExtra(KEY_ACTION)) return getParcelableExtra(KEY_ACTION)
	else throw IllegalArgumentException("Intent does contain action $this")
}

internal fun SharedPreferences.set(key: String, value: String?): Boolean {
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

internal fun Worker.logStart() {
	Timber.i("Starting ${javaClass.simpleName}")
}

internal fun Worker.logResult(result: Worker.Result): Worker.Result {
	Timber.i("${javaClass.simpleName} result $result")
	return result
}
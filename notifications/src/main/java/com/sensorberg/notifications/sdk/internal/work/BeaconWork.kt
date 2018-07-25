package com.sensorberg.notifications.sdk.internal.work

import androidx.work.Worker
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.logStart
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.registration.BeaconRegistration
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class BeaconWork : Worker(), KoinComponent {

	private val moshi: Moshi by inject(InjectionModule.moshiBean)
	private val beaconsAdapter: JsonAdapter<List<Trigger.Beacon>> by lazy {
		val listMyData = Types.newParameterizedType(List::class.java, Trigger.Beacon::class.java)
		moshi.adapter<List<Trigger.Beacon>>(listMyData)
	}

	override fun doWork(): Worker.Result {
		logStart()
		val beacons = beaconsAdapter.fromJson(inputData.getExtras())!!
		return if (BeaconRegistration().execute(beacons) == Worker.Result.SUCCESS) {
			Worker.Result.SUCCESS
		} else {
			// for beacon registration we want this to keep retrying until it succeeds
			Worker.Result.RETRY
		}
	}
}
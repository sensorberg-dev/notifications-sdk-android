package com.sensorberg.notifications.sdk.internal.backendsdkv2.model

import org.junit.Before
import org.junit.Test

class HistoryBodyTest {

	@Before
	fun setUp() {
	}

	@Test
	fun test_location_to_geohash() {
		HistoryBody.extractGeoHash(1.0, 1.0, 1f)
	}
}
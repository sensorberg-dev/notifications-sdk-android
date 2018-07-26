package com.sensorberg.notifications.sdk.internal

import android.arch.persistence.room.Room
import android.location.Location
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.storage.ActionDao
import com.sensorberg.notifications.sdk.internal.storage.AppDatabase
import com.sensorberg.notifications.sdk.internal.storage.GeofenceMapper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeofenceDistanceTests {

	private var actionDao: ActionDao? = null
	private var db: AppDatabase? = null

	@Before
	fun createDb() {
		val context = InstrumentationRegistry.getTargetContext()
		db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
		actionDao = db!!.actionDao()
	}

	@After
	fun closeDb() {
		db!!.clearAllTables()
		db!!.close()
	}

	@Test
	fun find_closest() {

		for (i in 0..100) {
			execution()
		}

	}

	fun execution() {
		val dao = actionDao!!
		dao.insertGeofences(
				locations()
					.map { Trigger.Geofence(it.latitude, it.longitude, 100f, Trigger.Type.Enter) }
					.map { GeofenceMapper.mapInsert(it) })

		val javaOrdered = locations().sortedBy { officeLocation.distanceTo(it) }
		val fences = dao.findClosestGeofenceQueries(officeLocation)
		javaOrdered.forEachIndexed { index, location ->
			assertEquals("$index:(${location.latitude}, ${location.longitude})", location.latitude, fences[index].latitude, 0.0)
			assertEquals("$index:(${location.latitude}, ${location.longitude})", location.longitude, fences[index].longitude, 0.0)
		}
		dao.clearGeofences()
	}

	private val officeLocation = location(52.528349870000255, 13.415339961647987)
	private fun locations(): List<Location> {
		return listOf(
				location(52.52235065543921, 13.412663387483462) // alex platz
				, location(52.51998746286365, 13.415098833268985) // alexa mall
				, location(52.52076258569472, 13.40937249737317) // tv tower
				, location(52.51627962391556, 13.377699851989746) // brandenburg tor
				, location(52.51454943590012, 13.35011601448059) // victoria tower
				, location(52.48780110760053, 13.220607628536527) // grunewald
				, location(52.39341235175408, 13.06026479086347) // potsdam center
				, location(52.35855056765295, 13.013257293133847) // potsdam see
				, location(51.857942891339164, 14.227300526717954) // spreewald
				, location(51.33459192834602, 12.38093802669573) // Leipzig
				, location(49.4430972106525, 11.07854348221963) // Nuremberg
				, location(48.147730797289526, 11.56743508378213) // Munich
				, location(47.325421164117124, 13.345126181089313) // Winter wonderland
				, location(41.89021020000001, 12.492230899999981) // Coliseum de Rome
				, location(-2.58024300748177, -44.304386002701165) // Hell on earth (a.k.a Brasi)
				, location(69.35579, 88.18929389999994) // Norilsk, Krasnoyarsk Krai, Russia
				, location(47.90895333597973, 106.91155298847912) // Ulaanbaatar, Mongolia
				, location(39.053624696434674, 125.76345627403441) // best Korea
				, location(35.663486847608475, 139.66260264099674) // Tokyo
				, location(-74.5286931941612, 56.242607675979116) // somewhere in Antartica
					 ).shuffled()
	}

	companion object {
		private fun location(lat: Double, lng: Double): Location {
			return Location("Mock").apply {
				latitude = lat
				longitude = lng
			}
		}
	}
}
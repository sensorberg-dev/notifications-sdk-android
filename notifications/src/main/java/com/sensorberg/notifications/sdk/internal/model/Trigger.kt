package com.sensorberg.notifications.sdk.internal.model

import com.squareup.moshi.JsonClass
import java.util.*

internal sealed class Trigger {
	abstract fun getTriggerId(): String
	abstract val type: Type

	@JsonClass(generateAdapter = true)
	internal data class Beacon(val proximityUuid: UUID,
							   val major: Short,
							   val minor: Short,
							   override val type: Type) : Trigger() {

		override fun getTriggerId(): String {
			return getTriggerId(proximityUuid, major, minor, type)
		}

		companion object {
			fun getTriggerId(proximityUuid: UUID,
							 major: Short,
							 minor: Short,
							 type: Type): String {
				return "beacon($proximityUuid)($major)($minor)"
			}
		}
	}

	internal data class Geofence(val latitude: Double,
								 val longitude: Double,
								 val radius: Float,
								 override val type: Type) : Trigger() {

		override fun getTriggerId(): String {
			return getTriggerId(latitude, longitude, radius, type)
		}

		companion object {
			fun getTriggerId(latitude: Double,
							 longitude: Double,
							 radius: Float,
							 type: Type): String {
				return "geofence(${latitude.toRawBits()})(${longitude.toRawBits()})(${radius.toRawBits()})(${type.name})"
			}
		}
	}

	enum class Type {
		Enter, Exit, EnterOrExit
	}

}
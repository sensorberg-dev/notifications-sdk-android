package com.sensorberg.notifications.sdk.internal.backendsdkv2.model

import ch.hsr.geohash.GeoHash
import com.sensorberg.notifications.sdk.internal.model.ActionHistory
import com.squareup.moshi.JsonClass
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private typealias SdkConversion = com.sensorberg.notifications.sdk.internal.model.ActionConversion

@JsonClass(generateAdapter = true)
data class HistoryBody(
	val actions: List<BeaconAction>? = null,
	val conversions: List<ActionConversion>? = null,
	val deviceTimestamp: String? = null) {

	companion object {

		private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

		fun fromImplementation(actions: List<ActionHistory>?, conversions: List<SdkConversion>?): HistoryBody {
			return HistoryBody(
					deviceTimestamp = formatter.format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault())),
					actions = actions?.map { mapAction(it) },
					conversions = conversions?.map { mapConversions(it) })
		}

		private fun mapAction(a: ActionHistory): BeaconAction {
			return BeaconAction().apply {
				actionId = a.actionId
				timeOfPresentation = a.timestamp
				trigger = a.trigger.getType()
				pid = a.triggerBackendMeta
				//	geohash = extractGeoHash(a.latitude, a.longitude, a.radius)
				actionInstanceUuid = a.instanceId
			}
		}

		private fun mapConversions(a: SdkConversion): ActionConversion {
			return ActionConversion(a.instanceId,
									a.timestamp,
									ActionConversion.getConversionType(a.value)
					//	extractGeoHash(a.latitude, a.longitude, a.radius))
								   )
		}

		internal fun extractGeoHash(latitude: Double?, longitude: Double?, radius: Float?): String? {
			return if (latitude == null || longitude == null || radius == null) null
			else GeoHash.geoHashStringWithCharacterPrecision(
					latitude,
					longitude,
					precisionForRadius(radius))
		}

		/**
		 * Geohash precision, array index is corresponding to character precision in reverse.
		 */
		private val precision = floatArrayOf(2.4f, 19f, 76f, 610f, 2400f, 20000f, 78000f, 630000f, 2500000f)

		private fun precisionForRadius(radius: Float): Int {
			for (index in precision.indices) {
				if (radius <= precision[index]) {
					return precision.size - index
				}
			}
			return 1
		}
	}
}

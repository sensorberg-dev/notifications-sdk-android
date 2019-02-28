package com.sensorberg.notifications.sdk.internal.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.nearby.messages.IBeaconId
import java.util.*

@Entity(tableName = "table_beacon_events")
internal data class BeaconEvent(@PrimaryKey val beaconKey: String,
								val timestamp: Long,
								val proximityUuid: UUID,
								val major: Short,
								val minor: Short,
								val type: Trigger.Type) {
	companion object {
		fun generateEvent(b: IBeaconId, timestamp: Long, type: Trigger.Type): BeaconEvent {
			return BeaconEvent(BeaconEvent.generateKey(b),
							   timestamp,
							   b.proximityUuid,
							   b.major,
							   b.minor,
							   type)
		}

		fun generateKey(b: IBeaconId): String {
			// I know this method is basically the same to Trigger.Beacon.getTriggerId
			// the reason it's a separate method is that they cater different needs
			// and those two methods implementation might differ in the future.
			// the trigger ID from beacon is to reference the trigger,
			// this is for the processing from the database.
			return "(${b.proximityUuid})(${b.major})(${b.minor})"
		}
	}
}

@Entity(tableName = "table_visible_beacons")
internal data class VisibleBeacons(@PrimaryKey val id: String, val timestamp: Long)

/**
 * That's supposed to be a very short lived data storage.
 * As per docs androidx.work.Data have a hard limitation on MAX_DATA_BYTES (10K).
 * So for very long list of beacons we would hit this limit.
 * So we'll save to DB and erase as soon as the registration succeeds
 */
@Entity(tableName = "table_beacons_registration")
internal data class BeaconStorage(@PrimaryKey val id: String,
								  val proximityUuid: UUID,
								  val major: Short,
								  val minor: Short,
								  val type: Trigger.Type) {
	companion object {
		fun from(beacon: Trigger.Beacon): BeaconStorage {
			return BeaconStorage(beacon.getTriggerId(),
								 beacon.proximityUuid,
								 beacon.major,
								 beacon.minor,
								 beacon.type)
		}
	}
}
package com.sensorberg.notifications.sdk.internal.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
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
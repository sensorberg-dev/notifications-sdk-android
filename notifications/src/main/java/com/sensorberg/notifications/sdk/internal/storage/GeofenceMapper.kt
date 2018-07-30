package com.sensorberg.notifications.sdk.internal.storage

import com.google.android.gms.location.Geofence
import com.sensorberg.notifications.sdk.internal.model.GeofenceQuery
import com.sensorberg.notifications.sdk.internal.model.Trigger

internal object GeofenceMapper {
	fun mapQuery(query: GeofenceQuery): Geofence {
		val transition = when (query.type) {
			Trigger.Type.Enter -> Geofence.GEOFENCE_TRANSITION_ENTER
			Trigger.Type.Exit -> Geofence.GEOFENCE_TRANSITION_EXIT
			Trigger.Type.EnterOrExit -> Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
		}

		return Geofence.Builder()
			.setRequestId(query.id)
			.setCircularRegion(query.latitude, query.longitude, query.radius)
			.setTransitionTypes(transition)
			.setExpirationDuration(Geofence.NEVER_EXPIRE)
			.build()
	}

	fun mapInsert(fence: Trigger.Geofence): GeofenceQuery {
		return GeofenceQuery(fence.getTriggerId(),
							 fence.latitude,
							 fence.longitude,
							 fence.radius,
							 fence.type,
							 Math.sin(Math.toRadians(fence.latitude)),
							 Math.sin(Math.toRadians(fence.longitude)),
							 Math.cos(Math.toRadians(fence.latitude)),
							 Math.cos(Math.toRadians(fence.longitude)))
	}
}
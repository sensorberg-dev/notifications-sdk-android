package com.sensorberg.notifications.sdk.internal.storage

import com.google.android.gms.location.Geofence

internal data class GeofenceQueryResult(val fencesToAdd: List<Geofence>, val maxDistance: Float, val fencesToRemove: List<String>)
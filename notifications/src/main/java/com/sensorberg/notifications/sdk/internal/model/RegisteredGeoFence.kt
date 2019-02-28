package com.sensorberg.notifications.sdk.internal.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_registered_geofences")
internal data class RegisteredGeoFence(@PrimaryKey val id: String)
package com.sensorberg.notifications.sdk.internal.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "table_registered_geofences")
data class RegisteredGeoFence(@PrimaryKey val id: String)
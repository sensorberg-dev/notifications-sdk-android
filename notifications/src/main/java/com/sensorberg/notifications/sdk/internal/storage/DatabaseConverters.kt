package com.sensorberg.notifications.sdk.internal.storage

import android.arch.persistence.room.TypeConverter
import com.sensorberg.notifications.sdk.Conversion
import com.sensorberg.notifications.sdk.internal.model.Trigger

class DatabaseConverters {
	@TypeConverter
	fun toTriggerType(code: Int): Trigger.Type {
		return when (code) {
			1 -> Trigger.Type.Enter
			2 -> Trigger.Type.Exit
			3 -> Trigger.Type.EnterOrExit
			else -> throw IllegalArgumentException("Trigger.Type code can't be $code")
		}
	}

	@TypeConverter
	fun fromTriggerType(type: Trigger.Type): Int {
		return when (type) {
			Trigger.Type.Enter -> 1
			Trigger.Type.Exit -> 2
			Trigger.Type.EnterOrExit -> 3
		}
	}

	@TypeConverter
	fun toConversionType(code: Int): Conversion {
		return when (code) {
			1 -> Conversion.NotificationDisabled
			2 -> Conversion.Suppressed
			3 -> Conversion.Ignored
			4 -> Conversion.Success
			else -> throw IllegalArgumentException("Conversion code can't be $code")
		}
	}

	@TypeConverter
	fun fromConversionType(conversion: Conversion): Int {
		return when (conversion) {
			Conversion.NotificationDisabled -> 1
			Conversion.Suppressed -> 2
			Conversion.Ignored -> 3
			Conversion.Success -> 4
		}
	}

}
package com.sensorberg.notifications.sdk.internal.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.sensorberg.notifications.sdk.Action

/**
 * This class is a copy of Action.
 * It's used for storage of delayed actions only.
 * After migrating to Room 2.1.0 check again if we can merge this with Action class
 */
@Entity(tableName = "table_delayed_actions")
internal data class DelayedActionModel(
	val actionId: String,
	@PrimaryKey val instanceId: String,
		// data
	val subject: String?,
	val body: String?,
	val url: String?,
	val payload: String?,
		// raw data
	val backendMeta: String?,
	val triggerBackendMeta: String?) {

	companion object {
		fun toAction(action: DelayedActionModel): Action {
			return Action(action.actionId,
						  action.instanceId,
						  action.subject,
						  action.body,
						  action.url,
						  action.payload,
						  action.backendMeta,
						  action.triggerBackendMeta)
		}

		fun fromAction(action: Action): DelayedActionModel {
			return DelayedActionModel(action.actionId,
									  action.instanceId,
									  action.subject,
									  action.body,
									  action.url,
									  action.payload,
									  action.backendMeta,
									  action.triggerBackendMeta)
		}
	}

}
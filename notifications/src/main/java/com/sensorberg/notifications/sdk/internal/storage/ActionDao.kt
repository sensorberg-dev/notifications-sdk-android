package com.sensorberg.notifications.sdk.internal.storage

import android.arch.persistence.room.*
import com.sensorberg.notifications.sdk.internal.model.*

@Dao
internal abstract class ActionDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertActions(actions: Collection<ActionModel>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertMappings(mappings: Collection<TriggerActionMap>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertTimePeriods(timePeriods: Collection<TimePeriod>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertStatistics(vararg statistics: Statistics)

	@Query("DELETE FROM table_action") abstract fun clearActions()
	@Query("DELETE FROM table_trigger_action_map") abstract fun clearMappings()
	@Query("DELETE FROM table_time_period") abstract fun clearTimePeriods()

	@Query("SELECT table_action.*, table_trigger_action_map.triggerBackendMeta FROM table_action " +
		   "INNER JOIN table_trigger_action_map ON table_trigger_action_map.actionId = table_action.id " +
		   "WHERE table_action.id IN (SELECT actionId FROM table_trigger_action_map WHERE (triggerId = :triggerId AND type IN (:types))) AND (deliverAt = 0 OR deliverAt > :now)")
	abstract fun getActionsForTrigger(triggerId: String, now: Long, vararg types: Trigger.Type): List<ActionQueryModel>

	@Query("SELECT * FROM table_statistics WHERE actionId = :actionId")
	abstract fun getStatisticsForAction(actionId: String): Statistics?

	@Query("SELECT COUNT(*) FROM table_time_period WHERE actionId = :actionId AND startsAt < :now  AND endsAt > :now")
	abstract fun getTimePeriodsForAction(actionId: String, now: Long): Long

	@Query("SELECT * FROM table_action_history")
	abstract fun getActionHistory(): List<ActionHistory>

	@Insert
	abstract fun insertActionHistory(vararg action: ActionHistory)

	@Delete
	abstract fun clearActionHistory(actions: List<ActionHistory>)

	@Query("SELECT * FROM table_action_conversion")
	abstract fun getActionConversion(): List<ActionConversion>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertActionConversion(vararg action: ActionConversion)

	@Delete
	abstract fun clearActionConversion(actions: List<ActionConversion>)
}


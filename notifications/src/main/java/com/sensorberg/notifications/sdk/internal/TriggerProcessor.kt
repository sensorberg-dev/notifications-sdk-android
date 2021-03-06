package com.sensorberg.notifications.sdk.internal

import android.app.Application
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.TriggerMapper
import com.sensorberg.notifications.sdk.internal.backend.backendsdkv2.model.getType
import com.sensorberg.notifications.sdk.internal.model.*
import com.sensorberg.notifications.sdk.internal.storage.ActionDao
import com.sensorberg.notifications.sdk.internal.work.UploadWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.*

internal class TriggerProcessor(private val dao: ActionDao,
								private val workUtils: WorkUtils,
								private val actionLauncher: ActionLauncher,
								private val app: Application) {

	fun process(triggerId: String, type: Trigger.Type) {
		val actions = findActionsToFire(dao, triggerId, type)
		updateStatistics(actions)

		val actionsToFire = actions.filter { !it.silent }
		val silentActions = actions.filter { it.silent }

		executeActions(actionsToFire, type)
		silentActions.forEach { silentAction ->
			val history: ActionHistory = silentAction.toActionHistory(type, app.getLastLocation())
			dao.insertActionHistory(history)
		}

		var reportImmediate = false
		actions.forEach {
			reportImmediate = reportImmediate || it.reportImmediately
		}

		if (reportImmediate) {
			workUtils.executeAndSchedule(UploadWork::class.java)
		}
	}

	private fun executeActions(actions: List<ActionQueryModel>, type: Trigger.Type) {
		actions.forEach { model ->

			/*
			TODO: <the hack starts here>
			See: https://git.sensorberg.io/android/notifications-sdk/issues/9
			 */
			val payload = model.payload?.let {
				try {
					val jsonPayload = JSONObject(it)
					jsonPayload.put(TriggerMapper.META_ACTION_TRIGGER, type.getType())
					jsonPayload.toString()
				} catch (e: JSONException) {
					null
				}
			} ?: model.payload
			/*
			TODO: </the hack ends here>
			 */

			val action = Action(model.id, UUID.randomUUID().toString(), model.subject, model.body, model.url, payload, model.backendMeta, model.triggerBackendMeta)
			when {
				model.deliverAt > 0 -> calculateAndSendDelayedAction(model, action, type)
				model.delay > 0 -> sendDelayedAction(action, type, model)
				else -> actionLauncher.launchAction(action, type)
			}
		}
	}

	private fun calculateAndSendDelayedAction(model: ActionQueryModel, action: Action, type: Trigger.Type) {
		val delay = calculateDelay(model.deliverAt)
		if (delay > 0) {
			workUtils.sendDelayedAction(action, type, model.reportImmediately, delay)
		}
	}

	private fun sendDelayedAction(action: Action, type: Trigger.Type, model: ActionQueryModel) {
		workUtils.sendDelayedAction(action, type, model.reportImmediately, model.delay)
	}

	private fun updateStatistics(actions: List<ActionQueryModel>) {
		val now = System.currentTimeMillis()
		actions.forEach {
			var statistics = dao.getStatisticsForAction(it.id)
			if (statistics == null) {
				statistics = Statistics(it.id, 1, now)
			} else {
				statistics.count++
				statistics.lastExecuted = now
			}
			Timber.d("Updating statistics for fired action: $statistics")
			dao.insertStatistics(statistics)
		}
	}

	companion object {

		internal fun calculateDelay(deliverAt: Long): Long {
			return deliverAt - System.currentTimeMillis()
		}

		internal fun findActionsToFire(dao: ActionDao, triggerId: String, type: Trigger.Type): List<ActionQueryModel> {
			val now = System.currentTimeMillis()
			val actions = dao.getActionsForTrigger(triggerId, now, type, Trigger.Type.EnterOrExit)
			Timber.d("Found ${actions.size} actions in total for trigger $triggerId")

			val actionsToFire = mutableListOf<ActionQueryModel>()

			actions.forEach next@{ action ->
				dao.getStatisticsForAction(action.id)?.let {
					if (action.maxCount != 0 && it.count >= action.maxCount) {
						// actions already fired max number of times
						Timber.d("Filter(MAX_COUNT): ${action.id} won't execute, it already executed ${it.count} times; max count is ${action.maxCount}")
						return@next
					}

					if (action.suppressionTime != 0L && (now - it.lastExecuted) < action.suppressionTime) {
						// action happened short ago
						Timber.d("Filter(SUPPRESSION_TIME): ${action.id} won't execute, it executed ${(now - it.lastExecuted) / 1000} seconds ago; suppression is ${action.suppressionTime / 1000} seconds")
						return@next
					}
				}

				// check if action is allowed now
				if (dao.getTimePeriodsForAction(action.id, now) > 0) {
					actionsToFire.add(action)
				} else {
					Timber.d("Filter(TIME_PERIOD): ${action.id} won't execute, it's not allowed at this time")
				}
			}

			Timber.d("Found ${actionsToFire.size} actions to execute for trigger $triggerId")
			return actionsToFire
		}

	}
}

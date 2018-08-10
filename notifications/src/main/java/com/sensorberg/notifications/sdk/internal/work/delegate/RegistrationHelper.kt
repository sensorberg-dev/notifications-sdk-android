package com.sensorberg.notifications.sdk.internal.work.delegate

import androidx.work.Worker
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal object RegistrationHelper {
	internal fun awaitResult(tag: String, seconds: Long, task: Task<*>): Worker.Result {
		return try {
			// await synchronously to completion
			Tasks.await(task, seconds, TimeUnit.SECONDS)
			if (task.isSuccessful) {
				Timber.d("$tag registration SUCCESS")
				Worker.Result.SUCCESS
			} else {
				Timber.e("$tag registration fail. RETRY. ${task.exception}")
				Worker.Result.RETRY
			}
		} catch (e: ExecutionException) {
			Timber.e("$tag registration fail. RETRY. ${e.message}")
			Worker.Result.RETRY
		} catch (e: InterruptedException) {
			Timber.e(e, "$tag registration interrupted. RETRY. ${e.message}")
			Worker.Result.RETRY
		} catch (e: TimeoutException) {
			Timber.e("$tag registration timeout. RETRY. ${e.message}")
			Worker.Result.RETRY
		} catch (e: Exception) {
			Timber.e(e, "$tag registration unknown error. RETRY. ${e.message}")
			Worker.Result.RETRY
		}
	}
}
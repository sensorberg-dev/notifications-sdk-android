package com.sensorberg.notifications.sdk.internal.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.IBeaconId
import com.google.android.gms.nearby.messages.Message
import com.google.android.gms.nearby.messages.MessageListener
import com.sensorberg.notifications.sdk.internal.InjectionModule
import com.sensorberg.notifications.sdk.internal.SdkEnableHandler
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.storage.BeaconDao
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor

class BeaconReceiver : BroadcastReceiver(), KoinComponent {

	private val executor: Executor by inject(InjectionModule.executorBean)
	private val dao: BeaconDao by inject()
	private val workUtils: WorkUtils by inject()
	private val sdkEnableHandler: SdkEnableHandler by inject()

	override fun onReceive(context: Context, intent: Intent) {
		if (!sdkEnableHandler.isEnabled()) return
		Nearby.getMessagesClient(context).handleIntent(intent, object : MessageListener() {
			override fun onFound(message: Message) {
				getBeacon(message)?.let {
					Timber.d("Found beacon: $it")
					enqueueEvent(it, System.currentTimeMillis(), Trigger.Type.Enter)
				}
			}

			override fun onLost(message: Message) {
				getBeacon(message)?.let {
					Timber.d("Lost beacon: $it")
					enqueueEvent(it, System.currentTimeMillis(), Trigger.Type.Exit)
				}
			}
		})
	}

	private fun enqueueEvent(beacon: IBeaconId, timestamp: Long, type: Trigger.Type) {
		val pending = goAsync() // process this trigger asynchronously
		executor.execute {
			dao.addBeaconEvent(BeaconEvent.generateEvent(beacon, timestamp, type))
			workUtils.executeBeaconWorkFor(BeaconEvent.generateKey(beacon), type)
			pending.finish()
		}
	}

	companion object {

		private const val BEACON_REQUEST_CODE = 1338

		private fun getBeacon(message: Message): IBeaconId? {
			return if (Message.MESSAGE_NAMESPACE_RESERVED == message.namespace
					   && Message.MESSAGE_TYPE_I_BEACON_ID == message.type) {
				IBeaconId.from(message)
			} else {
				null
			}
		}

		fun generatePendingIntent(context: Context): PendingIntent {
			val intent = Intent(context, BeaconReceiver::class.java)
			return PendingIntent.getBroadcast(context, BEACON_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
		}
	}
}
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
import com.sensorberg.notifications.sdk.internal.async
import com.sensorberg.notifications.sdk.internal.model.BeaconEvent
import com.sensorberg.notifications.sdk.internal.model.Trigger
import com.sensorberg.notifications.sdk.internal.storage.BeaconDao
import com.sensorberg.notifications.sdk.internal.work.BeaconProcessingWork
import com.sensorberg.notifications.sdk.internal.work.WorkUtils
import com.sensorberg.notifications.sdk.internal.work.delegate.BeaconProcessingDelegate
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
		async(executor) {
			dao.addBeaconEvent(BeaconEvent.generateEvent(beacon, timestamp, type))
			val beaconKey = BeaconEvent.generateKey(beacon)
			if (type == Trigger.Type.Enter) {
				// cancel if there's an awaiting exit event to be processed
				workUtils.cancelBeaconWork(beaconKey)
				// we can safely ignore here the return value,
				// that's because RETRY only happens if location or bluetooth is off
				// but we won't have an enter event if they're off
				BeaconProcessingDelegate().execute(beaconKey)
			} else {
				workUtils.executeBeaconWork(beaconKey)
			}
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

		fun generateSubscribePendingIntent(context: Context): PendingIntent {
			val intent = Intent(context, BeaconReceiver::class.java)
			return PendingIntent.getBroadcast(context, BEACON_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
		}

		fun generateUnsubscribePendingIntent(context: Context): PendingIntent {
			val intent = Intent(context, BeaconReceiver::class.java)
			return PendingIntent.getBroadcast(context, BEACON_REQUEST_CODE, intent, 0)
		}
	}
}
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
import com.sensorberg.notifications.sdk.internal.NotificationsSdkImpl
import com.sensorberg.notifications.sdk.internal.TriggerProcessor
import com.sensorberg.notifications.sdk.internal.common.model.Trigger
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.Executor

class BeaconReceiver : BroadcastReceiver(), KoinComponent {

	private val executor: Executor by inject(InjectionModule.executorBean)
	private val triggerProcessor: TriggerProcessor by inject()

	override fun onReceive(context: Context, intent: Intent) {
		Nearby.getMessagesClient(context).handleIntent(intent, object : MessageListener() {
			override fun onFound(message: Message) {
				getBeacon(message)?.let {

					if (NotificationsSdkImpl.BeaconRegistrationHack.isRecent()) {
						Timber.w("onFound ignored $it")
						return
					}

					Timber.i("Found beacon: $it")
					processTrigger(getTriggerId(it, Trigger.Type.Enter), Trigger.Type.Enter)
				}
			}

			override fun onLost(message: Message) {
				getBeacon(message)?.let {

					if (NotificationsSdkImpl.BeaconRegistrationHack.isRecent()) {
						Timber.w("onLost ignored $it")
						return
					}

					Timber.i("Lost beacon: $it")
					processTrigger(getTriggerId(it, Trigger.Type.Exit), Trigger.Type.Exit)
				}
			}
		})
	}

	private fun processTrigger(triggerId: String, type: Trigger.Type) {
		val pending = goAsync() // process this trigger asynchronously
		executor.execute {
			triggerProcessor.process(triggerId, type)
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

		private fun getTriggerId(b: IBeaconId, type: Trigger.Type): String {
			return Trigger.Beacon.getTriggerId(b.proximityUuid, b.major, b.minor, type)
		}

		fun generatePendingIntent(context: Context): PendingIntent {
			val intent = Intent(context, BeaconReceiver::class.java)
			return PendingIntent.getBroadcast(context, BEACON_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
		}
	}
}
package com.sensorberg.notifications.sdk.sample

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.sensorberg.notifications.sdk.AbstractActionReceiver
import com.sensorberg.notifications.sdk.Action
import com.sensorberg.notifications.sdk.Conversion
import timber.log.Timber

class ActionReceiver : AbstractActionReceiver() {

	private val NOTIFICATION_CHANNEL_ID = "NotificationChannelIdFromSdkSample"

	override fun onAction(context: Context, action: Action) {
		Timber.i("Action received by the application. $action")
		val intent = Intent(context, MainActivity::class.java)
		intent.putExtra("action", action)
		val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		val notification = createNotification(context, pending, action)

		notificationManager.notify(action.id.hashCode(), notification)
		val notificationSdk = (context.applicationContext as App).sdk
		notificationSdk.setConversion(action, Conversion.Ignored) //Ignored in Conversion/Manager speech is the same as shown for us lolz
	}

	private fun createNotification(context: Context, pending: PendingIntent, action: Action): Notification {
		initChannels(context)
		return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setContentIntent(pending)
			.setContentTitle(action.subject)
			.setContentText(action.body)
			.setSmallIcon(R.drawable.ic_notification)
			.setChannelId(NOTIFICATION_CHANNEL_ID)
			.setAutoCancel(true)
			.setShowWhen(true)
			.build()
	}

	@TargetApi(Build.VERSION_CODES.O)
	private fun initChannels(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification SDK", NotificationManager.IMPORTANCE_HIGH)
			channel.description = "Foooooo......"
			val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			manager.createNotificationChannel(channel)
		}
	}

}

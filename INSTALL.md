# Sensorberg Notification SDK

## Implementation Guide

The implementation of this SDK is extremely similar to the "old" Sensorberg Beacon SDK.

The app requires fine location permission, please refer to the official docs about [Request App Permissions](https://developer.android.com/training/permissions/requesting)

### Initialise the SDK.

Initialise the SDK as a singleton during `Application.onCreate()`

```
notificationsSdk = NotificationsSdk
                        .with(this)             // the Application object
                        .setApiKey(KEY)         // required, the API key as registered on `portal.sensorberg.com` (or individual backend instance)
                        .setBaseUrl(BASE_URL)   // optional, use in case executing on a different backend instance
                        .enableHttpLogs()       // optional, only use for debugging
                        .build()                // build the instance of the SDK, keep it as a Singleton
```

It's very important to be initialised as a singleton, multiple instances will not work.
It's very important to be initialise it during `Application.onCreate()`, because the SDK uses Services and BroadcastReceivers that can be executed without an Activity.

### Receiving Actions

Actions are delivered via `BroadcastReceivers`.

#### Create the BroadcastReceiver class

```
// Kotlin
class ActionReceiver : AbstractActionReceiver() {
    override fun onAction(context: Context, action: Action) {
        // action.subject, action.body, action.url, action.payload
    }
}

// Java
public class ActionReceiver extends AbstractActionReceiver {
	@Override public void onAction(@NotNull Context context, @NotNull Action action) {
		// action.getSubject(), action.getBody(), action.getUrl(), action.getPayload()
	}
}
```

The Action class is parcelable, which allows it to be passed to Intent/Bundle/etc

Alternatively, one can use a direct instance of `BroadcastReceivers` and call the helper method to extract the action:
```
// Kotlin
val action = NotificationsSdk.extractAction(intent)

// Java
Intent i = new Intent();
Action action = NotificationsSdk.Companion.extractAction(i);
```

#### Register the BroadcastReceiver on the manifest

Add the action receiver class to the manifest under the `application` tag as follows.

```
<receiver
    android:name=".ActionReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.sensorberg.notifications.sdk.ACTION_RECEIVER"/>
    </intent-filter>
</receiver>
```

Do not register more than once, only one will receive the action.

### Conversion, advertisement-id, and attributes

- Conversion

User conversion can be registered by calling the appropriate method on the SDK instance

```
notificationsSdk.setConversion(action, Conversion.Success)
```

- advertisement-id and attributes

Backend filtering can be added to the SDK via advertisement-id and attributes by calling the appropriate methods on the SDK instance

```
// set advertisement ID
notificationsSdk.setAdvertisementId(adId) // ad-id could be, for example, from Google Play Service advertise ID

// set attributes
val attributes = mutableMapOf<String, String>()
attributes.put("age", "20_30")
attributes.put("locale", "de")
sdk.setAttributes(attributes
notificationsSdk.setAttributes(attributes)
```
Valid attributes are only alphanumerical characters and underscore.
Advertisement ID and attributes are stored internally and don't need to be set every time.
Call the methods with `null` to clear them out.
When those parameters change, the SDK will soon synchronize the data with the backend.

## Important differences from "old" Sensorberg Beacon SDK

- The broadcast receiver has a new action name. If upgrading, make sure to update it.
- The old SDK required a different process to execute, this is not the case anymore. If you're upgrading to this SDK remove the process check from `Application.onCreate()` and the broadcast receiver
- It's not necessary to register a background detector anymore.
- This SDK optimizes for battery efficiency and does not scan all the time. Please refer to the testing guide below.

## Internal working

This SDK operates by registering beacons, geofences and push notifications (soon) to Google Play Services to deliver one unified and battery efficient experience.

For recurrent operations (synchronize and upload data) and for delayed action delivery this SDK uses the WorkManager.
In case your application also uses WorkManager, remember than using `workManager.cancelAllWork()` will also cancel the operations scheduled for this SDK.

## Debugging

The SDK is logging information using [Timber logger](https://github.com/JakeWharton/timber).
To see those logs simply plant a tree on Timber, e.g.: `Timber.plant(new Timber.DebugTree())`.
The optional builder parameter `.enableHttpLogs()` will enable all the HTTP communication logging on the VERBOSE level.

## Testing Guide

After SDK is correctly integrated into the Android App and a campaign setup on the backend, the easiest way to test it is:

- Open the app and make sure to accept the location permission for the application
- Await a few seconds to make sure the data have been synchronized with the backend
- After that the app can stay open or be closed, it doesn't matter

### Beacons

- Turn off the device screen
- Insert battery in the beacon
- Turn on the device screen
- A broadcast should be sent in a few seconds if an "on_enter" action have been configured
- Turn off the device screen
- Remove the battery from the beacon
- Turn on the device screen
- A broadcast should be sent in a few seconds if an "on_exit" action have been configured

### Geofences

Use a location mock app to "move the device" in and out of the geofenced area
We've used this [Mock GPS](https://play.google.com/store/apps/details?id=net.marlove.mockgps) and it work well.
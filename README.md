# Notification SDK

[ ![Download](null/packages/sensorberg/maven/notification-sdk/images/download.svg?version=2.7.4) ](https://bintray.com/sensorberg/maven/notification-sdk/2.7.4/link)

## Terms

- Trigger: defines a physical measurement on the real world.
- Trigger.Type: enter / exit
- ActionModel: defines an action that can be triggered
- Action: defines an instance occurrence of an action model
- ActionHistory: database entry for occurred actions
- ActionConversion: user feedback to the action instance

## Model

Triggers and actions exist independently.
There's a model/data-table that maps triggers to actions to trigger.type
Action and the mapping contains an `backendMeta` value to allow backend implementation to store extra information

## Components

### WorkUtils

Utility class to access the WorkManager.

- schedule() schedule a work for 24 hours sync
- execute() immediately executes a work and upon its completion, schedule for 24 hours sync
- utility extension functions to extra input data to be used by the work

#### Available works

- SyncWork: synchronizes the backend data with local storage and register triggers (BroadcastReceiver)
- UploadWork: publishes user action history and conversion to the backend
- FireActionWork: fire action instance to the user, used for delayed actions

### NotificationsSdkImpl

Entry class of the SDK. Should be a singleton initialized during `Application.onCreate()`

- On init, sets backend related data
- Awaits for application to be in foreground and to have location permission
- execute SyncWork and schedule UploadWork
- upon data change (adId or attributes), re-execute SyncWork

### Triggers

- parse their own data (beacon, geofence, push)
- push this data to TriggerProcessor

### TriggerProcessor

- receives a trigger from one of the receivers
- compares trigger-id to the database mapping
- process suppression, max count, time periods, report immediate
- if(delayed) schedule execution;
- else instantiate an Action and sends to host app
# Background notifications, incoming calls, and profiles

## Firebase setup required for background delivery

1. In one Firebase project, register Android package `com.twocall.chat`.
2. Place that project's Android configuration at `android-app/app/google-services.json` before building. The Google Services Gradle plugin applies when this file exists. Without it the app can still build for development, but background push will not work.
3. On the backend set `FIREBASE_SERVICE_ACCOUNT_JSON` to the service account JSON from that same project, using the host's secret settings. Alternatively mount the JSON file and set `FIREBASE_CREDENTIALS_PATH` to its absolute path. Never commit the private key.
4. Deploy the backend and install the newly built APK on both phones. The backend must apply Flyway V2 before the new client uses profiles and call recovery.
5. Grant notification permission. In app Settings → Calls & notifications, open Notification settings and enable both Messages and Incoming Calls. Android 14+ also has an Allow full-screen incoming calls setting. A disabled full-screen permission should still permit the call notification's Answer and Decline buttons.

The backend sends high-priority data messages. The Firebase service immediately posts an alert, and schedules message synchronization with WorkManager. An incoming call notification expires after 60 seconds. Opening or answering it reconnects signaling and retrieves the persisted offer and caller ICE candidates before accepting. Decline uses an authenticated REST endpoint, so it does not depend on an existing socket. Call-end pushes cancel the matching call alert.

Force-stopping the app in Android settings prevents delivery until it is opened again. Manufacturer battery restrictions and unavailable Google Play services can also affect push delivery. A periodic keep-alive request to the server does not replace Firebase notifications.

## Profiles

Settings → Your profile supports a name up to 64 characters, a selected photo, and removing the photo. Photos are reduced to JPEG thumbnails before upload. The authenticated profile API scopes reads and updates to the token's pair and device. The name and thumbnail are stored in PostgreSQL; they are profile metadata visible to the backend.

Saving updates each known paired device identity. Failed sharing is reported in the UI and retried on synchronization. Partners receive a live profile-change event and refresh from the server; disconnected partners refresh on reconnect/foreground sync. Home, conversation headers, and both call screens display the partner's name and photo. The Room 2→3 migration preserves existing conversations and adds the photo column.

Upgrades from the original Room version 1 also migrate conversations and create call-history tables without deleting messages. Original single-pair credentials migrate into per-pair storage while retaining identity keys.

## Two-phone verification

- With both phones paired, put the receiver app in the background and lock its screen. Send a message; verify the notification appears without opening the app and opens the correct conversation.
- Repeat with the receiver process removed from recents (not force-stopped). Call the receiver; verify the incoming notification, Answer, and Decline. Confirm audio/video connects after Answer.
- End the call from the caller; verify the receiver alert disappears. Wait beyond 60 seconds before opening an old alert; it must not start a stale call.
- Deny notification permission and use the Settings shortcut to re-enable it; verify alerts resume.
- Start a video call with a short system screen timeout. Leave it idle beyond the timeout; the visible call screen remains awake. End or leave the call; normal screen timeout resumes.
- Rename and change a photo on each phone; verify it on the other phone's home, chat, and call screens. Repeat while the other phone is offline and verify on reconnect.
- Add a second conversation and verify its push registration and profile synchronization independently.

References: https://firebase.google.com/docs/cloud-messaging/android/receive-messages and https://developer.android.com/develop/background-work/background-tasks/awake/screen-on

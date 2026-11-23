# ChatApp (EchoMeet)

An Android chat app with an XML Views interface branded EchoMeet. It uses Firebase Authentication for email/password accounts and Firebase Realtime Database for user profiles and one-to-one text conversations.

## Implemented features

- Sign up, log in, log out, change display name, and delete an account after password reauthentication.
- Browse and search registered users, add contacts, search saved contacts, and remove contacts by long press.
- Send and receive text messages in real time, with latest-message previews and timestamps in the contact list.
- Display online/last-seen information.
- Pick a profile photo stored on the device. Contacts are also saved locally, separately for each signed-in user.

## Stack and structure

Kotlin 2.1.0, AndroidX/AppCompat, Material Components, RecyclerView, XML layouts, Gson 2.10.1, Firebase BoM 33.10.0, Firebase Auth 23.2.0 and Realtime Database 21.0.0. Builds use Android Gradle Plugin 8.6.1, Gradle wrapper 8.7, and Google Services plugin 4.4.2. Java/Kotlin compilation targets JVM 1.8.

```text
app/src/main/java/com/example/chatapp/  Activities, adapters, models, local storage helpers
app/src/main/res/                      Layouts, themes, drawables, fonts and icons
app/src/main/AndroidManifest.xml       Components and Internet permission
app/src/test/                         Example JUnit test
app/src/androidTest/                  Example Android instrumentation test
app/google-services.json.example      Invalid Firebase configuration template
app/build.gradle.kts                  Android settings and app dependencies
gradle/libs.versions.toml             Dependency and plugin versions
gradle/wrapper/                       Gradle wrapper configuration and JAR
```

## Prerequisites and setup

1. Install Android Studio supporting AGP 8.6.1, a JDK compatible with Gradle 8.7 (JDK 17 recommended), Android SDK Platform 35, and the SDK Build Tools requested by Gradle. The app compiles/targets API 35 and requires Android 13 (API 33) or newer to run.
2. Open this project folder in Android Studio. Set the Gradle JDK and SDK location; Android Studio normally creates the ignored `local.properties` containing `sdk.dir`. Dependency resolution requires access to Google Maven, Maven Central and the Gradle distribution service on first setup.
3. Complete the Firebase configuration below before building. The Google Services plugin requires a valid local `app/google-services.json`.
4. Sync Gradle, select the `app` run configuration and an API 33+ emulator/device, then Run. For an emulator, use an image with Google Play services.

### Firebase configuration

Use your own development Firebase project or obtain access to the intended project from its owner. In the Firebase console:

1. Register/select an Android app with package name `com.example.chatapp`, matching `applicationId` in `app/build.gradle.kts`.
2. Enable **Authentication > Sign-in method > Email/Password**.
3. Create the project's default Realtime Database and configure reviewed Security Rules before using real data. The code calls `FirebaseDatabase.getInstance()` without an explicit URL, so ensure the downloaded configuration includes the correct default database URL (`project_info.firebase_url`). Download the configuration again after creating the database if needed.
4. In **Project settings > General > Your apps**, download `google-services.json` for that Android app and save it as `app/google-services.json`.

The example file shows the expected shape using deliberately invalid placeholders; it cannot run the app. Download a real configuration rather than copying the example over a working file. The local file is ignored by Git; the developer's existing file is preserved. Do not commit project-specific configurations or service-account credentials. No CI workflow is present. If CI is added, restore the file from its protected configuration store before invoking Gradle without echoing or uploading its contents.

### Build and tests

From this folder in Windows PowerShell:

```powershell
.\gradlew.bat :app:processDebugGoogleServices :app:assembleDebug
.\gradlew.bat :app:installDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

On macOS/Linux, use `sh ./gradlew` in place of `.\gradlew.bat`. Installation and instrumentation tests require a connected API 33+ device/emulator. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. The existing tests only check integer addition and the app package name; they do not validate Firebase access, chat behavior, or authorization.

## Security and the Google API key alert

Repository evidence identifies the flagged `app/google-services.json` entry as a Firebase Android client API key consumed by the Google Services plugin. No other credential-pattern matches or duplicate occurrences of that key were found in first-party files, including ignored local/IDE configuration; dependencies, generated outputs and caches were inventoried by metadata rather than treated as source. A bucket field in the configuration does not mean this app uses Cloud Storage; the code uses Authentication and Realtime Database.

Firebase client keys identify the project and are extractable from an APK. Keeping this configuration local prevents unintended recommits; environment variables would not make the packaged key secret. Repository evidence cannot establish the key's current validity, API/application restrictions, usage or billing. Review these in the provider console using the [Firebase API key guidance](https://firebase.google.com/docs/projects/api-keys).

### Owner actions to resolve the alert

1. In Google Cloud **APIs & Services > Credentials**, locate the alerted key privately and inventory all consumers, including shipped APKs and other apps. Review enabled APIs, API usage/metrics, available audit logs, quotas and billing for unexpected activity. Console checks have not been performed here.
2. Check API restrictions against the Firebase services actually needed, including Authentication's Identity Toolkit and Token Service APIs; verify the complete required API list against Firebase's guidance. Exclude unrelated billable APIs and use separate restricted keys for other Google services. For Android application restrictions, register `com.example.chatapp` with each authorized signing certificate's SHA-1 (debug and release/Play App Signing as applicable), and verify SDK compatibility on each build before rollout.
3. Since safety cannot be established locally, create a restricted replacement key in the same project. Ensure Firebase selects it for the Android app, download fresh configuration and privately verify that it uses the replacement. Update every affected local configuration, protected CI value and app consumer; rebuild and distribute updates. Test signup/login, token refresh, profile edits and messaging between two accounts, on relevant signing variants.
4. After affected consumers are migrated and verified, revoke/delete the old key, verify it no longer works and monitor for failures or unexpected use. Coordinate the cutoff for older installed versions that still embed it. No rotation, revocation or cloud configuration change was performed by this repository fix.
5. Commit the configuration removal, ignore rule, example and documentation. Removing a tracked file from the index and adding an ignore rule do not erase Git history or close the alert. After confirmed revocation, resolve the GitHub alert as **Revoked** with a remediation note. If the owner instead establishes that this is intentionally public, safely restricted Firebase client configuration and retains it, document that evidence and use **Used in tests / not a secret** only where justified. Do not mark the real key as a false positive merely because it was removed. See [GitHub's alert resolution guidance](https://docs.github.com/en/code-security/how-tos/manage-security-alerts/manage-secret-scanning-alerts/resolving-alerts). History rewriting is not part of this change.

### Database authorization and App Check

No Firebase Security Rules, emulator configuration or App Check SDK integration are checked in, so deployed protection cannot be verified. Review [Realtime Database Security Rules](https://firebase.google.com/docs/database/security) to require authentication, restrict user-profile writes to the owner and chat access to participants, and validate message sender IDs, field types and sizes. The app reads the whole `/user` directory (including email addresses), writes profiles/presence under `/user/{uid}`, and duplicates messages under `/chats/{receiverUid + senderUid}/messages` and the reversed room ID. Concatenated IDs alone are not a trustworthy participant authorization model. Secure chat access may require an explicit membership schema and a separate reviewed implementation; do not enable public/test-mode rules to make the current client work.

[App Check with Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider) can complement Authentication and Rules for Realtime Database and supported Authentication protection. Plan a separate SDK integration and app registration, test valid/debug clients, monitor request metrics, then enable enforcement after supported clients are deployed. Enabling enforcement now could block this app because it does not send App Check tokens. App Check does not replace user authorization.

## Confirmed limitations

- Profile photos and contact lists do not synchronize across devices; photos are not uploaded to Firebase Storage.
- Messages are stored as text in Realtime Database; no end-to-end encryption or attachment transfer is implemented.
- Account deletion removes the Auth account and `/user/{uid}` profile, but does not delete existing chat history or local contacts/photos.
- Presence is updated by client activity lifecycle events, without server disconnect handling; it can become stale.
- There are no end-to-end Firebase tests or checked-in backend deployment/rules configuration. Runtime service behavior and provider security settings require owner verification.

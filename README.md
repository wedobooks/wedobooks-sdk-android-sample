# WeDoBooks SDK Sample App

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#wedobooks-sdk-sample-app)

This is a public sample app intended to serve as inspiration for integrators.

It primarily demonstrates how to sign in with a user, check out a book, and open it using the reader component. While the reader is designed for full-screen use, it can also be embedded in smaller views. You can also explore theming, localization, and various UI configuration options.

> **Note:** This app requires access to the WeDoBooks SDK backend and additional credentials to function correctly. Contact us to request access.

---

## Setup

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#setup)

- Access to the Maven server hosting the SDK, including a username and password  
- A demo user ID for use with our demo backend  
- Reader API credentials (key and secret)

These values must be added to your [`local.properties`](https://github.com/wedobooks/wedobooks-sdk-android-sample#localproperties) file. See below for details

---

### Local.properties

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#localproperties)

The project expects the following entries in your `local.properties` file:

```
WDB_USER_NAME=<username>  
WDB_PASSWORD=<password>  
READER_API_KEY="<key>" 
READER_API_SECRET="<secret>"  
DEMO_USER_ID="<user-id>"
```

Once you've added these values, sync the Gradle project.

---

### Android Manifest

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#android-manifest)

Make sure to include the following permissions in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />  
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />  
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />  
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

---

## Backend-to-Backend Integration

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#backend-to-backend-integration)

Signing a user into the WeDoBooks SDK requires a custom token.  
In a production setup, this token must be obtained through backend-to-backend integration and passed to the app.

For demo purposes, this app uses a demo backend endpoint to function without requiring backend integration.  
Refer to `LoginViewModel.login()` for the demo call implementation.

---

## Overview of the App

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#overview-of-the-app)

Working with the SDK generally follows this pattern:

- The WeDoBooksFacade singleton is the gateway to all the SDK functionality and can be accessed like this  `WeDoBooksSDK`

- Before using any SDK features, call the `setup()` method on the singleton.  
  This must only be called once. Repeated calls will result in an error.  
  You can use `WeDoBooksThemeConfiguration.builder().build()` to create a default theme configuration, or customize it as needed.

- SDK functionality is grouped into namespaces, accessible through properties on the `WeDoBooksSDK` singleton.  
  Current namespaces include:  
  `bookOperations`, `storageOperations`, `userOperations`, `localization`, `styling`, `images`, and `easyAccess`.

- A user must be signed in to perform book operations.  
  This can be checked using `userOperations.currentUserId`, or by observing the `currentUserIdFlow`.

- If no user is signed in, the sample app presents a login screen.  
  Tapping the login button will log in with the demo user ID specified in [`local.properties`](https://github.com/wedobooks/wedobooks-sdk-android-sample#localproperties).

- Once signed in, books can be checked out and opened via the `bookOperations` namespace.

- To show a custom loading screen while pages load, use `styling.setLoadingSVG()` with the entire SVG string as input:  
  ```kotlin
  WeDoBooksSDK.styling.setLoadingSVG("<svg>...</svg>")
  ```

---

## Localization and Icon Customization
[](https://github.com/wedobooks/wedobooks-sdk-android-sample#localization-and-icon-customization)

To customize localization strings or player icons, use the `localization` and `images` namespaces.

Use the `setEbookLocalization()` and `changeAudioPlayerIcons()` methods, which both accept a `Map<Int, Int>`.  
The keys represent specific localizable strings or icon identifiers, and the values are resource references (e.g., `R.string.*`, `R.drawable.*`).

Annotations such as `EbookStringKey` and `AudioPlayerIconKey` are available to improve type safety.

Example:

```kotlin
WeDoBooksSDK.images.changeAudioPlayerIcons(
    mapOf(
        AudioPlayerIconKey.PLAY to R.drawable.ic_play,
        AudioPlayerIconKey.PAUSE to R.drawable.ic_pause
    )
)

WeDoBooksSDK.localization.setEbookLocalization(
    mapOf(
        EbookStringKey.BUTTONS_CANCEL to R.string.general_cancel
    )
)
```

---
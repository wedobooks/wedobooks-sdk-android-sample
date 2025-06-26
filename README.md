# WeDoBooks SDK Sample App

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#wedobooks-sdk-sample-app)

Public sample app as inspiration for integrators.

It mainly demonstrates how to sign in with user, checkout a book and open how to get the reader (do note it is designed for fullscreen but it is possible to use it in smaller windows). It's also possible to play around with theming, localization and a few other configurations of the UI.

This app requires a WeDoBooks SDK backend and other credentials to function properly. Get in touch to obtain access.

## Setup

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#setup)

In order to get up and running you need the following (which will be delivered by WeDoBooks when requested):

-   Access to the maven server that's delivering the SDK through a username and password .
-   A demo user id of a user in our demo backend.
-   Credentials to the reader component.

where these will be used can be found in the [Local.properties segment](https://github.com/wedobooks/wedobooks-sdk-android-sample#localproperties)
### Local.properties

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#localproperties)

The project expects these values in Local.properties:

```
WDB_USER_NAME=<username>  
WDB_PASSWORD=<password>  
READER_API_KEY="<key>" 
READER_API_SECRET="<secret>"  
DEMO_USER_ID="<user-id>"
```
here is an example, this is by no means a working example
```
WDB_USER_NAME=wdbuser  
WDB_PASSWORD=wdbpass 
READER_API_KEY="erjt84443" 
READER_API_SECRET="3wcfeq2"  
DEMO_USER_ID="gfralisdfjiw"
```

These values are should be added and then afterwards do a gradle sync take note that READER_API_KEY, READER_API_SECRET and DEMO_USER_ID is first added when building

### Manifest uses

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#manifest-uses)

make sure to add this to your manifest

```
<uses-permission android:name="android.permission.INTERNET" />  
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />  
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />  
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

## Backend to backend integration

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#backend-to-backend-integration)

In order to sign a user in to the WeDoBooks SDK you need a so-called custom token. In a real app this would have to be obtained through a backend to backend integration and passed to the app. For demo purposes this app utilizes a demo backend endpoint in order to function without having the backend to backend integration setup. See  `LoginViewModel.login()`  for the call to the demo backend endpoint.

## Overview of the app

[](https://github.com/wedobooks/wedobooks-sdk-android-sample#overview-of-the-app)

Working with the SDK follows this outline:

-   The WeDoBooksFacade singleton is the gateway to all the SDK functionality and can be accessed like this  `WeDoBooksSDK`
-   Before doing anything else with the SDK you must call the  `setup`  method on the WeDoBooksSDK singleton instance. This should only be called once and will throw an error if it's called more than once. For the setup you can make use of `WeDoBooksThemeConfiguration.builder().build()` for the `themeConfig` property, for a standard theme or you can set values as you want it.
-   All other functionality in the SDK is grouped in namespaces which can be accessed through properties on the WeDoBooksFacade instance. At the time of writing these namespaces exist:  `bookOperations`,  `storageOperations`,  `localization`,  `styling`  and  `userOperations`, `images`, `easyAccess`.
-   In order to do any book operations a user needs to be signed in. This can be checked by collecting the flow  `currentUserIdFlow` or just grab `currentUserId` in  `userOperations`  namespace property.
-   If no user is signed in the sample app displays a login screen with just a login button, where it will log into the account specified in the [Local.properties](https://github.com/wedobooks/wedobooks-sdk-android-sample#localproperties) file
-   After having signed in then you can checkout and open books through the  `bookOperations`  namespace property.

## Localization and Icon changes
[](https://github.com/wedobooks/wedobooks-sdk-android-sample#localization-and-icon-changes)

When you want to change localization or some icons, then make use of the `images` or `localization` namespaces with either `localization.setEbookLocalization` or `images.changeAudioPlayerIcon`, these takes a paramater of `Map<Int, Int>` where the key is what you localization/image you want to change and the value is a resource `R.drawable.<something>` or `R.string.<something>`

for ease of use Int Annotations such as `EbookStringKey`,  `AudioPlayerStringKey` can be used instead of doing guesswork
for a small example 
```
WeDoBooksSDK.images.changeAudioPlayerIcons(  
    mapOf<Int, Int>(  
        AudioPlayerIconKey.PLAY to R.drawable.ic_play,
        AudioPlayerIconKey.PAUSE to R.drawable.ic_pause,
    )  
)

WeDoBooksSDK.localization.setEbookLocalization(  
    mapOf<Int, Int>(  
        EbookStringKey.BUTTONS_CANCEL to R.string.general_cancel  
    )  
)
```

[README.md](https://github.com/user-attachments/files/32548681/README.md)
# Soil Resistivity Android App

The app opens with a two-second animated splash screen showing the app icon and title.

An offline Android application for entering Wenner (alpha) field measurements and calculating apparent resistivity.

## Features

- Manual `MN/2`, `AB/2`, and resistance entry
- Automatic geometric factor and apparent-resistivity calculation
- Logarithmic resistivity graph using the correct Wenner `AB/3` spacing axis
- Selectable 2–5 layer automatic curve fit
- Black measured points, red fitted curve, and blue layer step model
- Layer resistivity, thickness, cumulative depth, and fit error
- Save and reopen the most recent survey on the phone
- Automatic draft recovery after pressing Home or after Android closes the app
- Tap any reading row to edit it, then return automatically to `MN/2`
- Survey History with reopen, edit, export, and delete functions
- Built-in sample dataset from the supplied field sheet
- PDF report export
- Excel-compatible CSV export
- No internet permission and no third-party runtime libraries

## Calculation

`K = π × ((AB/2)² − (MN/2)²) ÷ (2 × MN/2)`

`ρa = K × R`

Distances are entered in metres and resistance in ohms, producing apparent resistivity in ohm-metres.

## Build in Android Studio

1. Install the latest Android Studio.
2. Select **Open** and choose the `SoilResistivityApp` folder.
3. Allow Gradle to synchronize and install Android SDK 35 if requested.
4. Connect an Android phone with USB debugging enabled, or create an emulator.
5. Press **Run** to test it.
6. To create an installable APK, select **Build > Build APK(s)**.
7. Android Studio places it under `app/build/outputs/apk/debug/app-debug.apk`.

The project uses Java 17, Android API 35, and supports Android 7.0 and newer.

## Change the app icon later

The default icon is `app/src/main/res/drawable/app_icon.xml`. To use your own icon:

1. Prepare a square PNG, preferably 1024 × 1024.
2. Rename it `app_icon.png`.
3. Delete `app_icon.xml` from the same GitHub folder.
4. Upload `app_icon.png` to `app/src/main/res/drawable/` and commit it.
5. GitHub Actions will build a new APK with the replacement icon.

The workflow caches one debug signing key and uses an increasing GitHub run number as the Android version code. After installing the first APK produced by this updated workflow, future APKs should install as updates without clearing saved survey history.

## Build the APK on GitHub (no Android Studio)

1. Create a new GitHub repository and keep it private.
2. Upload the **contents** of this `SoilResistivityApp` folder to the repository root.
3. Open the repository's **Actions** tab and select **Build Android APK**.
4. Choose **Run workflow**. A build also starts automatically after the first upload.
5. Open the completed build, then download the `SoilResistivity-APK` artifact.
6. Extract that artifact ZIP to obtain `app-debug.apk`.

## Important scientific note

The automatic layer model is a fast smooth curve-fitting estimate for preliminary field screening. It is not a reproduction of IPI2Win's proprietary inversion engine. Confirm layer interpretations with qualified geophysical analysis before engineering, drilling, construction, or safety decisions.

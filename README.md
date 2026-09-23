# Soil Resistivity Android App — update 1.4

The app opens with a two-second animated splash screen showing the app icon and title.

An offline Android application for plotting and fitting Wenner-alpha VES apparent-resistivity measurements.

## Features

- Wenner-only input: `a` and direct `ρa`; graph X-axis `a = AB/3`
- Correct direct apparent-resistivity handling (no incorrect second `K × R` conversion)
- Physical layered-earth forward response using the Koefoed resistivity transform and Wenner potential integral
- Damped, robust 2–5 layer inversion in logarithmic model space
- Full-decade IPI2Win-style graph limits and a visible engine-version label
- Automatic invalidation of models previously saved by the obsolete sigmoid fitter
- Black measured points, red fitted curve, and blue layer step model
- Layer `ρ`, thickness `h`, cumulative depth `d`, altitude `Alt = -d`, and fit error
- Save and reopen the most recent survey on the phone
- Automatic draft recovery after pressing Home or after Android closes the app
- Tap any reading row to edit it, then return automatically to the `a` input
- Survey History with reopen, edit, export, and delete functions
- Built-in sample dataset from the supplied field sheet
- PDF report export
- Excel-compatible CSV export
- No internet permission and no third-party runtime libraries

The supplied third measurement is already apparent resistivity `ρa` in Ωm. Ground level is `0 m`; depth increases downward and altitude is negative below ground. The final half-space shows `h = ∞`, `d = —`, and `Alt = —`.

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
3. Open the repository's **Actions** tab and select **Build Android APK**. Pushes from `update-1.2` and other branches are supported.
4. Choose **Run workflow**. A build also starts automatically after the first upload.
5. Open the completed build, then download the `SoilResistivity-APK` artifact.
6. Extract that artifact ZIP to obtain `app-debug.apk`.

## Important scientific note

The graph and array axis follow the supplied IPI2Win Wenner screenshots. Update 1.4 uses a physical layered-earth calculation and discards results cached by the old sigmoid engine. It remains an independent implementation, not IPI2Win's private filters or least-layer engine. Equivalent layered-earth models can fit the same VES curve, so `ρ` and `h` can differ from IPI2Win even when the calculated curve is a good fit. Confirm interpretations with qualified geophysical analysis before engineering, drilling, construction, or safety decisions.

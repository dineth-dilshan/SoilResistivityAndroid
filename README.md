# Soil Resistivity Android App

An offline Android application for entering Wenner (alpha) field measurements and calculating apparent resistivity.

## Features

- Manual `MN/2`, `AB/2`, and resistance entry
- Automatic geometric factor and apparent-resistivity calculation
- Logarithmic resistivity graph using the correct Wenner `AB/3` spacing axis
- Selectable 2–5 layer automatic curve fit
- Black measured points, red fitted curve, and blue layer step model
- Layer resistivity, thickness, cumulative depth, and fit error
- Save and reopen the most recent survey on the phone
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

## Important scientific note

The automatic layer model is a fast smooth curve-fitting estimate for preliminary field screening. It is not a reproduction of IPI2Win's proprietary inversion engine. Confirm layer interpretations with qualified geophysical analysis before engineering, drilling, construction, or safety decisions.

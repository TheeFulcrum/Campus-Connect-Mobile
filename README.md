# Campus Connect Mobile

Campus Connect Mobile is an Android application that provides a peer-to-peer marketplace for university students. Students can connect with their campus community, manage their profiles, and communicate through in-app chat.

## Features

- Student account registration and login
- OTP-based login flow
- Profile setup and management
- Campus marketplace experience
- In-app chat
- Splash screen and responsive Android UI
- Backend API configuration for authentication and application services

## Tech Stack

- **Language:** Java
- **Platform:** Android
- **Build system:** Gradle Kotlin DSL
- **Minimum Android version:** Android 8.0 (API 26)
- **Target Android version:** API 36
- **UI:** AndroidX, Material Design, ConstraintLayout
- **Additional libraries:** Facebook Shimmer, AndroidX SplashScreen

## Project Structure

```text
app/
└── src/
    └── main/
        ├── java/
        │   └── com/example/campusconnectmobile/
        ├── res/
        └── AndroidManifest.xml
```

Important activities include:

- `SplashActivity`
- `MainActivity`
- `SignUpActivity`
- `OtpLoginActivity`
- `ProfileSetupActivity`
- `HomeActivity`
- `ChatActivity`

## Requirements

- Android Studio
- JDK 11 or later
- Android SDK with API 36
- An available Campus Connect backend API

## Configuration

The authentication API base URL is configured through the Gradle property:

```properties
ccwApiBaseUrl=http://10.0.2.2:8000/
```

For Android Emulator development, `10.0.2.2` points to the host machine's localhost. To use a remote or deployed backend, provide the API URL when building:

```bash
./gradlew assembleDebug -PccwApiBaseUrl=https://your-api.example.com/
```

The application currently permits cleartext traffic for local development. Use HTTPS for production deployments.

## Building the Application

Clone the repository and open it in Android Studio:

```bash
git clone https://github.com/TheeFulcrum/Campus-Connect-Mobile.git
cd Campus-Connect-Mobile
```

Build the debug APK from the command line:

```bash
./gradlew assembleDebug
```

The generated APK will be available under:

```text
app/build/outputs/apk/debug/
```

## Running the Application

1. Start the Campus Connect backend API.
2. Configure `ccwApiBaseUrl` if the backend is not running at the default emulator address.
3. Open the project in Android Studio.
4. Connect an Android device or start an emulator running Android 8.0 or newer.
5. Run the `app` configuration.

## Testing

Run unit tests with:

```bash
./gradlew test
```

Run instrumentation tests with:

```bash
./gradlew connectedAndroidTest
```

## Related Project

The mobile application is part of the broader Campus Connect platform and is designed to work alongside the Campus Connect web application and backend services.

## License

No license has currently been specified for this repository.

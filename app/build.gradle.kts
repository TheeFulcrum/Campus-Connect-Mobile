plugins {
    alias(libs.plugins.android.application)
}

val authApiBaseUrl = providers.gradleProperty("ccwApiBaseUrl")
    .orElse("https://auth.campusconnect.ink/")
    .get()

android {
    namespace = "com.example.campusconnectmobile"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.campusconnectmobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "CCW_AUTH_API_BASE_URL", "\"$authApiBaseUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.core.splashscreen)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.facebook.shimmer)
    implementation(libs.fragment)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
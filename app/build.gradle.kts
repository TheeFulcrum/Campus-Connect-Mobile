plugins {
    alias(libs.plugins.android.application)
}

val authApiBaseUrl = providers.gradleProperty("ccwApiBaseUrl")
    .orElse("https://auth.campusconnect.ink/")
    .get()

// Firebase client settings (Firebase console > Project settings > Your apps). Push stays off until all are set,
// e.g. in ~/.gradle/gradle.properties:  ccwFcmProjectId=...  ccwFcmAppId=...  ccwFcmApiKey=...  ccwFcmSenderId=...
fun fcmProperty(name: String): String = providers.gradleProperty(name).orElse("").get()

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
        buildConfigField("String", "FCM_PROJECT_ID", "\"${fcmProperty("ccwFcmProjectId")}\"")
        buildConfigField("String", "FCM_APP_ID", "\"${fcmProperty("ccwFcmAppId")}\"")
        buildConfigField("String", "FCM_API_KEY", "\"${fcmProperty("ccwFcmApiKey")}\"")
        buildConfigField("String", "FCM_SENDER_ID", "\"${fcmProperty("ccwFcmSenderId")}\"")

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
        viewBinding = true
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
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)
    implementation(libs.glide)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.zxing.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
configurations.all {
    exclude(group = "com.android.support", module = "support-compat")
}
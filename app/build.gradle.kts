plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val padhoKeystorePath = System.getenv("PADHO_KEYSTORE_PATH")
val padhoKeystorePassword = System.getenv("PADHO_KEYSTORE_PASSWORD")
val padhoKeyAlias = System.getenv("PADHO_KEY_ALIAS")
val padhoKeyPassword = System.getenv("PADHO_KEY_PASSWORD")
val hasPadhoSigning = listOf(
    padhoKeystorePath, padhoKeystorePassword, padhoKeyAlias, padhoKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.padho.reader"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.padho.reader"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    signingConfigs {
        create("padhoRelease") {
            if (hasPadhoSigning) {
                storeFile = file(padhoKeystorePath!!)
                storePassword = padhoKeystorePassword
                keyAlias = padhoKeyAlias
                keyPassword = padhoKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasPadhoSigning) {
                signingConfig = signingConfigs.getByName("padhoRelease")
            }
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.google.mlkit:translate:17.0.3")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

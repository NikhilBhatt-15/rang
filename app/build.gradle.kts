import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nikhilbhatt.rang"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nikhilbhatt.rang"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    // Release signing comes from keystore.properties (kept out of git); without it, release builds are unsigned.
    val keystoreProps = rootProject.file("keystore.properties").takeIf { it.isFile }?.let { f ->
        Properties().apply { f.inputStream().use(::load) }
    }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    testImplementation("junit:junit:4.13.2")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.batteryng.mvp"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.batteryng.mvp"
        minSdk = 23
        targetSdk = 34
        versionCode = 14
        versionName = "0.12.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    testImplementation("junit:junit:4.13.2")
}

tasks.register<Copy>("exportDebugApk") {
    group = "build"
    description = "Builds the debug APK and copies it to dist/BatteryNG-debug.apk."
    dependsOn("assembleDebug")
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename("app-debug.apk", "BatteryNG-debug.apk")
}

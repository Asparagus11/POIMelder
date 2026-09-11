plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.poimelder.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.poimelder.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 16
        versionName = "16 (Pastell-Theme)"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release-Signatur nur dann, wenn die noetigen Umgebungsvariablen gesetzt sind.
    // Lokal (Debug/Nextcloud) bleibt alles wie bisher; auf dem GitHub-Runner liefert
    // der Release-Workflow die vier Variablen aus verschluesselten Secrets. Der
    // Keystore liegt NIE im Repo.
    val keystorePath = System.getenv("KEYSTORE_FILE")
    val hasSigning = keystorePath != null && file(keystorePath).exists()

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Nur wenn ein Keystore bereitsteht. Sonst bleibt der Release-Build
            // unsigniert – das faellt beim Installieren auf, statt still mit dem
            // Debug-Key zu signieren.
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    applicationVariants.all {
        val variant = this
        val versionNum = variant.versionCode
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val suffix = if (variant.buildType.name == "release") "_release" else ""
            output.outputFileName = "poimelder${suffix}_v${versionNum}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose - ALL pinned to 1.6.0 to avoid version conflicts (siehe android-stability)
    val composeVersion = "1.6.0"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // OkHttp (Overpass-API, Task 3)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson – JSON-Parsing für Overpass (funktioniert auch in JVM-Unit-Tests)
    implementation("com.google.code.gson:gson:2.10.1")

    // osmdroid (Kartenansicht, Task 10)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
}

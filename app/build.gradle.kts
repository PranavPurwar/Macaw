plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.pranav.macaw"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.pranav.macaw"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.activity)
    implementation(libs.gson)

    implementation(libs.editor)
    implementation(libs.language.textmate)
    implementation(libs.dfc)
    implementation(libs.coil)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)
    implementation(libs.anggrayudi.storage)
    implementation(libs.java.diff.utils)
    implementation(libs.compose.markdown)
    implementation(libs.compose.preferences)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.session)

    // Optional: For additional audio format support
    implementation(libs.androidx.media3.decoder)
    implementation(libs.androidx.media3.extractor)

    // For better notification handling
    implementation(libs.androidx.media3.common)

    implementation(libs.saket.cascade)
    implementation(libs.saket.squigglyslider)
    implementation(libs.saket.zoomable)
    implementation(libs.saket.zoomable.coil3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

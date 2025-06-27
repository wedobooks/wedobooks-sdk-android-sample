import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.wedobooks.sdk.library.wedobookssdksampleapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.wedobooks.sdk.library.wedobookssdksampleapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "READER_API_KEY",
            getLocalProperty("READER_API_KEY").toString()
        )
        buildConfigField("String", "READER_API_SECRET",
            getLocalProperty("READER_API_SECRET").toString()
        )
        buildConfigField("String", "DEMO_USER_ID",
            getLocalProperty("DEMO_USER_ID").toString()
        )
        buildConfigField("String", "FIREBASE_API_KEY",
            getLocalProperty("FIREBASE_API_KEY").toString()
        )
        buildConfigField("String", "FIREBASE_APP_ID",
            getLocalProperty("FIREBASE_APP_ID").toString()
        )
        buildConfigField("String", "FIREBASE_PROJECT_ID",
            getLocalProperty("FIREBASE_PROJECT_ID").toString()
        )
        buildConfigField("String", "CUSTOM_TOKEN_URL",
            getLocalProperty("CUSTOM_TOKEN_URL").toString())
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.sdk)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
}

fun getLocalProperty(key: String): String? {
    val localPropertiesFile = rootDir.resolve("local.properties")
    if (!localPropertiesFile.exists()) return null

    val props = Properties()
    localPropertiesFile.inputStream().use { props.load(it) }
    return props.getProperty(key)
}
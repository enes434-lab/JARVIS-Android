plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "com.jarvis.assistant"; compileSdk = 35
    defaultConfig { applicationId = "com.jarvis.assistant"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0" }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

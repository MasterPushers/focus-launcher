plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.focus.fixture.post"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.focus.fixture.post"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

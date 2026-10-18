plugins { id("com.android.application") }
android {
    namespace = "dev.kaede.pixelboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.kaede.pixelboard"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { compileOnly("io.github.libxposed:api:102.0.0"); implementation("io.github.libxposed:service:102.0.0") }

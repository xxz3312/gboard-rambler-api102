plugins { id("com.android.application") }
android {
    namespace = "dev.kaede.pixelboard"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.kaede.pixelboard"
        minSdk = 29
        targetSdk = 35
        versionCode = 43
        versionName = "0.2.5"
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug") // CI test certificate; see RELEASE_NOTES.md.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { compileOnly("io.github.libxposed:api:102.0.0"); implementation("io.github.libxposed:service:102.0.0"); implementation("com.google.android.material:material:1.13.0"); implementation("org.luckypray:dexkit:2.3.0"); testImplementation("junit:junit:4.13.2"); testImplementation("org.robolectric:robolectric:4.14.1") }



plugins { id("com.android.application") }
android {
    namespace = "dev.kaede.pixelboard"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.kaede.pixelboard"
        minSdk = 29
        targetSdk = 35
        versionCode = 28
        versionName = "0.1.26-model-matrix"
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation("org.luckypray:dexkit:2.3.0"); compileOnly("io.github.libxposed:api:102.0.0"); implementation("io.github.libxposed:service:102.0.0"); testImplementation("junit:junit:4.13.2"); implementation("com.google.mlkit:genai-prompt:1.0.0-beta4"); implementation("com.google.mlkit:genai-rewriting:1.0.0-beta1"); implementation("com.google.mlkit:genai-proofreading:1.0.0-beta1") }




// Prompt beta4 invokes interface-static Job.cancel$default, added in coroutines 1.11.
// Its published metadata can otherwise resolve an older, binary-incompatible runtime.
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}

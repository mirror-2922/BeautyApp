plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Official way to extract AAR content if prefab is missing/broken
val extractOrtAar by tasks.registering(Copy::class) {
    val configuration = configurations.detachedConfiguration(dependencies.create("com.microsoft.onnxruntime:onnxruntime-android:1.18.0"))
    val aarFile = configuration.resolve().first()
    from(zipTree(aarFile))
    into(layout.buildDirectory.dir("intermediates/ort-extracted"))
}

android {
    namespace = "com.example.beautyapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.beautyapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        ndkVersion = "28.2.13676358"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                arguments("-DANDROID_STL=c++_shared")
                // Use linker flags to ignore non-fatal property errors in older static libs (like OpenCV's ippicv on x86)
                arguments("-DANDROID_CPP_FEATURES=rtti exceptions")
                cppFlags("-Wno-unused-command-line-argument")
                // Pass the extracted path to CMake
                arguments("-DORT_PATH=${layout.buildDirectory.dir("intermediates/ort-extracted").get().asFile.absolutePath}")
            }
        }
        // Removed mandatory abiFilters to support all platforms
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; prefab = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

tasks.withType<com.android.build.gradle.tasks.ExternalNativeBuildTask> {
    dependsOn(extractOrtAar)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.opencv)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    testImplementation(libs.junit)
}

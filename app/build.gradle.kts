plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id ("androidx.navigation.safeargs")
    id("kotlin-parcelize")
//    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.alya.ecommerce_serang"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alya.ecommerce_serang"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.13:3000/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.13:3000/\"")
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    implementation(libs.glide)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.swiperefreshlayout)


//    implementation(libs.hilt.android)
//    kapt("com.google.dagger:hilt-compiler:2.48")
//
//    // For ViewModel injection (if needed)
//    implementation(libs.androidx.hilt.lifecycle.viewmodel)
//    kapt("androidx.hilt:hilt-compiler:1.0.0")
}


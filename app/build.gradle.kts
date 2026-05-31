plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
//    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
}

android {
    namespace = "com.example.animouse"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.animouse"
        minSdk = 27
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.cardview:cardview:1.0.0")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // Gson
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Glide для загрузки постеров
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // Room
//    implementation("androidx.room:room-runtime:2.7.1")
//    implementation("androidx.room:room-ktx:2.7.1")
//    ksp("androidx.room:room-compiler:2.7.1")
}
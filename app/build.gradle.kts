import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}



val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

val MAGIC_LINK: String? = localProperties.getProperty("MAGIC_LINK")
val TATA_AIG_BASE_URL: String? = localProperties.getProperty("TATA_AIG_BASE_URL")
val TATA_AIG_AUTH_TOKEN: String? = localProperties.getProperty("TATA_AIG_AUTH_TOKEN")


android {
    namespace = "com.example.visitandroidsdk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.visitandroidsdk"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MAGIC_LINK", "\"${MAGIC_LINK ?: ""}\"")
        buildConfigField("String", "TATA_AIG_BASE_URL", "\"${TATA_AIG_BASE_URL ?: ""}\"")
        buildConfigField("String", "TATA_AIG_AUTH_TOKEN", "\"${TATA_AIG_AUTH_TOKEN ?: ""}\"")
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
    buildFeatures {
        buildConfig = true
        dataBinding = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {

    implementation(project(":visit"))
    // implementation(files("visit-debug.aar"))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.firebase:firebase-auth:24.0.1")

    implementation("com.google.android.gms:play-services-fitness:21.3.0")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    implementation("com.twilio:video-android:7.9.0")
    implementation("com.twilio:audioswitch:1.2.4")

    implementation("com.github.bumptech.glide:glide:4.11.0")
    kapt("com.github.bumptech.glide:compiler:4.11.0")

    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    implementation("io.reactivex:rxjava:1.3.8")
    implementation("io.reactivex:rxandroid:1.2.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    val retrofitVersion = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")

    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    implementation("com.github.delight-im:Android-AdvancedWebView:v3.0.0")

    implementation("com.getkeepsafe.relinker:relinker:1.4.4") {
        version {
            strictly("1.4.4")
        }
    }

    implementation("org.greenrobot:eventbus:3.3.1")

    // Chucker
    debugImplementation("com.github.chuckerteam.chucker:library:4.2.0")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:4.2.0")

    implementation("androidx.browser:browser:1.9.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
}
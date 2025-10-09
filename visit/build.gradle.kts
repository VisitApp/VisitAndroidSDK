plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("maven-publish")
}

android {
    namespace = "com.getvisitapp.google_fit"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        targetSdk = 36
        // Note: multiDexEnabled applies to application modules; it’s ignored for libraries.
        // multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            buildConfigField("String", "APP_MODE", "\"RELEASE\"")
            buildConfigField("String", "PROTOCOL", "\"https\"")
            buildConfigField("String", "DOMAIN_NAME", "\"getvisitapp\"")
            buildConfigField("String", "SUB_DOMAIN_NAME", "\"api\"")
            buildConfigField("String", "TOP_LEVEL_DOMAIN", "\"com\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("String", "APP_MODE", "\"DEBUG\"")
            buildConfigField("String", "PROTOCOL", "\"https\"")
            buildConfigField("String", "DOMAIN_NAME", "\"samuraijack\"")
            buildConfigField("String", "SUB_DOMAIN_NAME", "\"api\"")
            buildConfigField("String", "TOP_LEVEL_DOMAIN", "\"xyz\"")
        }
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

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
}

dependencies {

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

    implementation("com.google.code.gson:gson:2.13.2")
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

    implementation("androidx.browser:browser:1.9.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")


    implementation("com.getkeepsafe.relinker:relinker:1.4.4") {
        version {
            strictly("1.4.4")
        }
    }
}

// Publishing: keep the same semantics as your Groovy afterEvaluate usage.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.getvisitapp"
                artifactId = "visit"
                version = "1.89"
            }
        }
    }
}
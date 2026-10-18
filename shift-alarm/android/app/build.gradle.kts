plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.shiftalarm.app"; compileSdk = 35
 defaultConfig { applicationId = "com.shiftalarm.app"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}

dependencies { implementation("com.tom-roush:pdfbox-android:2.0.27.0") }

/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties

android {
  namespace = "com.meta.wearable.dat.externalsampleapps.cameraaccess"
  compileSdk = 35

  buildFeatures { buildConfig = true }

  defaultConfig {
    applicationId = "com.meta.wearable.dat.externalsampleapps.cameraaccess"
    minSdk = 31
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }

    val lp = Properties()
    val lpFile = rootProject.file("local.properties")
    if (lpFile.exists()) lp.load(lpFile.inputStream())
    buildConfigField("String", "AWS_ACCESS_KEY", "\"${lp.getProperty("aws_access_key", "")}\"")
    buildConfigField("String", "AWS_SECRET_KEY", "\"${lp.getProperty("aws_secret_key", "")}\"")
    buildConfigField("String", "AWS_REGION", "\"${lp.getProperty("aws_region", "us-east-1")}\"")
    buildConfigField("String", "ELEVENLABS_API_KEY", "\"${lp.getProperty("elevenlabs_api_key", "")}\"")
    buildConfigField("String", "ELEVENLABS_VOICE_ID", "\"${lp.getProperty("elevenlabs_voice_id", "")}\"")

    manifestPlaceholders["META_APPLICATION_ID"] = lp.getProperty("meta_application_id", "0")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
  }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,io.netty.versions.properties,versions/9/OSGI-INF/MANIFEST.MF}" } }
  signingConfigs {
    getByName("debug") {
      storeFile = file("sample.keystore")
      storePassword = "sample"
      keyAlias = "sample"
      keyPassword = "sample"
    }
  }
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.exifinterface)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.androidx.material3)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.mwdat.core)
  implementation(libs.mwdat.camera)
  implementation(libs.mwdat.mockdevice)
  implementation(libs.aws.bedrockruntime)
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  androidTestImplementation(libs.androidx.ui.test.junit4)
  androidTestImplementation(libs.androidx.test.uiautomator)
  androidTestImplementation(libs.androidx.test.rules)
}

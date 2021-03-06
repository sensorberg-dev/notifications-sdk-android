import kotlin.String

/**
 * Generated by https://github.com/jmfayard/buildSrcVersions
 *
 * Update this file with
 *   `$ ./gradlew buildSrcVersions`
 */
object Libs {
  /**
   * https://developer.android.com/jetpack/androidx
   */
  const val appcompat: String = "androidx.appcompat:appcompat:" + Versions.appcompat

  /**
   * http://tools.android.com
   */
  const val constraintlayout: String = "androidx.constraintlayout:constraintlayout:" +
      Versions.constraintlayout

  /**
   * https://developer.android.com/topic/libraries/architecture/index.html
   */
  const val lifecycle_extensions: String = "androidx.lifecycle:lifecycle-extensions:" +
      Versions.lifecycle_extensions

  /**
   * https://developer.android.com/topic/libraries/architecture/index.html
   */
  const val room_compiler: String = "androidx.room:room-compiler:" + Versions.androidx_room

  /**
   * https://developer.android.com/topic/libraries/architecture/index.html
   */
  const val room_runtime: String = "androidx.room:room-runtime:" + Versions.androidx_room

  /**
   * https://developer.android.com/topic/libraries/architecture/index.html
   */
  const val room_testing: String = "androidx.room:room-testing:" + Versions.androidx_room

  /**
   * https://developer.android.com/testing
   */
  const val espresso_core: String = "androidx.test.espresso:espresso-core:" + Versions.espresso_core

  /**
   * https://developer.android.com/testing
   */
  const val androidx_test_runner: String = "androidx.test:runner:" + Versions.androidx_test_runner

  /**
   * https://developer.android.com/topic/libraries/architecture/index.html
   */
  const val work_gcm: String = "androidx.work:work-gcm:" + Versions.androidx_work

  /**
   * https://developer.android.com/topic/libraries/architecture/index.html
   */
  const val work_runtime: String = "androidx.work:work-runtime:" + Versions.androidx_work

  /**
   * https://github.com/kungfoo/geohash-java
   */
  const val geohash: String = "ch.hsr:geohash:" + Versions.geohash

  /**
   * https://developer.android.com/studio
   */
  const val aapt2: String = "com.android.tools.build:aapt2:" + Versions.aapt2

  /**
   * https://developer.android.com/studio
   */
  const val com_android_tools_build_gradle: String = "com.android.tools.build:gradle:" +
      Versions.com_android_tools_build_gradle

  /**
   * https://developer.android.com/studio
   */
  const val lint_gradle: String = "com.android.tools.lint:lint-gradle:" + Versions.lint_gradle

  const val firebase_jobdispatcher: String = "com.firebase:firebase-jobdispatcher:" +
      Versions.firebase_jobdispatcher

  const val play_services_location: String = "com.google.android.gms:play-services-location:" +
      Versions.com_google_android_gms

  const val play_services_nearby: String = "com.google.android.gms:play-services-nearby:" +
      Versions.com_google_android_gms

  /**
   * https://github.com/JakeWharton/ThreeTenABP/
   */
  const val threetenabp: String = "com.jakewharton.threetenabp:threetenabp:" + Versions.threetenabp

  /**
   * https://github.com/JakeWharton/timber
   */
  const val timber: String = "com.jakewharton.timber:timber:" + Versions.timber

  /**
   * https://github.com/sensorberg-dev/permission-bitte
   */
  const val permission_bitte: String = "com.sensorberg.libs:permission-bitte:" +
      Versions.permission_bitte

  /**
   * https://github.com/square/moshi
   */
  const val moshi_kotlin_codegen: String = "com.squareup.moshi:moshi-kotlin-codegen:" +
      Versions.com_squareup_moshi

  /**
   * https://github.com/square/moshi
   */
  const val moshi: String = "com.squareup.moshi:moshi:" + Versions.com_squareup_moshi

  /**
   * https://github.com/square/okhttp
   */
  const val logging_interceptor: String = "com.squareup.okhttp3:logging-interceptor:" +
      Versions.com_squareup_okhttp3

  /**
   * https://github.com/square/okhttp
   */
  const val okhttp: String = "com.squareup.okhttp3:okhttp:" + Versions.com_squareup_okhttp3

  /**
   * https://github.com/square/retrofit/
   */
  const val converter_moshi: String = "com.squareup.retrofit2:converter-moshi:" +
      Versions.com_squareup_retrofit2

  /**
   * https://github.com/square/retrofit/
   */
  const val retrofit: String = "com.squareup.retrofit2:retrofit:" + Versions.com_squareup_retrofit2

  /**
   * http://github.com/vanniktech/gradle-maven-publish-plugin/
   */
  const val gradle_maven_publish_plugin: String = "com.vanniktech:gradle-maven-publish-plugin:" +
      Versions.gradle_maven_publish_plugin

  const val de_fayard_buildsrcversions_gradle_plugin: String =
      "de.fayard.buildSrcVersions:de.fayard.buildSrcVersions.gradle.plugin:" +
      Versions.de_fayard_buildsrcversions_gradle_plugin

  /**
   * http://mockk.io
   */
  const val mockk: String = "io.mockk:mockk:" + Versions.mockk

  /**
   * http://junit.org
   */
  const val junit: String = "junit:junit:" + Versions.junit

  /**
   * https://kotlinlang.org/
   */
  const val kotlin_android_extensions_runtime: String =
      "org.jetbrains.kotlin:kotlin-android-extensions-runtime:" + Versions.org_jetbrains_kotlin

  /**
   * https://kotlinlang.org/
   */
  const val kotlin_android_extensions: String = "org.jetbrains.kotlin:kotlin-android-extensions:" +
      Versions.org_jetbrains_kotlin

  /**
   * https://kotlinlang.org/
   */
  const val kotlin_annotation_processing_gradle: String =
      "org.jetbrains.kotlin:kotlin-annotation-processing-gradle:" + Versions.org_jetbrains_kotlin

  /**
   * https://kotlinlang.org/
   */
  const val kotlin_gradle_plugin: String = "org.jetbrains.kotlin:kotlin-gradle-plugin:" +
      Versions.org_jetbrains_kotlin

  /**
   * https://kotlinlang.org/
   */
  const val kotlin_stdlib_jdk7: String = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:" +
      Versions.org_jetbrains_kotlin

  const val json: String = "org.json:json:" + Versions.json

  const val koin_core: String = "org.koin:koin-core:" + Versions.koin_core
}

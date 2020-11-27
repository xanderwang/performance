package com.xander.performance.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project


class PerformancePlugin : Plugin<Project> {

  override fun apply(target: Project) {
    println("----------------------------------------")
    println("apply PerformancePlugin")
    var android = target.extensions.getByType(AppExtension::class.java)
    android.registerTransform(PerformanceTransform(target))
  }
}
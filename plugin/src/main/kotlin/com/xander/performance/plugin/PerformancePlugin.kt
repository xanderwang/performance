package com.xander.performance.plugin

import com.android.build.api.transform.Transform
import com.xander.plugin.asm.BasePlugin
import org.gradle.api.Project


class PerformancePlugin : BasePlugin() {

  override fun createTransforms(project: Project): List<Transform> {
    return listOf(PerformanceTransform(project))
  }

  override fun apply(project: Project) {
    println("----------------------------------------")
    println("apply PerformancePlugin")
    super.apply(project)
  }

}
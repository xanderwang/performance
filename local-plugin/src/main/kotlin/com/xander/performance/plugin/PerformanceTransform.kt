package com.xander.performance.plugin

import com.xander.plugin.asm.BaseClassTransform
import com.xander.plugin.asm.lib.BaseWeaverFactory
import com.xander.plugin.asm.lib.PluginConfig
import org.gradle.api.Project

/**
 * @author Xander Wang Created on 2020/11/27.
 * @Description //TODO
 */
class PerformanceTransform(project:Project) : BaseClassTransform(project) {

  private val config = PluginConfig()
  init {
    config.skipJar = false
    config.classLog = false
    config.log = false
  }

  override fun getName(): String {
    return "PerformanceTransform"
  }

  override fun getConfigName(): String {
    return "performanceConfig"
  }

  override fun createWeaver(): BaseWeaverFactory {
    return PerformanceWeaver()
  }
}
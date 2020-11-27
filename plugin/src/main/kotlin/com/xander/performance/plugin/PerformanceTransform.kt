package com.xander.performance.plugin

import com.xander.plugin.asm.BaseTransform
import com.xander.plugin.asm.lib.BaseWeaverFactory
import com.xander.plugin.asm.lib.PluginConfig
import org.gradle.api.Project

/**
 * @author Xander Wang Created on 2020/11/27.
 * @Description //TODO
 */
class PerformanceTransform(project:Project) : BaseTransform(project) {

  private val config = PluginConfig()
  init {
    config.skipJar = false
  }

  override fun getName(): String {
    return "PerformanceTransform"
  }

  override fun createPluginConfig(): PluginConfig {
    return config
  }

  override fun createWeaver(): BaseWeaverFactory {
    return PerformanceWeaver()
  }
}
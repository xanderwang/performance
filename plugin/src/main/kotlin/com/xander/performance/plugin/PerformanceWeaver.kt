package com.xander.performance.plugin

import com.xander.plugin.asm.lib.BaseClassVisitor
import com.xander.plugin.asm.lib.BaseWeaverFactory
import com.xander.plugin.asm.lib.IWeaverFactory
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor

/**
 *
 * @Description //TODO
 *
 * @author Xander Wang
 * Created on 2020/11/27.
 */

class PerformanceWeaver : BaseWeaverFactory() {
  override fun createClassVisitor(classWriter: ClassWriter): ClassVisitor {
    // return super.createClassVisitor(classWriter)
    return PerformanceClassVisitor(classWriter, this);
  }
}

class PerformanceClassVisitor(cv: ClassVisitor, iWeaverFactory: IWeaverFactory) : BaseClassVisitor(cv, iWeaverFactory) {
  override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
    var superClassName = superName
    if("android/os/Handler" == superName && name != "com/xander/performance/PerformanceHandler" ) {
      println("PerformanceClassVisitor superName:$superName")
      println("PerformanceClassVisitor name:$name")
      superClassName = "com/xander/performance/PerformanceHandler"
    }
    super.visit(version, access, name, signature, superClassName, interfaces)
  }

  override fun visitMethod(access: Int, methodName: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
    return super.visitMethod(access, methodName, descriptor, signature, exceptions)
  }
}
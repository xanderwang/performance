package com.xander.performance.plugin

import com.xander.plugin.asm.lib.BaseClassVisitor
import com.xander.plugin.asm.lib.BaseWeaverFactory
import com.xander.plugin.asm.lib.IWeaverFactory
import org.objectweb.asm.*
import org.objectweb.asm.util.Printer
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter

/**
 *
 * @Description //TODO
 *
 * @author Xander Wang
 * Created on 2020/11/27.
 */

class PerformanceWeaver : BaseWeaverFactory() {

  var traceClass = false

  var traceMethod = false

  override fun createClassVisitor(classVisitor: ClassVisitor): ClassVisitor {
    var classVisitor = classVisitor
    if (traceClass) {
      classVisitor = TraceClassVisitor(classVisitor, PrintWriter(System.out))
    }
    return PerformanceClassVisitor(classVisitor, this)
  }

  override fun createMethodVisitor(methodName: String, access: Int, desc: String?, mv: MethodVisitor): MethodVisitor {
    if (traceMethod) {
      val p = Textifier()
      val traceMethodVisitor = TraceMethodVisitor(mv, p)
      return MyTraceMethod(p, traceMethodVisitor)
    }
    return PerformanceMethodVisitor(methodName, access, desc, mv)
  }
}

class PerformanceClassVisitor(cv: ClassVisitor, iWeaverFactory: IWeaverFactory) : BaseClassVisitor(cv, iWeaverFactory) {

  override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
    // println("PerformanceClassVisitor name:$name")
    var superName = superName
    if ("android/os/Handler" == superName && name != "com/xander/performance/PerformanceHandler") {
      println("PerformanceClassVisitor name:$name superName:$superName")
      superName = "com/xander/performance/PerformanceHandler"
    }
    super.visit(version, access, name, signature, superName, interfaces)
  }

  // override fun visitField(access: Int, name: String?, descriptor: String?, signature: String?, value: Any?): FieldVisitor {
  //   var descriptor = descriptor
  //   if (descriptor == "Landroid/os/Handler;") {
  //     println("PerformanceClassVisitor visitField access:$access, name:$name, descriptor:$descriptor, signature:$signature, value:$value")
  //     // descriptor = "Lcom/xander/performance/PerformanceHandler;"
  //   }
  //   return super.visitField(access, name, descriptor, signature, value)
  // }

  override fun visitMethod(access: Int, methodName: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
    var descriptor = descriptor
    // if (descriptor == "()Landroid/os/Handler;") {
    //   // descriptor = "()Lcom/xander/performance/PerformanceHandler;"
    // }
    // println("PerformanceClassVisitor visitMethod access:$access, methodName:$methodName, descriptor:$descriptor, signature:$signature, exceptions:$exceptions")
    return super.visitMethod(access, methodName, descriptor, signature, exceptions)
  }
}

class PerformanceMethodVisitor(methodName: String?, access: Int, descriptor: String?, methodVisitor: MethodVisitor)
  : MethodVisitor(Opcodes.ASM9, methodVisitor) {

  // override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
  //   var descriptor = descriptor
  //   if (descriptor == "()Landroid/os/Handler;") {
  //     println("================================================================================")
  //     // descriptor = "()Lcom/xander/performance/PerformanceHandler;"
  //     println("visitMethodInsn opcode:${Printer.OPCODES[opcode]},owner:$owner,name:$name,descriptor:$descriptor,isInterface:$isInterface")
  //   }
  //   super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
  // }

  // override fun visitInvokeDynamicInsn(name: String?, descriptor: String?, bootstrapMethodHandle: Handle?, vararg bootstrapMethodArguments: Any?) {
  //   super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
  //   println("visitInvokeDynamicInsn name:${name},descriptor:$descriptor,bootstrapMethodHandle:$bootstrapMethodHandle,bootstrapMethodArguments:$bootstrapMethodArguments")
  // }

  override fun visitTypeInsn(opcode: Int, type: String?) {
    var type = type
    if(Printer.OPCODES[opcode] == "NEW" && type == "android/os/Handler" ) {
      type = "com/xander/performance/PerformanceHandler"
      println("visitTypeInsn opcode:${Printer.OPCODES[opcode]},type:$type")
    }
    super.visitTypeInsn(opcode, type)
  }

  // override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
  //   super.visitFieldInsn(opcode, owner, name, descriptor)
  //   println("visitFieldInsn opcode:${Printer.OPCODES[opcode]},owner:$owner,name:$name,descriptor:$descriptor")
  // }

  // override fun visitInsn(opcode: Int) {
  //   super.visitInsn(opcode)
  //   println("visitInsn opcode:${Printer.OPCODES[opcode]}")
  // }

  // override fun visitVarInsn(opcode: Int, `var`: Int) {
  //   super.visitVarInsn(opcode, `var`)
  //   println("visitVarInsn opcode:${Printer.OPCODES[opcode]},var:${`var`}")
  // }

  // override fun visitLabel(label: Label?) {
  //   super.visitLabel(label)
  //   println("visitLabel Label:${label?.info}")
  // }

}

class MyTraceMethod(val p: Printer, mv: MethodVisitor) : MethodVisitor(Opcodes.ASM9, mv) {

  override fun visitCode() {
    super.visitCode()
    println("MyTraceMethod visitCode")
  }

  override fun visitEnd() {
    super.visitEnd()
    p.getText().forEach {
      println(it)
    }
    println("MyTraceMethod visitEnd ======================")
  }

}
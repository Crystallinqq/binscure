package dev.binclub.binscure.utils

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

/**
 * @author cookiedragon234 24/Feb/2020
 */
object EmptyClassVisitor : ClassVisitor(Opcodes.ASM7)

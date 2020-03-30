package dev.binclub.binscure.processors.flow

import dev.binclub.binscure.CObfuscator.isExcluded
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.kotlin.add
import dev.binclub.binscure.runtime.OpaqueRuntimeManager
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.genericType
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 13/Mar/2020
 */
object ObscurePop: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				
				for (insn in method.instructions) {
					if (
						(insn.opcode == POP || insn.opcode == POP2)
						&&
						(
							insn.previous is MethodInsnNode && (insn.previous as MethodInsnNode).doesReturnVal()
							||
							insn.previous is FieldInsnNode && (insn.previous as FieldInsnNode).doesReturnVal()
							||
							insn.previous.opcode == CHECKCAST
						)
					) {
						val desc = genericType(
							when {
								insn.previous is MethodInsnNode -> Type.getReturnType((insn.previous as MethodInsnNode).desc)
								insn.previous is FieldInsnNode -> Type.getType((insn.previous as FieldInsnNode).desc)
								insn.previous.opcode == CHECKCAST -> Type.getType((insn.previous as TypeInsnNode).desc)
								else -> error("Bad type")
							}
						).descriptor
						
						/*modifier.replace(insn, InsnList().apply {
							add(MethodInsnNode(
								INVOKESTATIC,
								OpaqueRuntimeManager.classNode.name,
								OpaqueRuntimeManager.consumeMethodName,
								"($desc)V",
								false
							))
							
							if (insn.opcode == POP2) {
								add(POP)
							}
						})*/
						
						if (insn.previous.opcode == CHECKCAST) {
							modifier.remove(insn.previous)
						}
					}
				}
				
				modifier.apply(method)
			}
		}
	}
	
	private fun MethodInsnNode.doesReturnVal(): Boolean = this.desc.endsWith(")V").not()
	
	private fun FieldInsnNode.doesReturnVal(): Boolean = this.opcode == GETSTATIC || this.opcode == GETFIELD
}

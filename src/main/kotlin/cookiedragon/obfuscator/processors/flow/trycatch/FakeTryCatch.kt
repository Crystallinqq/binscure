package cookiedragon.obfuscator.processors.flow.trycatch

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.random
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 27/Jan/2020
 */
object FakeTryCatch: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Merging Try Catch Blocks").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (method.tryCatchBlocks.size > 0) {// && CObfuscator.randomWeight(2)) {
					val tryCatch = method.tryCatchBlocks.random(CObfuscator.random)
					println("Selected $tryCatch in ${classNode.name}")
					
					if (method.localVariables == null)
						method.localVariables = ArrayList(1)
					
					val lastVar = method.localVariables.stream().max(Comparator.comparingInt{v -> v.index}).orElse(null)
					val newIndex = if (lastVar != null) {
						if (lastVar.desc.matches(Regex("[JD]"))) {
							lastVar.index + 2
						} else {
							lastVar.index + 1
						}
					} else {
						0
					}
					
					val localVar = LocalVariableNode(
						"d",
						Type.getDescriptor(Int::class.javaPrimitiveType),
						null,
						tryCatch.start,
						tryCatch.end,
						newIndex
					).also { method.localVariables.add(it) }
					method.maxLocals++
					
					for (localVariable in method.localVariables) {
						println("\t${localVariable.name} : ${localVariable.desc} : ${localVariable.index}")
					}
					
					val startList = InsnList()
						.also {
							it.add(VarInsnNode(Type.getType(localVar.desc).getOpcode(ILOAD), localVar.index))
							it.add(ldcInt(1))
							it.add(JumpInsnNode(IF_ICMPGT, tryCatch.handler))
							it.add(ldcInt(1))
							it.add(VarInsnNode(Type.getType(localVar.desc).getOpcode(ISTORE), localVar.index))
						}
					
					val afterList = InsnList()
						.also {
							it.add(ldcInt(0))
							it.add(VarInsnNode(Type.getType(localVar.desc).getOpcode(ISTORE), localVar.index))
						}
					
					method.instructions.insert(
						tryCatch.start,
						startList
					)
					method.instructions.insertBefore(
						tryCatch.handler,
						afterList
					)
				}
			}
		}
	}
}

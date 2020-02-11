package cookiedragon.obfuscator.processors

import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.utils.InstructionModifier
import org.objectweb.asm.Opcodes.CHECKCAST
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList

/**
 * @author cookiedragon234 10/Feb/2020
 */
object RemoveCheckCast: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			for (method in classNode.methods) {
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (insn.opcode == CHECKCAST) {
						val list = InsnList().apply {
							//add(InsnNode(ACONST_NULL))
						}
						modifier.replace(insn, list)
					}
				}
				modifier.apply(method)
			}
		}
	}
	
}

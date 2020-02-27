package cookiedragon.obfuscator.processors.flow

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.runtime.opaqueSwitchJump
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.newLabel
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 27/Feb/2020
 */
object CfgFucker: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes.toTypedArray()) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				val endings = hashSetOf<InsnList>()
				for (insn in method.instructions) {
					if (
						(insn is MethodInsnNode || insn is FieldInsnNode || insn is VarInsnNode)
						&&
						insn.next != null
						&&
						random.nextBoolean()
					) {
						val list = opaqueSwitchJump()
						modifier.prepend(insn, list)
						
						if (insn.next == null) {
							for (ending in endings) {
								modifier.append(insn, ending)
							}
						}
					}
				}
				modifier.apply(method)
			}
		}
	}
}

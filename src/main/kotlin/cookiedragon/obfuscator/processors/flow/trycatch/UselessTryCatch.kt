package cookiedragon.obfuscator.processors.flow.trycatch

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.randomThrowable
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.ATHROW
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 31/Jan/2020
 */
object UselessTryCatch: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		
		for (classNode in CObfuscator.getProgressBar("Adding useless try catches").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				val encounteredLabels = arrayListOf<LabelNode>()
				for (insn in method.instructions) {
					//if (insn is LabelNode) {
					//	encounteredLabels.add(insn)
					//}
					
					if (insn is MethodInsnNode) {
						val beforeLabel = LabelNode(Label())
						val afterLabel = LabelNode(Label())
						val handler = LabelNode(Label())
						val end = LabelNode(Label())
						
						val before = InsnList().apply {
							add(beforeLabel)
						}
						
						val list = InsnList().apply {
							add(afterLabel)
							add(JumpInsnNode(GOTO, end))
							add(handler)
							//add(InsnNode(DUP))
							//add(JumpInsnNode(IFNULL, beforeLabel))
							add(InsnNode(ATHROW))
							add(end)
						}
						
						method.tryCatchBlocks.add(TryCatchBlockNode(beforeLabel, afterLabel, handler, randomThrowable()))
						
						modifier.prepend(insn, before)
						modifier.append(insn, list)
					}
				}
				modifier.apply(method)
			}
		}
	}
}

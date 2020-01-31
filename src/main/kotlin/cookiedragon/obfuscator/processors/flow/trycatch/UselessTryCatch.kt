package cookiedragon.obfuscator.processors.flow.trycatch

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
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
				
				var isInitialised = !method.name.startsWith("<")
				
				var handler: LabelNode? = null
				
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					//if (insn is LabelNode) {
					//	encounteredLabels.add(insn)
					//}
					
					if (insn is MethodInsnNode) {
						if (insn.opcode == INVOKESPECIAL && !isInitialised) {
							isInitialised = true
							continue
						}
						
						if (!isInitialised)
							continue
						
						if (handler == null) {
							handler = LabelNode(Label())
							val handler1 = LabelNode(Label())
							val handlerEnd = LabelNode(Label())
							
							val handlerList = InsnList().apply {
								add(JumpInsnNode(GOTO, handlerEnd))
								add(handler)
								add(InsnNode(DUP))
								add(JumpInsnNode(IFNULL, handler1))
								add(InsnNode(ATHROW))
								add(handler1)
								add(InsnNode(ACONST_NULL))
								add(InsnNode(ATHROW))
								add(handlerEnd)
							}
							
							modifier.prepend(method.instructions.first, handlerList)
						}
						
						val beforeLabel = LabelNode(Label())
						val afterLabel = LabelNode(Label())
						val nullThrow = LabelNode(Label())
						val end = LabelNode(Label())
						
						val before = InsnList().apply {
							//add(nullThrow)
							//add(InsnNode(ACONST_NULL))
							//add(InsnNode(ATHROW))
							//add(JumpInsnNode(GOTO, beforeLabel))
							//add(handler)
							//add(JumpInsnNode(IFNULL, handler))
							add(beforeLabel)
						}
						
						val list = InsnList().apply {
							add(afterLabel)
							//add(end)
						}
						
						method.tryCatchBlocks.add(TryCatchBlockNode(beforeLabel, afterLabel, handler, null))
						
						modifier.prepend(insn, before)
						modifier.append(insn, list)
					}
				}
				modifier.apply(method)
			}
		}
	}
}

package cookiedragon.obfuscator.processors.flow.trycatch

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.random
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.randomThrowable
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
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
					continue
				
				var isInitialised = !method.name.startsWith("<")
				
				var handler: LabelNode? = null
				val handlerEnd = LabelNode(Label())
				val endHandler = LabelNode(Label())
				val endHandlerEnd = LabelNode(Label())
				
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
							
							val handlerList = InsnList().apply {
								add(JumpInsnNode(GOTO, handlerEnd))
								add(handler)
								add(InsnNode(DUP))
								add(JumpInsnNode(IFNULL, endHandler))
								add(InsnNode(ATHROW))
								add(handlerEnd)
							}
							
							val endHandleList = InsnList().apply {
								add(endHandler)
								add(InsnNode(ACONST_NULL))
								add(InsnNode(ATHROW))
								add(endHandlerEnd)
							}
							
							modifier.prepend(method.instructions.first, handlerList)
							modifier.append(method.instructions.last, endHandleList)
							method.tryCatchBlocks.add(TryCatchBlockNode(endHandler, endHandlerEnd, handler, randomThrowable()))
						}
						
						val beforeLabel2 = LabelNode(Label())
						val beforeLabel3 = LabelNode(Label())
						val afterLabel = LabelNode(Label())
						val afterLabel2 = LabelNode(Label())
						val afterLabel3 = LabelNode(Label())
						
						val before = InsnList().apply {
							add(JumpInsnNode(GOTO, beforeLabel2))
							add(beforeLabel3)
							add(InsnNode(ATHROW))
							add(beforeLabel2)
						}
						
						val list = InsnList().apply {
							add(JumpInsnNode(GOTO, afterLabel3))
							add(afterLabel2)
							add(InsnNode(ATHROW))
							add(afterLabel)
							add(afterLabel3)
						}
						
						val availableHandlers = arrayOf(afterLabel2, beforeLabel3, handler)
						val possibleStarts = arrayOf(beforeLabel2, beforeLabel3)
						val possibleEnds = arrayOf(afterLabel, afterLabel3, afterLabel2)
						
						method.tryCatchBlocks.add(TryCatchBlockNode(
							possibleStarts.random(random),
							possibleEnds.random(random),
							availableHandlers.random(random),
							randomThrowable()
						))
						method.tryCatchBlocks.add(TryCatchBlockNode(
							possibleStarts.random(random),
							possibleEnds.random(random),
							availableHandlers.random(random),
							randomThrowable()
						))
						method.tryCatchBlocks.add(TryCatchBlockNode(
							possibleStarts.random(random),
							possibleEnds.random(random),
							availableHandlers.random(random),
							randomThrowable()
						))
						method.tryCatchBlocks.add(TryCatchBlockNode(
							possibleStarts.random(random),
							possibleEnds.random(random),
							availableHandlers.random(random),
							randomThrowable()
						))
						
						modifier.prepend(insn, before)
						modifier.append(insn, list)
					}
				}
				modifier.apply(method)
			}
		}
	}
}

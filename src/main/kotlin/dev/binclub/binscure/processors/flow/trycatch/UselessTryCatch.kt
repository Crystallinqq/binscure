package dev.binclub.binscure.processors.flow.trycatch

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.kotlin.random
import dev.binclub.binscure.utils.BlameableLabelNode
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.newLabel
import dev.binclub.binscure.utils.randomThrowable
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 31/Jan/2020
 */
object UselessTryCatch: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!ConfigurationManager.rootConfig.flowObfuscation.enabled) {
			return
		}
		
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
					continue
				
				method.tryCatchBlocks = method.tryCatchBlocks ?: arrayListOf()
				
				var isInitialised = !method.name.startsWith("<")
				
				var handler: LabelNode? = null
				val handlerEnd = newLabel()
				val endHandler = newLabel()
				val endHandlerEnd = newLabel()
				
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
							handler = newLabel()
							
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
						
						val beforeLabel2 = newLabel()
						val beforeLabel3 = newLabel()
						val afterLabel = newLabel()
						val afterLabel2 = newLabel()
						val afterLabel3 = newLabel()
						
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

package dev.binclub.binscure.processors.flow.trycatch

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.kotlin.add
import dev.binclub.binscure.kotlin.wrap
import dev.binclub.binscure.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.BlameableLabelNode
import dev.binclub.binscure.utils.randomThrowable
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 27/Jan/2020
 */
object FakeTryCatch: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Adding try catches").wrap(classes)) {
			if (!CObfuscator.isExcluded(classNode)) {
				for (method in classNode.methods) {
					if (CObfuscator.noMethodInsns(method))// || !CObfuscator.randomWeight(5))
						continue
					
					addFakeTryCatches(method)
				}
			}
		}
	}
	
	fun addFakeTryCatches(methodNode: MethodNode) {
		methodNode.tryCatchBlocks.addAll(
			addFakeTryCatches(methodNode.instructions)
		)
	}
	
	fun addFakeTryCatches(insnList: InsnList): Array<TryCatchBlockNode> {
		val switchStart = BlameableLabelNode()
		val fakeEnd = BlameableLabelNode()
		val start = BlameableLabelNode()
		val handler = BlameableLabelNode()
		val end = BlameableLabelNode()
		val secondCatch = BlameableLabelNode()
		val dead = BlameableLabelNode()
		val dead2 = BlameableLabelNode()
		
		val list = InsnList()
			.apply {
				add(switchStart)
				add(start)
				add(InsnNode(ACONST_NULL))
				add(randomOpaqueJump(secondCatch, false))
				add(InsnNode(POP))
				add(InsnNode(ACONST_NULL))
				if (ConfigurationManager.rootConfig.crasher.enabled) {
					add(TypeInsnNode(CHECKCAST, "give up"))
				}
				add(JumpInsnNode(GOTO, handler))
				add(fakeEnd)
				add(InsnNode(ATHROW))
				add(end)
			}
		
		val endList = InsnList()
			.apply {
				add(dead)
				add(POP)
				add(JumpInsnNode(GOTO, end))
				add(dead2)
				add(POP)
				add(ACONST_NULL)
				add(JumpInsnNode(GOTO, dead))
				add(handler)
				add(DUP)
				add(JumpInsnNode(IFNULL, dead))
				add(InsnNode(ATHROW))
				add(secondCatch)
				add(DUP)
				add(JumpInsnNode(IFNULL, dead2))
				add(InsnNode(ATHROW))
			}
		
		insnList.insert(list)
		insnList.add(endList)
		return arrayOf(
			TryCatchBlockNode(start, end, handler, randomThrowable()),
			TryCatchBlockNode(fakeEnd, end, secondCatch, randomThrowable()),
			TryCatchBlockNode(handler, secondCatch, handler, randomThrowable()),
			TryCatchBlockNode(start, fakeEnd, secondCatch, randomThrowable())
		
		)
	}
}

package cookiedragon.obfuscator.processors.flow.trycatch

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.configuration.ConfigurationManager
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.runtime.randomOpaqueJump
import cookiedragon.obfuscator.utils.BlameableLabelNode
import cookiedragon.obfuscator.utils.randomThrowable
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
		
		val list = InsnList()
			.apply {
				add(switchStart)
				add(start)
				add(InsnNode(ACONST_NULL))
				add(randomOpaqueJump(secondCatch))
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
				add(handler)
				add(InsnNode(POP))
				add(InsnNode(ACONST_NULL))
				add(JumpInsnNode(GOTO, fakeEnd))
				add(secondCatch)
				add(JumpInsnNode(IFNULL, end))
				add(InsnNode(ACONST_NULL))
				add(JumpInsnNode(GOTO, fakeEnd))
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

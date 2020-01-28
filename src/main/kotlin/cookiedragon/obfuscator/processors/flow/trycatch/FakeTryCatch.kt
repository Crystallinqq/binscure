package cookiedragon.obfuscator.processors.flow.trycatch

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.internalName
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 27/Jan/2020
 */
object FakeTryCatch: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (!CObfuscator.isExcluded(classNode) && classNode.methods.size > 0) {
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
		val switchStart = LabelNode(Label())
		val fakeEnd = LabelNode(Label())
		val start = LabelNode(Label())
		val handler = LabelNode(Label())
		val end = LabelNode(Label())
		val secondCatch = LabelNode(Label())
		
		val list = InsnList()
			.apply {
				add(switchStart)
				add(start)
				add(InsnNode(ACONST_NULL))
				add(MethodInsnNode(INVOKESTATIC, Runtime::class.internalName, "getRuntime", "()Ljava/lang/Runtime;"))
				add(JumpInsnNode(IFNONNULL, secondCatch))
				add(InsnNode(ACONST_NULL))
				add(InsnNode(DUP))
				add(TypeInsnNode(CHECKCAST, "give up"))
				add(MethodInsnNode(INVOKESTATIC, "null", "super", "()Ljava/lang/YourMum;"))
				add(InsnNode(POP2))
				add(InsnNode(POP))
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
			TryCatchBlockNode(start, end, handler, RuntimeException::class.internalName),
			TryCatchBlockNode(fakeEnd, end, secondCatch, Error::class.internalName),
			TryCatchBlockNode(handler, secondCatch, handler, StringIndexOutOfBoundsException::class.internalName),
			TryCatchBlockNode(start, fakeEnd, secondCatch, StackOverflowError::class.internalName)
		
		)
	}
}

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
					
					val fakeEnd = LabelNode(Label())
					val start = LabelNode(Label())
					val handler = LabelNode(Label())
					val end = LabelNode(Label())
					val secondCatch = LabelNode(Label())
					
					method.tryCatchBlocks.add(TryCatchBlockNode(start, end, handler, null))
					method.tryCatchBlocks.add(TryCatchBlockNode(fakeEnd, end, secondCatch, null))
					
					val list = InsnList()
						.also {
							it.add(start)
							it.add(InsnNode(ACONST_NULL))
							it.add(MethodInsnNode(INVOKESTATIC, System::class.internalName, "currentTimeMillis", "()J", false))
							it.add(InsnNode(L2I))
							it.add(InsnNode(INEG))
							it.add(JumpInsnNode(IFGE, secondCatch))
							it.add(InsnNode(POP))
							it.add(InsnNode(ACONST_NULL))
							it.add(JumpInsnNode(GOTO, handler))
							it.add(fakeEnd)
							it.add(InsnNode(ATHROW))
							it.add(secondCatch)
							it.add(InsnNode(POP))
							it.add(end)
						}
					
					val endList = InsnList()
						.also {
							it.add(handler)
							it.add(InsnNode(POP))
							it.add(InsnNode(ACONST_NULL))
							it.add(JumpInsnNode(GOTO, fakeEnd))
						}
					
					method.instructions.insert(list)
					method.instructions.add(endList)
				}
			}
		}
	}
	
}

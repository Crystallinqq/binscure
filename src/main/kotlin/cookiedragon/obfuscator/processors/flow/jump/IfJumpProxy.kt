package cookiedragon.obfuscator.processors.flow.jump

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 29/Jan/2020
 */
object IfJumpProxy: IClassProcessor {
	val heavy = false
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Proxying if jumps").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (insn is JumpInsnNode) {
						val tryStart = LabelNode(Label())
						val tryEnd = LabelNode(Label())
						val catch = LabelNode(Label())
						val after = LabelNode(Label())
						
						val list = InsnList().apply {
							add(tryStart)
							if (heavy) {
								add(MethodInsnNode(INVOKESTATIC, Thread::class.internalName, "currentThread", "()Ljava/lang/Thread;"))
								add(InsnNode(POP))
							}
							add(JumpInsnNode(insn.opcode, insn.label))
							//add(JumpInsnNode(GOTO, after))
							add(InsnNode(ACONST_NULL))
							add(tryEnd)
							add(catch)
							add(InsnNode(POP))
							add(JumpInsnNode(GOTO, after))
							//add(InsnNode(ATHROW))
						}
						
						insn.label = tryStart
						insn.opcode = GOTO
						
						modifier.append(insn, InsnList().apply{add(after)})
						modifier.append(method.instructions.last, list)
						method.tryCatchBlocks.add(TryCatchBlockNode(tryStart, tryEnd, catch, null))
						
						println("Added to ${method.name}")
						break
					}
				}
				modifier.apply(method)
			}
		}
	}
}

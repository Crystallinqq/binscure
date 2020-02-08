package cookiedragon.obfuscator.processors.flow

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.InstructionModifier
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 28/Jan/2020
 */
object BadInvoke: IClassProcessor {
	val badText = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Adding invalid invocations").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			var field: FieldNode? = null
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
					continue
				
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (insn is JumpInsnNode){// && CObfuscator.randomWeight(5)) {
						if (field == null) {
							field = FieldNode(ACC_PUBLIC + ACC_STATIC, "\$badInvoke", Type.getDescriptor(String::class.java), null, null)
						}
						
						val end = LabelNode(Label())
						
						val list = InsnList()
							.apply {
								add(FieldInsnNode(GETSTATIC, classNode.name, field.name, field.desc))
								add(JumpInsnNode(Opcodes.IFNULL, end))
								add(MethodInsnNode(INVOKESTATIC, "java/lang/Object", badText, "()V"))
								add(end)
							}
						modifier.prepend(insn, list)
						println("Added to ${classNode.name}.${method.name}")
						break
					}
				}
				modifier.apply(method)
			}
			if (field != null) {
				classNode.fields.add(field)
			}
		}
	}
}

package cookiedragon.obfuscator.processors.indirection

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.configuration.ConfigurationManager
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.kotlin.xor
import cookiedragon.obfuscator.processors.renaming.impl.ClassRenamer
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 22/Jan/2020
 */
object DynamicCallObfuscation: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!ConfigurationManager.rootConfig.indirection.enabled)
			return
		
		val methodCalls = mutableSetOf<MethodCall>()
		for (classNode in CObfuscator.getProgressBar("Indirecting method calls").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				for (insn in method.instructions) {
					if (insn is MethodInsnNode && insn.opcode != INVOKESPECIAL && insn.opcode != INVOKEDYNAMIC) {
						if (insn.name.startsWith("<"))
							continue
						
						if (CObfuscator.isExcluded("${insn.owner}.${insn.name}${insn.desc}"))
							continue
						
						methodCalls.add(MethodCall(classNode, method, insn))
					}
				}
			}
		}
		
		if (!methodCalls.isEmpty()) {
			val decryptNode = ClassNode().apply {
				access = ACC_PUBLIC + ACC_FINAL
				version = methodCalls.first().classNode.version
				name = ClassRenamer.namer.uniqueRandomString()
				signature = null
				superName = "java/lang/Object"
				classes.add(this)
				ClassPath.classes[this.name] = this
				ClassPath.classPath[this.name] = this
			}
			
			val bootStrapMethod = MethodNode(
				ACC_PUBLIC + ACC_FINAL,
				"\u0000",
				"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
				null,
				null
			).apply {
				generateBootstrapMethod(this)
				decryptNode.methods.add(this)
			}
			
			val handler = Handle(H_INVOKESTATIC, decryptNode.name, bootStrapMethod.name, bootStrapMethod.desc, false)
			
			for (methodCall in methodCalls) {
				val desc = if (methodCall.insnNode.opcode == INVOKESTATIC) {
					methodCall.insnNode.desc
				} else {
					methodCall.insnNode.desc.replaceFirst("(", "(Ljava/lang/Object;")
				}
				
				methodCall.methodNode.instructions.set(
					methodCall.insnNode,
					InvokeDynamicInsnNode(
						encryptName(methodCall.classNode.name, methodCall.methodNode.name, methodCall.insnNode),
						desc,
						handler
					)
				)
			}
		}
	}
	
	private fun encryptName(className: String, methodName: String, methodNode: MethodInsnNode): String {
		val classHash = className.replace('/', '.').hashCode()
		val methodHash = methodName.replace('/', '.').hashCode()
		
		var originalStr = "${methodNode.owner}.${methodNode.name}.${methodNode.desc}"
		if (methodNode.opcode == INVOKESTATIC) {
			originalStr += "s"
		} else {
			originalStr += "v"
		}
		val original = originalStr.toCharArray()
		val new = CharArray(original.size)
		
		for (i in 0 until original.size) {
			val char = original[i]
			new[i] = when (i % 5) {
				0 -> char xor 2
				1 -> char xor classHash
				2 -> char xor methodHash
				3 -> char xor (classHash + methodHash)
				4 -> char xor i
				else -> throw IllegalStateException("Illegal ${i % 6}")
			}
		}
		return String(new)
	}
	
	private fun generateBootstrapMethod(methodNode: MethodNode) {
		methodNode.instructions.apply {
			val loopStart = LabelNode(Label())
			val exitLoop = LabelNode(Label())
			val setCharArrVal = LabelNode(Label())
			val throwNull = LabelNode(Label())
			
			val tble0 = LabelNode(Label())
			val tble1 = LabelNode(Label())
			val tble2 = LabelNode(Label())
			val tble3 = LabelNode(Label())
			val tble4 = LabelNode(Label())
			
			// First we need to decrypt the method description stored at local var 1
			// We will turn it into a char array
			add(VarInsnNode(ALOAD, 1))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false))
			add(InsnNode(DUP))
			// Store it in local var 3
			add(VarInsnNode(ASTORE, 3))
			// Find the array length and create our decrypted char array (store in slot 4)
			add(InsnNode(ARRAYLENGTH))
			add(IntInsnNode(NEWARRAY, T_CHAR))
			add(VarInsnNode(ASTORE, 4))
			
			// Now loop over our new array
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 5))
			add(loopStart)
			add(VarInsnNode(ILOAD, 5)) // index stored at slot 5
			add(VarInsnNode(ALOAD, 4))
			add(InsnNode(ARRAYLENGTH))
			add(JumpInsnNode(IF_ICMPGE, exitLoop)) // If at the end of the loop go to exit
			
			
			add(VarInsnNode(ILOAD, 5))
			add(ldcInt(5))
			add(InsnNode(IREM))
			add(TableSwitchInsnNode(
				0,
				4,
				throwNull,
				tble0, tble1, tble2, tble3, tble4
			))
			
			
			add(tble0)
			add(VarInsnNode(ALOAD, 4)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 5)) // index
			add(InsnNode(CALOAD))
			add(ldcInt(2))
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			
			add(setCharArrVal)
			add(InsnNode(I2C))
			add(VarInsnNode(ALOAD, 4)) // Decrypted Char Array
			add(InsnNode(SWAP))
			add(VarInsnNode(ILOAD, 5)) // Index
			add(InsnNode(SWAP))
			add(InsnNode(CASTORE))
			// Increment and go to top of loop
			add(IincInsnNode(5, 1))
			add(JumpInsnNode(GOTO, loopStart))
			
			
			// If we are here then we have a decrypted char array in slot 4
			add(exitLoop)
			
			
			
			add(throwNull)
			add(InsnNode(ACONST_NULL))
			add(InsnNode(ATHROW))
		}
	}
	
	data class MethodCall(val classNode: ClassNode, val methodNode: MethodNode, val insnNode: MethodInsnNode)
}

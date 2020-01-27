package cookiedragon.obfuscator.processors.string

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.configuration.ConfigurationManager
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.kotlin.xor
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import kotlin.random.Random

/**
 * @author cookiedragon234 20/Jan/2020
 */
object StringObfuscator: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!ConfigurationManager.rootConfig.stringObfuscation.enabled)
			return
		
		for (classNode in CObfuscator.getProgressBar("Obfuscating Strings").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			val stringInsns = arrayListOf<EncryptedString>()
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				for (insn in method.instructions) {
					if (insn is LdcInsnNode && insn.cst is String) {
						
						val encryptedString = encryptString(
							insn.cst as String,
							Math.round(CObfuscator.random.nextFloat() * 100),
							classNode,
							method,
							insn
						)
						stringInsns.add(encryptedString)
					}
				}
			}
			
			if (stringInsns.size > 0) {
				
				val storageField = FieldNode(
					ACC_PRIVATE + ACC_STATIC + ACC_FINAL,
					"decryptionStorage${CObfuscator.random.nextInt(5000)}",
					"[Ljava/lang/String;",
					null,
					null
				)
				classNode.fields.add(storageField)
				
				generateStaticBlock(classNode, storageField, stringInsns)
				
				val decryptorMethod = generateDecrypterMethod(classNode, storageField, stringInsns)
				
				for ((index, string) in stringInsns.withIndex()) {
					
					val modifier = InstructionModifier()
					
					val list = InsnList()
					
					if (CObfuscator.random.nextInt(21) == 1) {
						list.add(ldcInt(string.key))
						list.add(ldcInt(index))
						list.add(InsnNode(SWAP))
					} else {
						list.add(ldcInt(index))
						list.add(ldcInt(string.key))
					}
					when (CObfuscator.random.nextInt(40)) {
						1 -> {
							list.add(InsnNode(ICONST_M1))
							list.add(InsnNode(POP))
						}
						2 -> {
							list.add(LdcInsnNode(classNode.name))
							list.add(InsnNode(POP))
						}
						3 -> {
							list.add(InsnNode(ACONST_NULL))
							list.add(InsnNode(POP))
						}
						4 -> {
							list.add(InsnNode(NOP))
						}
					}
					list.add(MethodInsnNode(
						INVOKESTATIC,
						classNode.name,
						decryptorMethod.name,
						decryptorMethod.desc,
						false
					))
					
					modifier.replace(string.insn, list)
					modifier.apply(string.methodNode)
				}
			}
		}
	}
	
	private fun generateDecrypterMethod(classNode: ClassNode, storageField: FieldNode, strings: ArrayList<EncryptedString>): MethodNode {
		val decryptorMethod = MethodNode(
			ACC_PRIVATE + ACC_STATIC,
			"stringDecrypter${CObfuscator.random.nextInt(5000)}",
			"(II)Ljava/lang/String;",
			null,
			null
		)
		decryptorMethod.exceptions.add("java/lang/Math")
		
		// First check if the value is cached
		decryptorMethod.instructions.also {
			it.add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc))
			it.add(IntInsnNode(ILOAD, 0))
			// Multiply by 2
			it.add(ldcInt(2))
			it.add(InsnNode(IMUL))
			// Add 1
			it.add(ldcInt(1))
			it.add(InsnNode(IADD))
			it.add(InsnNode(AALOAD))
			it.add(InsnNode(DUP))
			// Return if not null
			val afterRet = LabelNode(Label())
			it.add(JumpInsnNode(IFNULL, afterRet))
			it.add(InsnNode(ARETURN))
			it.add(afterRet)
			
			it.add(LdcInsnNode(""))
			it.add(InsnNode(ARETURN))
			
			val fakeEnd = LabelNode(Label())
			val start = LabelNode(Label())
			val handler = LabelNode(Label())
			val end = LabelNode(Label())
			val secondCatch = LabelNode(Label())
			
			val threadOther = LabelNode(Label())
			val firstSwitch = LabelNode(Label())
			
			it.add(ldcInt(0))
			it.add(firstSwitch)
			//it.add(LookupSwitchInsnNode(
			//	afterRet,
			//	intArrayOf( 0, 1),
			//	arrayOf(start, threadOther)
			//))
			it.add(InsnNode(DUP))
			it.add(ldcInt(0))
			it.add(JumpInsnNode(IFEQ, start))
			it.add(ldcInt(1))
			it.add(JumpInsnNode(IFEQ, threadOther))
			
			// Get Stack Trace
			it.add(MethodInsnNode(INVOKESTATIC, Thread::class.internalName, "currentThread", "()Ljava/lang/Thread;", false))
			it.add(VarInsnNode(ASTORE, 3))
			it.add(ldcInt(1))
			it.add(JumpInsnNode(GOTO, firstSwitch))
			it.add(threadOther)
			it.add(VarInsnNode(ALOAD, 3))
			it.add(MethodInsnNode(INVOKEVIRTUAL, Thread::class.internalName, "getStackTrace", "()[Ljava/lang/StackTraceElement;", false))
			it.add(InsnNode(POP))
			it.add(LdcInsnNode(""))
			it.add(InsnNode(ARETURN))
			
			// Fake try catch
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
			
			// Center
			it.add(LdcInsnNode(""))
			it.add(InsnNode(ARETURN))
			
			it.add(handler)
			it.add(InsnNode(POP))
			it.add(InsnNode(ACONST_NULL))
			it.add(JumpInsnNode(GOTO, fakeEnd))
		}
			classNode.methods.add(decryptorMethod)
			return decryptorMethod
	}
	
	private fun generateStaticBlock(classNode: ClassNode, storageField: FieldNode, strings: ArrayList<EncryptedString>): MethodNode {var staticInit: MethodNode? = null
		for (method in classNode.methods) {
			if (method.name == "<clinit>") {
				staticInit = method
			}
		}
		if (staticInit == null) {
			staticInit = MethodNode(ACC_STATIC, "<clinit>", "()V", null, null)
			staticInit.instructions.add(InsnNode(RETURN))
			classNode.methods.add(staticInit)
		}
		
		val list = InsnList()
		list.add(ldcInt(strings.size * 2))
		list.add(TypeInsnNode(ANEWARRAY, "java/lang/String"))
		list.add(VarInsnNode(ASTORE, classNode.fields.size - 1))
		
		for ((index, string) in strings.withIndex()) {
			list.add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc))
			if (CObfuscator.random.nextInt(20) == 1) {
				list.add(LdcInsnNode(string.encrypted))
				list.add(ldcInt(index * 2))
				list.add(InsnNode(SWAP))
			} else {
				list.add(ldcInt(index * 2))
				list.add(LdcInsnNode(string.encrypted))
			}
			list.add(InsnNode(AASTORE))
		}
		
		staticInit.instructions.insert(list)
		return staticInit
	}
	
	private data class EncryptedString(
		val original: String,
		val encrypted: String,
		val key: Int,
		val classNode: ClassNode,
		val methodNode: MethodNode,
		val insn: LdcInsnNode
	)
	
	private fun encryptString(original: String, key: Int, classNode: ClassNode, methodNode: MethodNode, insn: LdcInsnNode): EncryptedString {
		val classHash = classNode.name.replace('/', '.').hashCode()
		val methodHash = methodNode.name.replace('/', '.').hashCode()
		
		val old = original.toCharArray()
		val new = CharArray(original.length)
		val rand = Random(key)
		
		for ((index, char) in old.withIndex()) {
			new[index] = when (index % 6) {
				0 -> char xor rand.nextInt(20)
				1 -> char xor key
				2 -> char xor classHash
				3 -> char xor methodHash
				4 -> char xor (methodHash + classHash)
				5 -> char xor index
				else -> throw IllegalStateException("Impossible Value ($index % 6 = ${index % 6})")
			}
		}
		
		return EncryptedString(original, String(new), key, classNode, methodNode, insn)
	}
	
	private fun decryptString(encrypted: String, key: Int) {
		val stackTrace = NullPointerException().stackTrace
		val classHash = stackTrace[1].className.hashCode()
		val methodHash = stackTrace[1].methodName.hashCode()
		
		val old = encrypted.toCharArray()
		val new = CharArray(encrypted.length)
		
		for (i in 0..old.size) {
			if (i % 5 == 0) {
				new[i] = old[i] xor 2
			} else if (i % 5 == 1) {
				new[i] = old[i] xor key
			} else if (i % 5 == 2) {
				new[i] = old[i] xor classHash
			} else if (i % 5 == 3) {
				new[i] = old[i] xor methodHash
			} else if (i % 5 == 4) {
				new[i] = old[i] xor  (methodHash + classHash)
			} else if (i % 5 == 5) {
				new[i] = old[i] xor i
			}
		}
	}
}

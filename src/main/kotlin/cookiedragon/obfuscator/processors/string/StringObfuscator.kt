package cookiedragon.obfuscator.processors.string

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.configuration.ConfigurationManager
import cookiedragon.obfuscator.kotlin.*
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.IllegalStateException
import java.lang.NullPointerException
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
		decryptorMethod.instructions.add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc))
		decryptorMethod.instructions.add(IntInsnNode(ILOAD, 0))
		// Multiply by 2
		decryptorMethod.instructions.add(ldcInt(2))
		decryptorMethod.instructions.add(InsnNode(IMUL))
		// Add 1
		decryptorMethod.instructions.add(ldcInt(1))
		decryptorMethod.instructions.add(InsnNode(IADD))
		decryptorMethod.instructions.add(InsnNode(AALOAD))
		decryptorMethod.instructions.add(InsnNode(DUP))
		// Return if not null
		val afterRet = LabelNode(Label())
		decryptorMethod.instructions.add(JumpInsnNode(IFNULL, afterRet))
		decryptorMethod.instructions.add(InsnNode(ARETURN))
		decryptorMethod.instructions.add(afterRet)
		// Decrypt
		//decryptorMethod.instructions.add(-)
		
		decryptorMethod.instructions.add(LdcInsnNode(""))
		decryptorMethod.instructions.add(InsnNode(ARETURN))
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

package dev.binclub.binscure.processors.constants

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.kotlin.hasAccess
import dev.binclub.binscure.kotlin.internalName
import dev.binclub.binscure.kotlin.wrap
import dev.binclub.binscure.kotlin.xor
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.util.*

/**
 * @author cookiedragon234 20/Jan/2020
 */
object StringObfuscator: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!ConfigurationManager.rootConfig.stringObfuscation.enabled) {
			return
		}
		
		val stringInsns = arrayListOf<EncryptedString>()
		for (classNode in CObfuscator.getProgressBar("Obfuscating Strings").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
					continue
				
				for (insn in method.instructions) {
					if (insn is LdcInsnNode && insn.cst is String) {
						val encryptedString = encryptString(
							insn.cst as String,
							Math.round(random.nextFloat() * 100),
							classNode,
							method,
							insn
						)
						stringInsns.add(encryptedString)
					}
				}
			}
		}
		if (stringInsns.size > 0) {
			val classNode = ClassNode()
				.apply {
					this.access = ACC_PUBLIC + ACC_FINAL
					this.version = stringInsns[0].classNode.version
					this.name = ClassRenamer.namer.uniqueRandomString()
					this.signature = null
					this.superName = "java/lang/Object"
					classes.add(this)
					ClassPath.classes[this.name] = this
					ClassPath.classPath[this.name] = this
				}
			val storageField = FieldNode(
				ACC_PRIVATE + ACC_STATIC + ACC_FINAL,
				"a",
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
				
				if (random.nextInt(21) == 1) {
					list.add(ldcInt(string.key))
					list.add(ldcInt(index))
					list.add(InsnNode(SWAP))
				} else {
					list.add(ldcInt(index))
					list.add(ldcInt(string.key))
				}
				when (random.nextInt(40)) {
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
	
	private fun generateDecrypterMethod(classNode: ClassNode, storageField: FieldNode, strings: ArrayList<EncryptedString>): MethodNode {
		val decryptorMethod = MethodNode(
			ACC_PUBLIC + ACC_STATIC,
			"a",
			"(II)Ljava/lang/String;",
			null,
			null
		)
		
		val realStart = BlameableLabelNode()
		val fakeEnd = BlameableLabelNode()
		val start = BlameableLabelNode()
		val handler = BlameableLabelNode()
		val end = BlameableLabelNode()
		val secondCatch = BlameableLabelNode()
		
		val switch = BlameableLabelNode()
		val switchDefault = BlameableLabelNode()
		
		val genericCatch = BlameableLabelNode()
		val checkCache = BlameableLabelNode()
		val afterRet = BlameableLabelNode()
		val getCurrentThread = BlameableLabelNode()
		val getStackTrace = BlameableLabelNode()
		val getClassName = BlameableLabelNode()
		val getMethodName = BlameableLabelNode()
		val finalReturn = BlameableLabelNode()
		val createCharArrays = BlameableLabelNode()
		val xors = BlameableLabelNode()
		
		// XOR SWITCH LABELS
		val loopStart = BlameableLabelNode()
		val exitLoop = BlameableLabelNode()
		val switchEnd = BlameableLabelNode()
		val setCharArrVal = BlameableLabelNode()
		val l0 = BlameableLabelNode()
		val l1 = BlameableLabelNode()
		val l2 = BlameableLabelNode()
		val l3 = BlameableLabelNode()
		val l4 = BlameableLabelNode()
		val l5 = BlameableLabelNode()
		
		decryptorMethod.tryCatchBlocks.apply {
			add(TryCatchBlockNode(getCurrentThread, finalReturn, genericCatch, "java/lang/Throwable"))
			add(TryCatchBlockNode(getStackTrace, getClassName, genericCatch, null))
			add(TryCatchBlockNode(getMethodName, checkCache, genericCatch, "java/lang/Exception"))
			add(TryCatchBlockNode(start, end, handler, null))
			add(TryCatchBlockNode(fakeEnd, end, secondCatch, null))
			add(TryCatchBlockNode(l3, xors, secondCatch, "java/lang/Throwable"))
			add(TryCatchBlockNode(getCurrentThread, xors, secondCatch, null))
		}
		
		// First check if the value is cached
		val insnList = InsnList().apply {
			add(JumpInsnNode(GOTO, start))
			add(switchDefault)
			add(InsnNode(ACONST_NULL))
			add(TypeInsnNode(CHECKCAST, "java/lang/YourMum"))
			add(InsnNode(POP))
			
			add(l5) // xor i
			add(VarInsnNode(ALOAD, 8)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(loopStart)
			add(VarInsnNode(ILOAD, 10))
			add(VarInsnNode(ALOAD, 8))
			add(InsnNode(ARRAYLENGTH))
			add(JumpInsnNode(IF_ICMPGE, exitLoop))
			add(VarInsnNode(ILOAD, 10))
			add(ldcInt(5))
			add(InsnNode(IREM))
			add(LookupSwitchInsnNode(
				switchEnd,
				intArrayOf(0, 1, 2, 3, 4, 5),
				arrayOf(l0, l1, l2, l3, l4, l5)
			))
			
			// Fake try catch start
			add(start)
			add(InsnNode(ACONST_NULL))
			add(randomOpaqueJump(secondCatch))
			add(InsnNode(POP))
			add(InsnNode(ACONST_NULL))
			add(JumpInsnNode(GOTO, handler))
			add(fakeEnd)
			add(InsnNode(ATHROW))
			add(secondCatch)
			add(InsnNode(POP))
			add(end)
			// Fake try catch start half end
			
			add(JumpInsnNode(GOTO, realStart))
			
			add(getCurrentThread)
			add(MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false))
			add(VarInsnNode(ASTORE, 4))
			add(ldcInt(3))
			add(VarInsnNode(ISTORE, 2))
			add(JumpInsnNode(GOTO, switch)) // GOTO getStackTrace
			
			add(createCharArrays)
			add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc))
			add(IntInsnNode(ILOAD, 0))
			add(ldcInt(2))
			add(InsnNode(IMUL))
			add(InsnNode(AALOAD))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false))
			add(VarInsnNode(ASTORE, 8))
			add(VarInsnNode(ALOAD, 8))
			add(InsnNode(ARRAYLENGTH))
			add(IntInsnNode(NEWARRAY, T_CHAR))
			add(VarInsnNode(ASTORE, 9))
			add(ldcInt(8))
			add(VarInsnNode(ISTORE, 2))
			add(JumpInsnNode(GOTO, switch)) // GOTO xors
			
			add(setCharArrVal)
			add(InsnNode(I2C))
			add(VarInsnNode(ALOAD, 9)) // Decrypted Char Array
			add(InsnNode(SWAP))
			add(VarInsnNode(ILOAD, 10)) // Index
			add(InsnNode(SWAP))
			add(InsnNode(CASTORE))
			// Increment and go to top of loop
			add(IincInsnNode(10, 1))
			add(JumpInsnNode(GOTO, loopStart))
			
			add(exitLoop)
			add(ldcInt(1))
			add(VarInsnNode(ISTORE, 2))
			add(JumpInsnNode(GOTO, switch)) // GOTO finalReturn
			
			add(l3) // xor methodhash
			add(VarInsnNode(ALOAD, 8)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 7)) // methodhash
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(xors)
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 10))
			add(JumpInsnNode(GOTO, loopStart))
			
			add(l0) // xor 2
			add(VarInsnNode(ALOAD, 8)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(CALOAD))
			add(ldcInt(2))
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(l4) // xor methodhash + classhash
			add(VarInsnNode(ALOAD, 8)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 7)) // methodhash
			add(VarInsnNode(ILOAD, 6)) // classhash
			add(InsnNode(IADD))
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(finalReturn)
			add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc)) // Get field
			
			// Get Arr Index
			add(IntInsnNode(ILOAD, 0)) // String index
			add(ldcInt(2)) // Multiply by 2
			add(InsnNode(IMUL))
			add(ldcInt(1)) // Add 1
			add(InsnNode(IADD))
			
			// Create value
			add(TypeInsnNode(NEW, "java/lang/String"))
			add(InsnNode(DUP))
			add(VarInsnNode(ALOAD, 9)) // Decrypted Char Array
			add(MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"))
			add(InsnNode(DUP_X2)) // Duplicate two values down
			
			add(InsnNode(AASTORE)) // Store in array
			add(InsnNode(ARETURN)) // Return string
			
			add(l2) // xor classhash
			add(VarInsnNode(ALOAD, 8)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 6)) // classhash
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(getMethodName)
			add(VarInsnNode(ALOAD, 5))
			add(ldcInt(2))
			add(InsnNode(AALOAD))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getMethodName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 7))
			add(ldcInt(7))
			add(VarInsnNode(ISTORE, 2))
			add(JumpInsnNode(GOTO, switch)) // GOTO createCharArrays
			
			
			add(checkCache)
			add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc))
			add(IntInsnNode(ILOAD, 0))
			// Multiply by 2
			add(ldcInt(2))
			add(InsnNode(IMUL))
			// Add 1
			add(ldcInt(1))
			add(InsnNode(IADD))
			add(InsnNode(AALOAD))
			add(InsnNode(DUP))
			// Return if not null
			add(JumpInsnNode(IFNULL, afterRet))
			add(InsnNode(ARETURN))
			add(afterRet)
			add(InsnNode(POP))
			add(ldcInt(2))
			add(VarInsnNode(ISTORE, 2))
			add(JumpInsnNode(GOTO, switch)) // GOTO getCurrentThread
			
			add(getStackTrace)
			add(VarInsnNode(ALOAD, 4))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false))
			add(VarInsnNode(ASTORE, 5))
			add(ldcInt(4))
			add(VarInsnNode(ISTORE, 2))
			add(JumpInsnNode(GOTO, switch)) // GOTO getClassName
			
			add(getClassName)
			add(VarInsnNode(ALOAD, 5))
			add(ldcInt(2))
			add(InsnNode(AALOAD))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getClassName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 6))
			add(ldcInt(5))
			add(VarInsnNode(ISTORE, 2))
			add(JumpInsnNode(GOTO, switch)) // GOTO getMethodName
			
			add(genericCatch)
			add(InsnNode(POP))
			add(InsnNode(ACONST_NULL))
			add(InsnNode(ATHROW))
			
			
			// Fake try catch second half start
			add(handler)
			add(InsnNode(POP))
			add(InsnNode(ACONST_NULL))
			add(JumpInsnNode(GOTO, fakeEnd))
			
			add(l1) // xor key
			add(VarInsnNode(ALOAD, 8)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 1)) // key
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(switchEnd)
			add(InsnNode(ACONST_NULL))
			add(InsnNode(ATHROW))
			
			add(realStart)
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 3)) // Switch control
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 4)) // Thread
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 5)) // StackTrace Element Arr
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 6)) // ClassName hashcode
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 7)) // MethodName hashcode
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 8)) // Encrypted Char Array
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 9)) // Decrypted Char Array
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 10)) // Char array for index
			
			
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 2))
			add(switch)
			add(VarInsnNode(ILOAD, 2))
			add(LookupSwitchInsnNode(
				switchDefault,
				intArrayOf(
					0,
					1,
					2,
					3,
					4,
					5,
					6,
					7,
					8
				),
				arrayOf(
					checkCache,
					finalReturn,
					getCurrentThread,
					getStackTrace,
					getClassName,
					getMethodName,
					checkCache,
					createCharArrays,
					xors
				)
			))
		}
		decryptorMethod.instructions.add(insnList)
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
		list.add(FieldInsnNode(PUTSTATIC, classNode.name, storageField.name, storageField.desc))
		
		for ((index, string) in strings.withIndex()) {
			list.add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc))
			if (random.nextInt(20) == 1) {
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
		
		for ((index, char) in old.withIndex()) {
			new[index] = when (index % 5) {
				0 -> char xor 2
				1 -> char xor key
				2 -> char xor classHash
				3 -> char xor methodHash
				4 -> char xor (methodHash + classHash)
				5 -> char xor index
				else -> throw IllegalStateException("Impossible Value ($index % 6 = ${index % 6})")
			}
		}
		
		val asString = String(new)
		val decryptedAgain = decryptString(asString, key, classHash, methodHash)
		if (decryptedAgain != original) {
			throw IllegalStateException("Enc did not match {$asString} -> {$decryptedAgain}")
		}
		
		return EncryptedString(original, asString, key, classNode, methodNode, insn)
	}
	
	private fun decryptString(first: String, key: Int, classHash: Int, methodHash: Int): String {
		val old = first.toCharArray()
		val new = CharArray(first.length)
		
		for (i in 0 until (old.size)) {
			when (i % 5) {
				0 -> new[i] = old[i] xor 2
				1 -> new[i] = old[i] xor key
				2 -> new[i] = old[i] xor classHash
				3 -> new[i] = old[i] xor methodHash
				4 -> new[i] = old[i] xor (methodHash + classHash)
				5 -> new[i] = old[i] xor i
			}
		}
		return String(new)
	}
}

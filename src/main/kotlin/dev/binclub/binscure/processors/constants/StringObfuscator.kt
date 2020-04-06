package dev.binclub.binscure.processors.constants

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.kotlin.*
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.runtime.OpaqueRuntimeManager
import dev.binclub.binscure.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.util.*

/**
 * @author cookiedragon234 20/Jan/2020
 */
object StringObfuscator: IClassProcessor {
	val key = random.nextInt(Int.MAX_VALUE)
	
	var decryptNode: ClassNode? = null
	var decryptMethod: MethodNode? = null
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!ConfigurationManager.rootConfig.stringObfuscation.enabled) {
			return
		}
		
		val stringInsns = arrayListOf<EncryptedString>()
		for (classNode in classes) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
					continue
				
				for (insn in method.instructions) {
					if (insn is LdcInsnNode && insn.cst is String) {
						val encryptedString = encryptString(
							insn.cst as String,
							key,
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
			val decryptNode = ClassNode()
				.apply {
					this.access = ACC_PUBLIC + ACC_FINAL
					this.version = V1_8
					this.name = ClassRenamer.namer.uniqueRandomString() + "EntryPoint"
					this.signature = null
					this.superName = OpaqueRuntimeManager.classNode.name
					classes.add(this)
					ClassPath.classes[this.name] = this
					ClassPath.classPath[this.name] = this
					this@StringObfuscator.decryptNode = this
				}
			val storageField = FieldNode(
				ACC_STATIC,
				"0",
				"L${decryptNode.name};",
				null,
				null
			)
			decryptNode.fields.add(storageField)
			
			generateStaticBlock(decryptNode, storageField)
			generateInitFunc(decryptNode, storageField)
			
			val decryptorMethod = generateDecrypterMethod(decryptNode, storageField, stringInsns)
			this.decryptMethod = decryptorMethod
			
			decryptNode.methods.add(MethodNode(ACC_PUBLIC + ACC_STATIC, "0", "(Ljava/lang/String;)Ljava/lang/String;", null, null).apply {
				instructions.apply {
					add(VarInsnNode(ALOAD, 0))
					add(ldcInt(3))
					add(MethodInsnNode(INVOKESTATIC, decryptNode.name, decryptorMethod.name, decryptorMethod.desc))
					add(ARETURN)
				}
			})
			
			for ((index, string) in stringInsns.withIndex()) {
				
				val modifier = InstructionModifier()
				
				string.insn.cst = string.encrypted
				val list = InsnList().apply {
					add(MethodInsnNode(
						INVOKESTATIC,
						decryptNode.name,
						decryptorMethod.name,
						"(Ljava/lang/String;)Ljava/lang/String;",
						false
					))
				}
				
				modifier.append(string.insn, list)
				modifier.apply(string.methodNode)
			}
		}
	}
	
	private fun generateDecrypterMethod(classNode: ClassNode, storageField: FieldNode, strings: ArrayList<EncryptedString>): MethodNode {
		val decryptorMethod = MethodNode(
			ACC_PUBLIC + ACC_STATIC,
			"0",
			"(Ljava/lang/String;I)Ljava/lang/String;",
			null,
			null
		)
		
		val realStart = newLabel()
		val fakeEnd = newLabel()
		val start = newLabel()
		val handler = newLabel()
		val end = newLabel()
		val secondCatch = newLabel()
		
		val switch = newLabel()
		val switchImmediate = newLabel()
		val switchDefault = newLabel()
		
		val genericCatch = newLabel()
		val checkCache = newLabel()
		val afterRet = newLabel()
		val getCurrentThread = newLabel()
		val getStackTrace = newLabel()
		val getClassName = newLabel()
		val getMethodName = newLabel()
		val finalReturn = newLabel()
		val createCharArrays = newLabel()
		val xors = newLabel()
		
		// XOR SWITCH LABELS
		val loopStart = newLabel()
		val exitLoop = newLabel()
		val switchEnd = newLabel()
		val setCharArrVal = newLabel()
		val l0 = newLabel()
		val l1 = newLabel()
		val l2 = newLabel()
		val l3 = newLabel()
		val l4 = newLabel()
		val l5 = newLabel()
		
		decryptorMethod.tryCatchBlocks.apply {
			//add(TryCatchBlockNode(getCurrentThread, finalReturn, genericCatch, "java/lang/Throwable"))
			//add(TryCatchBlockNode(getStackTrace, getClassName, genericCatch, null))
			//add(TryCatchBlockNode(getMethodName, checkCache, genericCatch, "java/lang/Exception"))
			//add(TryCatchBlockNode(start, end, handler, null))
			//add(TryCatchBlockNode(fakeEnd, end, secondCatch, null))
			//add(TryCatchBlockNode(l3, xors, secondCatch, "java/lang/Throwable"))
			//add(TryCatchBlockNode(getCurrentThread, xors, secondCatch, null))
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
			add(VarInsnNode(ILOAD, 7)) // methodhash
			add(IADD)
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
			add(ldcInt(9))
			add(IADD)
			add(JumpInsnNode(GOTO, switchImmediate))
			
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
			add(VarInsnNode(ALOAD, 0))
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
			add(ldcInt(4))
			add(VarInsnNode(ILOAD, 6)) // classhash
			add(IADD)
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
			
			add(VarInsnNode(ALOAD, 0))
			// Create value
			add(TypeInsnNode(NEW, "java/lang/String"))
			add(InsnNode(DUP))
			add(VarInsnNode(ALOAD, 9)) // Decrypted Char Array
			add(MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"))
			add(InsnNode(DUP_X2)) // Duplicate two values down
			
			add(MethodInsnNode(INVOKEVIRTUAL, classNode.name, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
			add(SWAP)
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
			add(VarInsnNode(ILOAD, 11))
			add(InsnNode(AALOAD))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getMethodName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 7))
			add(ldcInt(7))
			add(VarInsnNode(ISTORE, 2))
			add(JumpInsnNode(GOTO, switch)) // GOTO createCharArrays
			
			
			add(checkCache)
			add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc))
			add(VarInsnNode(ALOAD, 0))
			add(MethodInsnNode(INVOKEVIRTUAL, classNode.name, "get", "(Ljava/lang/Object;)Ljava/lang/Object;"))
			add(DUP)
			// Return if not null
			add(JumpInsnNode(IFNULL, afterRet))
			add(TypeInsnNode(CHECKCAST, "java/lang/String"))
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
			add(VarInsnNode(ILOAD, 11))
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
			add(VarInsnNode(ILOAD, 1))
			add(VarInsnNode(ISTORE, 11))
			add(ldcInt(key))
			add(VarInsnNode(ISTORE, 1))
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
			add(switchImmediate)
			add(constructTableSwitch(
				0,
				switchDefault,
				checkCache,
				finalReturn,
				getCurrentThread,
				getStackTrace,
				getClassName,
				getMethodName,
				checkCache,
				createCharArrays,
				xors,
				l0, l1, l2, l3, l4, l5
			))
		}
		decryptorMethod.instructions.add(insnList)
		classNode.methods.add(decryptorMethod)
		return decryptorMethod
	}
	
	private fun generateInitFunc(classNode: ClassNode, storageField: FieldNode): MethodNode {
		return MethodNode(0, "<init>", "()V", null, null).apply {
			instructions.apply {
				add(VarInsnNode(ALOAD, 0))
				add(DUP)
				add(MethodInsnNode(INVOKESPECIAL, OpaqueRuntimeManager.classNode.superName, "<init>", "()V"))
				add(TypeInsnNode(CHECKCAST, classNode.name))
				add(FieldInsnNode(PUTSTATIC, classNode.name, storageField.name, storageField.desc))
				add(RETURN)
			}
			OpaqueRuntimeManager.classNode.methods.add(this)
			
			classNode.methods.add(MethodNode(0, "<init>", "()V", null, null).apply {
				instructions.apply {
					add(VarInsnNode(ALOAD, 0))
					add(MethodInsnNode(INVOKESPECIAL, classNode.superName, "<init>", "()V"))
					add(RETURN)
				}
			})
		}
	}
	
	private fun generateStaticBlock(classNode: ClassNode, storageField: FieldNode): MethodNode {
		val staticInit =
			classNode.methods.firstOrNull { it.name == "<clinit>" && it.desc == "()V" }
			?: MethodNode(ACC_STATIC, "<clinit>", "()V", null, null).apply {
				instructions.add(InsnNode(RETURN))
				classNode.methods.add(this)
			}
		
		staticInit.instructions.insert(InsnList().apply {
			add(TypeInsnNode(NEW, classNode.name))
			add(MethodInsnNode(INVOKESPECIAL, classNode.name, "<init>", "()V"))
		})
		
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
				0 -> char xor (4 + classHash)
				1 -> char xor key
				2 -> char xor classHash
				3 -> char xor methodHash
				4 -> char xor (methodHash + classHash)
				5 -> char xor (index + methodHash)
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
				0 -> new[i] = old[i] xor (4 + classHash)
				1 -> new[i] = old[i] xor key
				2 -> new[i] = old[i] xor classHash
				3 -> new[i] = old[i] xor methodHash
				4 -> new[i] = old[i] xor (methodHash + classHash)
				5 -> new[i] = old[i] xor (i + methodHash)
			}
		}
		return String(new)
	}
}

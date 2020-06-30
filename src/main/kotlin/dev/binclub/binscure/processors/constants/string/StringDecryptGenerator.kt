package dev.binclub.binscure.processors.constants.string

import dev.binclub.binscure.processors.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.util.ArrayList

/**
 * @author cookiedragon234 30/Jun/2020
 */
object StringDecryptGenerator {
	fun generateDecrypterMethod(classNode: ClassNode, storageField: FieldNode, strings: ArrayList<StringObfuscator.EncryptedString>): MethodNode {
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
		val popBeforeGetMethodName = newLabel()
		val finalReturn = newLabel()
		val createCharArrays = newLabel()
		val xors = newLabel()
		val switchExceptionReceiver = newLabel()
		val classNotFoundHandler = newLabel()
		val initialStartJumpBack = newLabel()
		
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
		
		val veryveryStart = newLabel()
		
		decryptorMethod.tryCatchBlocks.apply {
			add(TryCatchBlockNode(finalReturn, classNotFoundHandler, classNotFoundHandler, "java/lang/Throwable"))
			add(TryCatchBlockNode(getCurrentThread, switch, setCharArrVal, "java/lang/NoClassDefFoundError"))
			add(TryCatchBlockNode(getClassName, genericCatch, popBeforeGetMethodName, "java/lang/BootstrapMethodError"))
			add(TryCatchBlockNode(getCurrentThread, switch, switchExceptionReceiver, "java/lang/IllegalMonitorStateException"))
			add(TryCatchBlockNode(getCurrentThread, setCharArrVal, switchExceptionReceiver, "java/lang/IllegalMonitorStateException"))
			add(TryCatchBlockNode(getCurrentThread, finalReturn, switchExceptionReceiver, "java/lang/IllegalMonitorStateException"))
			add(TryCatchBlockNode(getCurrentThread, switch, classNotFoundHandler, "java/lang/BootstrapMethodError"))
			add(TryCatchBlockNode(getCurrentThread, setCharArrVal, classNotFoundHandler, "java/lang/BootstrapMethodError"))
			add(TryCatchBlockNode(getCurrentThread, finalReturn, classNotFoundHandler, "java/lang/BootstrapMethodError"))
			add(TryCatchBlockNode(getCurrentThread, switch, afterRet, "java/lang/IllegalStateException"))
			add(TryCatchBlockNode(getCurrentThread, setCharArrVal, afterRet, "java/lang/IllegalStateException"))
			add(TryCatchBlockNode(getCurrentThread, finalReturn, afterRet, "java/lang/IllegalStateException"))
			add(TryCatchBlockNode(getCurrentThread, switch, handler, "java/lang/IllegalArgumentException"))
			add(TryCatchBlockNode(getCurrentThread, setCharArrVal, handler, "java/lang/IllegalArgumentException"))
			add(TryCatchBlockNode(getCurrentThread, finalReturn, handler, "java/lang/IllegalArgumentException"))
			add(TryCatchBlockNode(getCurrentThread, setCharArrVal, genericCatch, "java/lang/NoClassDefFoundError"))
			add(TryCatchBlockNode(getCurrentThread, finalReturn, genericCatch, "java/lang/NoClassDefFoundError"))
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
			
			add(VarInsnNode(ALOAD, 0))
			add(VarInsnNode(ASTORE, 12))
			add(VarInsnNode(ALOAD, 0))
			add(VarInsnNode(ASTORE, 14))
			add(VarInsnNode(ALOAD, 0))
			add(VarInsnNode(ASTORE, 3)) // Switch control
			
			
			add(ICONST_1)
			add(FieldInsnNode(GETSTATIC, classNode.name, StringObfuscator.keysField.name, StringObfuscator.keysField.desc))
			add(VarInsnNode(ASTORE, 16))
			add(VarInsnNode(ISTORE, 13))
			
			add(VarInsnNode(ALOAD, 16))
			add(VarInsnNode(ASTORE, 12))
			add(VarInsnNode(ALOAD, 16))
			add(VarInsnNode(ASTORE, 14))
			add(VarInsnNode(ALOAD, 16))
			add(VarInsnNode(ASTORE, 3)) // Switch control
			
			add(JumpInsnNode(GOTO, veryveryStart))
			add(initialStartJumpBack)
			add(VarInsnNode(ALOAD, 0)) // string param
			add(JumpInsnNode(IFNONNULL, veryveryStart))
			add(TypeInsnNode(NEW, NullPointerException::class.internalName))
			add(DUP)
			add(LdcInsnNode("String deobfuscation parameter should not be null"))
			add(MethodInsnNode(INVOKESPECIAL, NullPointerException::class.internalName, "<init>", "(Ljava/lang/String;)V"))
			add(ATHROW)
			add(veryveryStart)
			add(TypeInsnNode(NEW, "java/lang/Exception"))
			add(ICONST_M1)
			add(ACONST_NULL)
			add(ACONST_NULL)
			add(VarInsnNode(ASTORE, 4))
			add(VarInsnNode(ASTORE, 8))
			add(ACONST_NULL)
			add(VarInsnNode(ASTORE, 5))
			add(ICONST_M1)
			add(VarInsnNode(ILOAD, 1))
			add(VarInsnNode(ISTORE, 11))
			add(ldcInt(StringObfuscator.key))
			add(VarInsnNode(ISTORE, 1))
			add(VarInsnNode(ISTORE, 2))
			add(ICONST_M1)
			add(ACONST_NULL)
			add(VarInsnNode(ASTORE, 9))
			add(VarInsnNode(ISTORE, 10))
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 15))
			add(VarInsnNode(ISTORE, 6))
			newLabel().also {
				add(randomOpaqueJump(it))
				add(
					InvokeDynamicInsnNode(
					null, null, null
				)
				)
				add(
					InvokeDynamicInsnNode(
					"fuck", "()V", Handle(H_INVOKESTATIC, "a", "a", "(IIIIIIIIIIIIIIIIIIIIIIII)Ljava/lang/Throwable;")
				)
				)
				add(
					InvokeDynamicInsnNode(
					"yayeet", "()Ljava/lang/YaYeet;", Handle(H_INVOKESTATIC, "a", "a", "()[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[I")
				)
				)
				add(POP)
				add(it)
			}
			add(ICONST_M1)
			add(VarInsnNode(ISTORE, 7))
			add(POP)
			add(VarInsnNode(ILOAD, 13))
			add(JumpInsnNode(IFEQ, initialStartJumpBack))
			add(ICONST_0)
			add(VarInsnNode(ISTORE, 13))
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
			add(VarInsnNode(ISTORE, 15))
			add(MethodInsnNode(INVOKESTATIC, "_______", "a", "()V"))
			add(JumpInsnNode(GOTO, getCurrentThread))
			
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
			add(VarInsnNode(ALOAD, 14))
			add(MONITOREXIT) // GOTO getStackTrace
			add(JumpInsnNode(GOTO, getCurrentThread))
			
			add(createCharArrays)
			add(VarInsnNode(ALOAD, 0))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false))
			add(VarInsnNode(ASTORE, 8))
			add(VarInsnNode(ALOAD, 8))
			add(InsnNode(ARRAYLENGTH))
			add(IntInsnNode(NEWARRAY, Opcodes.T_CHAR))
			add(VarInsnNode(ASTORE, 9))
			add(ldcInt(8))
			add(VarInsnNode(ISTORE, 2))
			add(VarInsnNode(ALOAD, 14))
			add(MONITOREXIT) // GOTO xors
			add(JumpInsnNode(GOTO, loopStart))
			
			add(setCharArrVal)
			add(POP)
			add(VarInsnNode(ILOAD, 15))
			add(InsnNode(I2C))
			add(VarInsnNode(ALOAD, 16))
			
			add(VarInsnNode(ILOAD, 10)) // Index
			add(ldcInt(StringObfuscator.keys.size))
			add(IREM)
			add(IALOAD)
			add(IXOR)
			
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
			add(VarInsnNode(ALOAD, 14))
			add(MONITOREXIT) // GOTO finalReturn
			add(JumpInsnNode(GOTO, xors))
			
			add(l3) // xor methodhash
			add(VarInsnNode(ALOAD, 8)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 7)) // methodhash
			add(InsnNode(IXOR))
			add(VarInsnNode(ISTORE, 15))
			add(MethodInsnNode(INVOKESTATIC, "_______", "a", "()V"))
			add(JumpInsnNode(GOTO, veryveryStart))
			
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
			add(VarInsnNode(ISTORE, 15))
			add(MethodInsnNode(INVOKESTATIC, "_______", "a", "()V"))
			add(JumpInsnNode(GOTO, l4))
			
			add(l4) // xor methodhash + classhash
			add(VarInsnNode(ALOAD, 8)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 10)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 7)) // methodhash
			add(VarInsnNode(ILOAD, 6)) // classhash
			add(InsnNode(IADD))
			add(InsnNode(IXOR))
			add(VarInsnNode(ISTORE, 15))
			add(MethodInsnNode(INVOKESTATIC, "_______", "a", "()V"))
			add(JumpInsnNode(GOTO, xors))
			
			add(finalReturn)
			add(InvokeDynamicInsnNode("________", "()V", Handle(H_INVOKESTATIC, "________", "a", "()V")))
			add(JumpInsnNode(GOTO, veryveryStart))
			
			add(classNotFoundHandler)
			add(POP)
			
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
			add(VarInsnNode(ISTORE, 15))
			add(MethodInsnNode(INVOKESTATIC, "_______", "a", "()V"))
			add(JumpInsnNode(GOTO, getCurrentThread))
			
			add(popBeforeGetMethodName)
			add(POP)
			
			add(getMethodName)
			add(VarInsnNode(ALOAD, 5))
			add(VarInsnNode(ILOAD, 11))
			add(InsnNode(AALOAD))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getMethodName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 7))
			add(ldcInt(7))
			add(VarInsnNode(ISTORE, 2))
			add(VarInsnNode(ALOAD, 14))
			add(MONITOREXIT) // GOTO createCharArrays
			
			
			add(checkCache)
			add(FieldInsnNode(GETSTATIC, classNode.name, storageField.name, storageField.desc))
			add(VarInsnNode(ALOAD, 0))
			add(MethodInsnNode(INVOKEVIRTUAL, classNode.name, "get", "(Ljava/lang/Object;)Ljava/lang/Object;"))
			add(DUP)
			// Return if not null
			val b4afterRet = newLabel()
			add(JumpInsnNode(IFNULL, b4afterRet))
			add(TypeInsnNode(CHECKCAST, "java/lang/String"))
			add(InsnNode(ARETURN))
			add(b4afterRet)
			add(TypeInsnNode(NEW, "java/lang/IllegalStateException"))
			add(DUP)
			add(MethodInsnNode(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V"))
			add(ATHROW)
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
			add(VarInsnNode(ALOAD, 14))
			add(MONITOREXIT) // GOTO getClassName
			add(ACONST_NULL)
			add(JumpInsnNode(GOTO, afterRet))
			
			add(getClassName)
			add(VarInsnNode(ALOAD, 5))
			add(VarInsnNode(ILOAD, 11))
			add(InsnNode(AALOAD))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getClassName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 6))
			add(ldcInt(5))
			add(VarInsnNode(ISTORE, 2))
			add(VarInsnNode(ALOAD, 14))
			add(DUP)
			val popBeforeRealStart = newLabel()
			add(JumpInsnNode(IFNULL, popBeforeRealStart))
			add(MONITOREXIT) // GOTO getMethodName
			add(InvokeDynamicInsnNode("fuck", "()V", Handle(H_INVOKESTATIC, "a", "a", "(IIIIIIIIIIIIIIIIIIIIIIII)Ljava/lang/Throwable;")))
			add(ACONST_NULL)
			add(ATHROW)
			
			add(genericCatch)
			//add(InsnNode(POP))
			//add(InsnNode(ACONST_NULL))
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
			add(VarInsnNode(ISTORE, 15))
			add(MethodInsnNode(INVOKESTATIC, "_______", "a", "()V"))
			add(JumpInsnNode(GOTO, getCurrentThread))
			
			add(switchEnd)
			add(InsnNode(ACONST_NULL))
			add(InsnNode(ATHROW))
			
			add(popBeforeRealStart)
			add(POP)
			
			add(realStart)
			add(ldcInt(StringObfuscator.key))
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
			add(JumpInsnNode(GOTO, switch))
			add(switchExceptionReceiver)
			add(JumpInsnNode(IFNONNULL, switch))
			add(ACONST_NULL)
			add(ATHROW)
			add(switch)
			add(VarInsnNode(ILOAD, 2))
			add(switchImmediate)
			add(
				constructTableSwitch(
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
			)
			)
		}
		decryptorMethod.instructions.add(insnList)
		//decryptorMethod.instructions = insnListOf(InsnNode(ACONST_NULL), InsnNode(ARETURN))
		classNode.methods.add(decryptorMethod)
		return decryptorMethod
	}
}

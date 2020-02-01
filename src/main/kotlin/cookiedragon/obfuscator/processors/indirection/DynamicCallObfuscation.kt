package cookiedragon.obfuscator.processors.indirection

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.kotlin.xor
import cookiedragon.obfuscator.processors.renaming.impl.ClassRenamer
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.io.PrintStream
import java.lang.invoke.ConstantCallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * @author cookiedragon234 22/Jan/2020
 */
object DynamicCallObfuscation: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		//if (!ConfigurationManager.rootConfig.indirection.enabled)
		//	return
		
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
						
						// Only obfuscate api calls
						//if (!CObfuscator.isExcluded("${insn.owner}.${insn.name}${insn.desc}"))
						//	continue
						
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
				ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
				"bootstrap",
				"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
				null,
				null
			).apply {
				generateBootstrapMethod(decryptNode.name, this)
				decryptNode.methods.add(this)
			}
			
			val handler = Handle(H_INVOKESTATIC, decryptNode.name, bootStrapMethod.name, bootStrapMethod.desc, false)
			
			for (methodCall in methodCalls) {
				val insn = methodCall.insnNode
				
				val desc = if (methodCall.insnNode.opcode == INVOKESTATIC) {
					methodCall.insnNode.desc
				} else {
					methodCall.insnNode.desc.replaceFirst("(", "(Ljava/lang/Object;")
				}
				
				var newDesc = if (insn.owner.startsWith("[")) "(" else "(L"
				newDesc += insn.owner
				newDesc += if (insn.owner.endsWith(";")) "" else ";"
				newDesc += insn.desc.substring(1)
				
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
		originalStr += (if (methodNode.opcode == INVOKESTATIC) "s" else "v")
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
	
	private fun generateBootstrapMethod(className: String, methodNode: MethodNode) {
		// Description "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
		methodNode.instructions.apply {
			val start = LabelNode(Label())
			val end = LabelNode(Label())
			val loopStart = LabelNode(Label())
			val exitLoop = LabelNode(Label())
			val setCharArrVal = LabelNode(Label())
			val throwNull = LabelNode(Label())
			val getThread = LabelNode(Label())
			val lookupVirtual = LabelNode(Label())
			val lookupStatic = LabelNode(Label())
			val handler = LabelNode(Label())
			
			val tble0 = LabelNode(Label())
			val tble1 = LabelNode(Label())
			val tble2 = LabelNode(Label())
			val tble3 = LabelNode(Label())
			val tble4 = LabelNode(Label())
			
			add(start)
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 4)) // encrypted char array
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 5)) // Decrypted char array
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 6)) // Loop index
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 7)) // Current Thread
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 8)) // StackTrace
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 9)) // Class Name Hash
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 10)) // Method Name Hash
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 11)) // Decrypted desc string
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 12)) // Static/Virtual ('s'/'v')
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 13)) // Class to lookup
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 14)) // Method Name to lookup
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 15)) // Method Desc to lookup
			add(InsnNode(ACONST_NULL))
			add(VarInsnNode(ASTORE, 16)) // Temporary descriptor array
			
			// First we need to decrypt the method description stored at local var 1
			// We will turn it into a char array
			add(VarInsnNode(ALOAD, 1))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false))
			add(InsnNode(DUP))
			// Store it in local var 3
			add(VarInsnNode(ASTORE, 4))
			// Find the array length and create our decrypted char array (store in slot 4)
			add(InsnNode(ARRAYLENGTH))
			add(IntInsnNode(NEWARRAY, T_CHAR))
			add(VarInsnNode(ASTORE, 5))
			//add(InsnNode(DUP))
			//add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "L${PrintStream::class.internalName};"))
			//add(InsnNode(SWAP))
			//add(MethodInsnNode(INVOKESTATIC, Arrays::class.internalName, "toString", "([C)Ljava/lang/String;"))
			//add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/String;)V"))
			
			// Get the class and method hash
			add(getThread)
			add(MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false))
			add(VarInsnNode(ASTORE, 7)) // Stored in var 7
			add(VarInsnNode(ALOAD, 7))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false))
			add(ldcInt(5))
			add(InsnNode(AALOAD))
			add(VarInsnNode(ASTORE, 8))
			add(VarInsnNode(ALOAD, 8))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getClassName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 9))
			add(VarInsnNode(ALOAD, 8))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getMethodName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 10))
			
			// Now loop over our new array
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 6))
			add(loopStart)
			add(VarInsnNode(ILOAD, 6)) // index stored at slot 6
			add(VarInsnNode(ALOAD, 4))
			add(InsnNode(ARRAYLENGTH))
			add(JumpInsnNode(IF_ICMPGE, exitLoop)) // If at the end of the loop go to exit
			
			
			add(VarInsnNode(ILOAD, 6))
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
			add(VarInsnNode(ILOAD, 6)) // index
			add(InsnNode(CALOAD))
			add(ldcInt(2))
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble1)
			add(VarInsnNode(ALOAD, 4)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 6)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 9)) // Class Hash
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble2)
			add(VarInsnNode(ALOAD, 4)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 6)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 10)) // method Hash
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble3)
			add(VarInsnNode(ALOAD, 4)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 6)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 9)) // Class Hash
			add(VarInsnNode(ILOAD, 10)) // method Hash
			add(InsnNode(IADD))
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble4)
			add(VarInsnNode(ALOAD, 4)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 6)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 6)) // index
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			
			add(setCharArrVal)
			add(InsnNode(I2C))
			add(VarInsnNode(ALOAD, 5)) // Decrypted Char Array
			add(InsnNode(SWAP))
			add(VarInsnNode(ILOAD, 6)) // Index
			add(InsnNode(SWAP))
			add(InsnNode(CASTORE))
			// Increment and go to top of loop
			add(IincInsnNode(6, 1))
			add(JumpInsnNode(GOTO, loopStart))
			
			
			// If we are here then we have a decrypted char array in slot 4
			add(exitLoop)
			add(TypeInsnNode(NEW, "java/lang/String"))
			add(InsnNode(DUP))
			add(VarInsnNode(ALOAD, 5)) // Decrypted Char Array
			add(MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"))
			add(VarInsnNode(ASTORE, 11)) // Decrypted String
			
			// Check if its static or not
			add(VarInsnNode(ALOAD, 11)) // Decrypted String
			add(InsnNode(DUP))
			add(MethodInsnNode(INVOKEVIRTUAL, String::class.internalName, "length", "()I"))
			add(ldcInt(-1))
			add(InsnNode(IADD))
			add(MethodInsnNode(INVOKEVIRTUAL, String::class.internalName, "charAt", "(I)C"))
			add(VarInsnNode(ISTORE, 12))
			
			// Substring the string
			add(VarInsnNode(ALOAD, 11)) // Decrypted String
			add(InsnNode(DUP))
			add(MethodInsnNode(INVOKEVIRTUAL, String::class.internalName, "length", "()I"))
			add(ldcInt(-1))
			add(InsnNode(IADD))
			add(ldcInt(0))
			add(InsnNode(SWAP))
			add(MethodInsnNode(INVOKEVIRTUAL, String::class.internalName, "substring", "(II)Ljava/lang/String;"))
			add(VarInsnNode(ASTORE, 11)) // Decrypted String
			
			add(VarInsnNode(ALOAD, 11)) // Decrypted String
			add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "L${PrintStream::class.internalName};"))
			add(InsnNode(SWAP))
			add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V"))
			
			// Get class and description
			add(VarInsnNode(ALOAD, 11)) // Decrypted String
			add(LdcInsnNode("[.]"))
			add(MethodInsnNode(INVOKEVIRTUAL, String::class.internalName, "split", "(Ljava/lang/String;)[Ljava/lang/String;"))
			add(VarInsnNode(ASTORE, 16)) // Temporary descriptor array
			
			// Get class reference
			add(VarInsnNode(ALOAD, 16))
			add(ldcInt(0))
			add(InsnNode(AALOAD))
			add(ldcInt('/'.toInt()))
			add(ldcInt('.'.toInt()))
			add(MethodInsnNode(INVOKEVIRTUAL, String::class.internalName, "replace", "(CC)Ljava/lang/String;"))
			add(MethodInsnNode(INVOKESTATIC, Class::class.internalName, "forName", "(Ljava/lang/String;)Ljava/lang/Class;"))
			add(VarInsnNode(ASTORE, 13))
			
			// Get Description
			add(VarInsnNode(ALOAD, 16))
			add(ldcInt(1))
			add(InsnNode(AALOAD))
			add(VarInsnNode(ASTORE, 14))
			
			// Get MethodType
			add(VarInsnNode(ALOAD, 16))
			add(ldcInt(2))
			add(InsnNode(AALOAD))
			add(VarInsnNode(ALOAD, 13))
			add(MethodInsnNode(INVOKEVIRTUAL, Class::class.internalName, "getClassLoader", "()Ljava/lang/ClassLoader;"))
			add(MethodInsnNode(INVOKESTATIC, MethodType::class.internalName, "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;"))
			add(VarInsnNode(ASTORE, 15))
			
			add(VarInsnNode(ILOAD, 12))
			add(ldcInt('s'.toInt()))
			add(JumpInsnNode(IF_ICMPEQ, lookupStatic))
			
			// Now we can find a method with this name and desc
			add(lookupVirtual)
			add(TypeInsnNode(NEW, ConstantCallSite::class.internalName))
			add(InsnNode(DUP))
			add(VarInsnNode(ALOAD, 0)) // Method Handle Lookup
			add(VarInsnNode(ALOAD, 13))
			add(InsnNode(DUP))
			add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "L${PrintStream::class.internalName};"))
			add(InsnNode(SWAP))
			add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V"))
			add(VarInsnNode(ALOAD, 14))
			add(InsnNode(DUP))
			add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "L${PrintStream::class.internalName};"))
			add(InsnNode(SWAP))
			add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V"))
			add(VarInsnNode(ALOAD, 15))
			add(InsnNode(DUP))
			add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "L${PrintStream::class.internalName};"))
			add(InsnNode(SWAP))
			add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V"))
			add(MethodInsnNode(INVOKEVIRTUAL, MethodHandles.Lookup::class.internalName, "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
			add(MethodInsnNode(INVOKESPECIAL, ConstantCallSite::class.internalName, "<init>", "(Ljava/lang/invoke/MethodHandle;)V"))
			add(InsnNode(ARETURN))
			
			add(lookupStatic)
			add(TypeInsnNode(NEW, ConstantCallSite::class.internalName))
			add(InsnNode(DUP))
			add(VarInsnNode(ALOAD, 0)) // Method Handle Lookup
			add(VarInsnNode(ALOAD, 13))
			add(InsnNode(DUP))
			add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "L${PrintStream::class.internalName};"))
			add(InsnNode(SWAP))
			add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V"))
			add(VarInsnNode(ALOAD, 14))
			add(InsnNode(DUP))
			add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "L${PrintStream::class.internalName};"))
			add(InsnNode(SWAP))
			add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V"))
			add(VarInsnNode(ALOAD, 15))
			add(InsnNode(DUP))
			add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "L${PrintStream::class.internalName};"))
			add(InsnNode(SWAP))
			add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V"))
			add(MethodInsnNode(INVOKEVIRTUAL, MethodHandles.Lookup::class.internalName, "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
			add(MethodInsnNode(INVOKESPECIAL, ConstantCallSite::class.internalName, "<init>", "(Ljava/lang/invoke/MethodHandle;)V"))
			add(InsnNode(ARETURN))
			
			add(throwNull)
			add(InsnNode(ACONST_NULL))
			add(InsnNode(ATHROW))
			
			add(end)
			add(handler)
			add(InsnNode(DUP))
			add(MethodInsnNode(INVOKEVIRTUAL, Throwable::class.internalName, "printStackTrace", "()V"))
			add(InsnNode(ATHROW))
			methodNode.tryCatchBlocks.add(TryCatchBlockNode(start, end, handler, null))
		}
	}
	
	data class MethodCall(val classNode: ClassNode, val methodNode: MethodNode, val insnNode: MethodInsnNode)
}

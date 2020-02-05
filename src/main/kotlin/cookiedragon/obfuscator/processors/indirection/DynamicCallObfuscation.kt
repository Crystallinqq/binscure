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
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

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
					if (insn is MethodInsnNode) {
						when (insn.opcode) {
							INVOKESTATIC, INVOKEVIRTUAL, INVOKEINTERFACE -> methodCalls.add(MethodCall(classNode, method, insn))
						}
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
			
			val stringDecryptMethod = MethodNode(
				ACC_PRIVATE + ACC_FINAL + ACC_STATIC,
				"str",
				"(Ljava/lang/String;)Ljava/lang/String;",
				null,
				null
			).apply {
				generateDecryptorMethod(decryptNode.name, this)
				decryptNode.methods.add(this)
			}
			
			val bootStrapMethod = MethodNode(
				ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
				"bootstrap",
				"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
				null,
				null
			).apply {
				generateBootstrapMethod(decryptNode.name, stringDecryptMethod, this)
				decryptNode.methods.add(this)
			}
			
			val handler = Handle(H_INVOKESTATIC, decryptNode.name, bootStrapMethod.name, bootStrapMethod.desc, false)
			
			for (methodCall in methodCalls) {
				val insn = methodCall.insnNode
				
				
				var newDesc = insn.desc
				if (insn.opcode != INVOKESTATIC) {
					newDesc = newDesc.replace("(", "(Ljava/lang/Object;")
				}
				val returnType = Type.getReturnType(newDesc)
				
				// Downcast types to java/lang/Object
				val args = Type.getArgumentTypes(newDesc)
				for (i in args.indices) {
					args[i] = genericType(args[i])
				}
				
				newDesc = Type.getMethodDescriptor(returnType, *args)
				
				val list = InsnList().apply {
					add(InvokeDynamicInsnNode("", newDesc, handler,
						encryptName(methodCall.classNode, methodCall.methodNode, insn.owner.replace('/', '.')),
						encryptName(methodCall.classNode, methodCall.methodNode, insn.name),
						encryptName(methodCall.classNode, methodCall.methodNode, insn.desc)
					))
					if (returnType.sort == Type.ARRAY) {
						add(TypeInsnNode(CHECKCAST, returnType.internalName))
					}
				}
				
				methodCall.methodNode.instructions.insert(insn, list)
				methodCall.methodNode.instructions.remove(insn)
			}
		}
	}
	
	private fun genericType(type: Type): Type {
		return when (type.sort) {
			Type.OBJECT -> Type.getType(Any::class.java)
			else -> type
		}
	}
	
	private fun encryptName(classNode: ClassNode, methodNode: MethodNode, originalStr: String): String {
		val classHash = classNode.name.replace('/', '.').hashCode()
		val methodHash = methodNode.name.replace('/', '.').hashCode()
		
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
	
	private fun generateDecryptorMethod(className: String, methodNode: MethodNode) {
		methodNode.instructions.apply {
			val loopStart = LabelNode(Label())
			val exitLoop = LabelNode(Label())
			val setCharArrVal = LabelNode(Label())
			val throwNull = LabelNode(Label())
			val getThread = LabelNode(Label())
			val tble0 = LabelNode(Label())
			val tble1 = LabelNode(Label())
			val tble2 = LabelNode(Label())
			val tble3 = LabelNode(Label())
			val tble4 = LabelNode(Label())
			
			// First we need to decrypt the method description stored at local var 1
			// We will turn it into a char array
			add(VarInsnNode(ALOAD, 0))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false))
			add(InsnNode(DUP))
			add(VarInsnNode(ASTORE, 1))
			// Find the array length and create our decrypted char array (store in slot 4)
			add(InsnNode(ARRAYLENGTH))
			add(IntInsnNode(NEWARRAY, T_CHAR))
			add(VarInsnNode(ASTORE, 2))
			
			// Get the class and method hash
			add(getThread)
			add(MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false))
			add(VarInsnNode(ASTORE, 3)) // Stored in var 7
			add(VarInsnNode(ALOAD, 3))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false))
			add(ldcInt(6))
			add(InsnNode(AALOAD))
			add(VarInsnNode(ASTORE, 4))
			add(VarInsnNode(ALOAD, 4))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getClassName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 5))
			add(VarInsnNode(ALOAD, 4))
			add(MethodInsnNode(INVOKEVIRTUAL, StackTraceElement::class.internalName, "getMethodName", "()Ljava/lang/String;", false))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
			add(VarInsnNode(ISTORE, 6))
			
			// Now loop over our new array
			add(ldcInt(0))
			add(VarInsnNode(ISTORE, 7))
			add(loopStart)
			add(VarInsnNode(ILOAD, 7))
			add(VarInsnNode(ALOAD, 2))
			add(InsnNode(ARRAYLENGTH))
			add(JumpInsnNode(IF_ICMPGE, exitLoop)) // If at the end of the loop go to exit
			
			
			add(VarInsnNode(ILOAD, 7))
			add(ldcInt(5))
			add(InsnNode(IREM))
			add(TableSwitchInsnNode(
				0,
				4,
				throwNull,
				tble0, tble1, tble2, tble3, tble4
			))
			
			add(tble0)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(InsnNode(CALOAD))
			add(ldcInt(2))
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble1)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 5)) // Class Hash
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble2)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 6)) // method Hash
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble3)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 5)) // Class Hash
			add(VarInsnNode(ILOAD, 6)) // method Hash
			add(InsnNode(IADD))
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble4)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(InsnNode(CALOAD))
			add(VarInsnNode(ILOAD, 7)) // index
			add(InsnNode(IXOR))
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			
			add(setCharArrVal)
			add(InsnNode(I2C))
			add(VarInsnNode(ALOAD, 1)) // Decrypted Char Array
			add(InsnNode(SWAP))
			add(VarInsnNode(ILOAD, 7)) // Index
			add(InsnNode(SWAP))
			add(InsnNode(CASTORE))
			// Increment and go to top of loop
			add(IincInsnNode(7, 1))
			add(JumpInsnNode(GOTO, loopStart))
			
			
			// If we are here then we have a decrypted char array in slot 4
			add(exitLoop)
			add(TypeInsnNode(NEW, "java/lang/String"))
			add(InsnNode(DUP))
			add(VarInsnNode(ALOAD, 2)) // Decrypted Char Array
			add(MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"))
			add(InsnNode(ARETURN))
		}
	}
	
	private fun generateBootstrapMethod(className: String, strDecryptNode: MethodNode, methodNode: MethodNode) {
		// Description (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;
		methodNode.instructions.apply {
		
		}
	}
	
	data class MethodCall(val classNode: ClassNode, val methodNode: MethodNode, val insnNode: MethodInsnNode)
}

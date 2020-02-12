package cookiedragon.obfuscator.processors.indirection

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.kotlin.add
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.kotlin.xor
import cookiedragon.obfuscator.processors.renaming.impl.ClassRenamer
import cookiedragon.obfuscator.utils.InstructionModifier
import cookiedragon.obfuscator.utils.ldcInt
import cookiedragon.obfuscator.utils.printlnAsm
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.invoke.ConstantCallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.AccessibleObject

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
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
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
		
		if (methodCalls.isNotEmpty()) {
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
				"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;",
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
				
				val modifier = InstructionModifier()
				val list = InsnList().apply {
					add(
						InvokeDynamicInsnNode(
							"",
							newDesc,
							handler,
							insn.opcode,
							encryptName(methodCall.classNode, methodCall.methodNode, insn.owner.replace('/', '.')),
							encryptName(methodCall.classNode, methodCall.methodNode, insn.name),
							encryptName(methodCall.classNode, methodCall.methodNode, insn.desc)
						)
					)
					if (returnType.sort == Type.ARRAY) {
						add(TypeInsnNode(CHECKCAST, returnType.internalName))
					}
				}
				modifier.replace(insn, list)
				modifier.apply(methodCall.methodNode)
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
		
		for (i in original.indices) {
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
			add(DUP)
			add(VarInsnNode(ASTORE, 1))
			// Find the array length and create our decrypted char array (store in slot 4)
			add(ARRAYLENGTH)
			add(IntInsnNode(NEWARRAY, T_CHAR))
			add(VarInsnNode(ASTORE, 2))
			
			// Get the class and method hash
			add(getThread)
			add(MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false))
			add(VarInsnNode(ASTORE, 3)) // Stored in var 7
			add(VarInsnNode(ALOAD, 3))
			add(MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false))
			add(ldcInt(6))
			add(AALOAD)
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
			add(ARRAYLENGTH)
			add(JumpInsnNode(IF_ICMPGE, exitLoop)) // If at the end of the loop go to exit
			
			
			add(VarInsnNode(ILOAD, 7))
			add(ldcInt(5))
			add(IREM)
			add(TableSwitchInsnNode(
				0,
				4,
				throwNull,
				tble0, tble1, tble2, tble3, tble4
			))
			
			add(tble0)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(CALOAD)
			add(ldcInt(2))
			add(IXOR)
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble1)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(CALOAD)
			add(VarInsnNode(ILOAD, 5)) // Class Hash
			add(IXOR)
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble2)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(CALOAD)
			add(VarInsnNode(ILOAD, 6)) // method Hash
			add(IXOR)
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble3)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(CALOAD)
			add(VarInsnNode(ILOAD, 5)) // Class Hash
			add(VarInsnNode(ILOAD, 6)) // method Hash
			add(IADD)
			add(IXOR)
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			add(tble4)
			add(VarInsnNode(ALOAD, 1)) // Encrypted Char Array
			add(VarInsnNode(ILOAD, 7)) // index
			add(CALOAD)
			add(VarInsnNode(ILOAD, 7)) // index
			add(IXOR)
			add(JumpInsnNode(GOTO, setCharArrVal))
			
			
			add(setCharArrVal)
			add(I2C)
			add(VarInsnNode(ALOAD, 1)) // Decrypted Char Array
			add(SWAP)
			add(VarInsnNode(ILOAD, 7)) // Index
			add(SWAP)
			add(CASTORE)
			// Increment and go to top of loop
			add(IincInsnNode(7, 1))
			add(JumpInsnNode(GOTO, loopStart))
			
			
			// If we are here then we have a decrypted char array in slot 4
			add(exitLoop)
			add(TypeInsnNode(NEW, "java/lang/String"))
			add(DUP)
			add(VarInsnNode(ALOAD, 1)) // Decrypted Char Array
			add(MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"))
			add(DUP)
			add(printlnAsm())
			add(ARETURN)
			
			add(throwNull)
			add(ACONST_NULL)
			add(ATHROW)
		}
	}
	
	private fun generateBootstrapMethod(className: String, strDecryptNode: MethodNode, methodNode: MethodNode) {
		// Description (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;
		methodNode.instructions.apply {
			/* Variables
				0 = MethodHandleLookup
				1 = callername
				2 = MethodType
				3 = Opcode (INVOKESTATIC, INVOKEVIRTUAL, INVOKEINTERFACE)
				4 = Class Name
				5 = Method Name
				6 = Method Sig
				7 = Class
				8 = Real Method Type
			 */
			
			// ========= Decrypt Names =========
			add(VarInsnNode(ALOAD, 4)) // Enc CLass Name
			add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
			add(DUP)
			add(printlnAsm())
			add(VarInsnNode(ASTORE, 4)) // CLass Name
			
			add(VarInsnNode(ALOAD, 5)) // Enc Method Name
			add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
			add(VarInsnNode(ASTORE, 5)) // Method Name
			
			add(VarInsnNode(ALOAD, 6)) // Enc Method Sig
			add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
			add(VarInsnNode(ASTORE, 6)) // Method Sig
			// ========= Decrypt Names =========
			
			// ========= Class.forName =========
			add(VarInsnNode(ALOAD, 4)) // CLass Name
			add(MethodInsnNode(INVOKESTATIC, Class::class.internalName, "forName", "(Ljava/lang/String;)Ljava/lang/Class;"))
			add(VarInsnNode(ASTORE, 7))// Class
			// ========= Class.forName =========
			
			// ========= Get Method Type =========
			add(VarInsnNode(ALOAD, 6)) // Method Sig
			println(Type.getType("L$className;"))
			add(LdcInsnNode(Type.getType("L$className;")))
			add(MethodInsnNode(INVOKEVIRTUAL, Class::class.internalName, "getClassLoader", "()Ljava/lang/ClassLoader;")) // Class Loader
			add(MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;"))
			add(VarInsnNode(ASTORE, 8)) // Real Method Type
			// ========= Get Method Type =========
			
			add(VarInsnNode(ALOAD, 4))
			add(VarInsnNode(ALOAD, 5))
			add(VarInsnNode(ALOAD, 8)) // Real Method Type
			add(MethodInsnNode(INVOKEVIRTUAL, MethodType::class.internalName, "ptypes", "()[Ljava/lang/Class;"))
			add(MethodInsnNode(INVOKEVIRTUAL, Class::class.internalName, "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"))
			add(DUP)
			add(ldcInt(1))
			add(MethodInsnNode(INVOKEVIRTUAL, AccessibleObject::class.internalName, "setAccessible", "(Z)V"))
			add(VarInsnNode(ALOAD, 0))
			add(SWAP)
			add(MethodInsnNode(INVOKEVIRTUAL, MethodHandles.Lookup::class.internalName, "unreflect", "(Ljava/lang/reflect/Method;)Ljava/lang/invoke/MethodHandle;"))
			
			// ========= If Statement =========
			val retConstant = LabelNode(Label())
			val lStatic = LabelNode(Label())
			val lVirtual = LabelNode(Label())
			val lInterface = LabelNode(Label())
			
			/*add(VarInsnNode(ILOAD, 3)) // Opcode
			add(ldcInt(INVOKESTATIC))
			add(JumpInsnNode(IF_ICMPEQ, lStatic))
			add(VarInsnNode(ILOAD, 3)) // Opcode
			add(ldcInt(INVOKEVIRTUAL))
			add(JumpInsnNode(IF_ICMPEQ, lVirtual))
			add(VarInsnNode(ILOAD, 3)) // Opcode
			add(ldcInt(INVOKEINTERFACE))
			add(JumpInsnNode(IF_ICMPEQ, lInterface))
			
			add(ACONST_NULL)
			add(ATHROW)
			// ========= If Statement =========
			
			// ========= If Static =========
			add(lStatic)
			add(VarInsnNode(ALOAD, 0))
			add(VarInsnNode(ALOAD, 4))
			add(VarInsnNode(ALOAD, 5))
			add(VarInsnNode(ALOAD, 8))
			add(MethodInsnNode(INVOKEVIRTUAL, MethodHandles.Lookup::class.internalName, "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
			add(JumpInsnNode(GOTO, retConstant))
			// ========= If Static =========
			
			// ========= If Virtual/Interface =========
			add(lVirtual)
			add(lInterface)
			add(VarInsnNode(ALOAD, 0))
			add(VarInsnNode(ALOAD, 4))
			add(VarInsnNode(ALOAD, 5))
			add(VarInsnNode(ALOAD, 8))
			add(MethodInsnNode(INVOKEVIRTUAL, MethodHandles.Lookup::class.internalName, "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
			add(JumpInsnNode(GOTO, retConstant))
			// ========= If Virtual/Interface =========*/
			
			add(retConstant)
			add(TypeInsnNode(NEW, ConstantCallSite::class.internalName))
			add(DUP_X1)
			add(SWAP)
			add(MethodInsnNode(INVOKESPECIAL, ConstantCallSite::class.internalName, "<init>", "(Ljava/lang/invoke/MethodHandle;)V"))
			add(InsnNode(ARETURN))
		}
	}
	
	data class MethodCall(val classNode: ClassNode, val methodNode: MethodNode, val insnNode: MethodInsnNode)
}

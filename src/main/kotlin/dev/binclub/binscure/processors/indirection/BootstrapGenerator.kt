package dev.binclub.binscure.processors.indirection

import dev.binclub.binscure.CObfuscator.random
import dev.binclub.binscure.utils.add
import dev.binclub.binscure.utils.internalName
import dev.binclub.binscure.processors.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.invoke.ConstantCallSite
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

/**
 * @author cookiedragon234 13/Feb/2020
 */
fun generateBootstrapMethod(className: String, strDecryptNode: MethodNode, methodNode: MethodNode) {
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
			9 = Switch Control Variable
			10 = MethodHandle
		 */
		val methodStart = newLabel()
		val methodEnd = newLabel()
		val retConstant = newLabel()
		val lStatic = newLabel()
		val lVirtual = newLabel()
		val lInterface = newLabel()
		val catchNullCheck = newLabel()
		val catch = newLabel()
		
		val switchKey = 0//random.nextInt(Integer.MAX_VALUE)
		val minVal = 0//random.nextInt(Integer.MAX_VALUE / 2)
		val switchNode = newLabel()
		val switch0 = newLabel()
		val switch1 = newLabel()
		val switch2 = newLabel()
		val switch3 = newLabel()
		val switch4 = newLabel()
		val switch5 = newLabel()
		val switch6 = newLabel()
		val switch7 = newLabel()
		val switch8 = newLabel()
		val switch9 = newLabel()
		val switch10 = newLabel()
		val switch11 = newLabel()
		val switch12 = newLabel()
		val switch13 = newLabel()
		val switch14 = newLabel()
		
		val switch15 = newLabel()
		val switch16 = newLabel()
		val switch17 = newLabel()
		val switch18 = newLabel()
		val switch19 = newLabel()
		val switch20 = newLabel()
		
		val switch21 = newLabel()
		val switch22 = newLabel()
		val switch23 = newLabel()
		val switch24 = newLabel()
		val switch25 = newLabel()
		val switch26 = newLabel()
		val switch27= newLabel()
		val switch28 = newLabel()
		val switch29 = newLabel()
		
		add(methodStart)
		// ========= Force global var scope =========
		add(ACONST_NULL)
		add(VarInsnNode(ASTORE, 7))
		add(ACONST_NULL)
		add(VarInsnNode(ASTORE, 8))
		add(ACONST_NULL)
		add(VarInsnNode(ASTORE, 10))
		add(ldcInt((7 + minVal) xor switchKey))
		add(VarInsnNode(ISTORE, 9))
		add(JumpInsnNode(GOTO, switchNode))
		// ========= Force global var scope =========
		
		val blocks = arrayListOf(
			InsnList().apply {
				add(switchNode)
				add(VarInsnNode(ILOAD, 9))
				add(ldcInt(switchKey))
				add(IXOR)
				add(
					constructTableSwitch(
					minVal,
					switch4,
					switch0, switch1, switch4, switch3, switch4, switch5, switch6, switch7, switch8, switch9, switch10, switch11, switch12, switch13, switch14,
					switch15, switch16, switch17, switch18, switch19, switch20,
					switch21, switch22, switch23, switch24, switch25, switch26, switch22, switch27, switch28, switch9, switch10, switch29, switch12, switch13, switch14,
					switch0, switch1, switch4, switch3, switch4, switch5, switch6, switch7, switch8, switch9, switch10, switch11, switch12, switch13, switch14,
					switch21, switch22, switch23, switch24, switch25, switch26, switch22, switch27, switch28, switch9, switch10, switch29, switch12, switch13, switch14,
					switch0, switch1, switch4, switch3, switch4, switch5, switch6, switch7, switch8, switch9, switch10, switch11, switch12, switch13, switch14,
					switch21, switch22, switch23, switch24, switch25, switch26, switch22, switch27, switch28, switch9, switch10, switch29, switch12, switch13, switch14,
					switch0, switch1, switch4, switch3, switch4, switch5, switch6, switch7, switch8, switch9, switch10, switch11, switch12, switch13, switch14,
					switch21, switch22, switch23, switch24, switch25, switch26, switch22, switch27, switch28, switch9, switch10, switch29, switch12, switch13, switch14
				)
				)
			},
			InsnList().apply {
				add(switch21)
				add(randomOpaqueJump(switch22))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch22)
				add(randomOpaqueJump(switch21))
				add(JumpInsnNode(GOTO, switch16))
			},
			InsnList().apply {
				add(switch23)
				add(randomOpaqueJump(switch21))
				add(JumpInsnNode(GOTO, switch19))
			},
			InsnList().apply {
				add(switch24)
				add(randomOpaqueJump(switch23))
				add(JumpInsnNode(GOTO, switch23))
			},
			InsnList().apply {
				add(switch25)
				add(randomOpaqueJump(switch24))
				add(JumpInsnNode(GOTO, switch25))
			},
			InsnList().apply {
				add(switch26)
				add(randomOpaqueJump(switch27))
				add(JumpInsnNode(GOTO, switch20))
			},
			InsnList().apply {
				add(switch27)
				add(randomOpaqueJump(switch29))
				add(JumpInsnNode(GOTO, switch7))
			},
			InsnList().apply {
				add(switch28)
				add(JumpInsnNode(GOTO, switch12))
			},
			InsnList().apply {
				add(switch29)
				add(JumpInsnNode(GOTO, switch16))
			},
			InsnList().apply {
				add(switch16)
				add(LdcInsnNode("sun.misc.Unsafe"))
				add(MethodInsnNode(INVOKESTATIC, Class::class.internalName, "forName", "(Ljava/lang/String;)Ljava/lang/Class;"))
				add(VarInsnNode(ASTORE, 7))// Class
				add(ldcInt((3 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch17)
				add(LdcInsnNode(String(ByteArray(15 + random.nextInt(10)).also{random.nextBytes(it)})))
				add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
				if (random.nextBoolean()) {
					add(VarInsnNode(ASTORE, 6))
				} else {
					add(VarInsnNode(ASTORE, 5))
				}
				add(JumpInsnNode(GOTO, switch19))
			},
			InsnList().apply {
				add(switch19)
				add(VarInsnNode(ALOAD, 7))// Class
				add(MethodInsnNode(INVOKEVIRTUAL, Class::class.internalName, "instanceof", "()V"))
				
				add(switch18)
				add(VarInsnNode(ILOAD, 9))
				add(MethodInsnNode(INVOKESTATIC, Class::class.internalName, "forName", "(I)Ljava/lang/Class;"))
				add(DUP)
				add(POP)//add(MONITOREXIT)
				add(VarInsnNode(ASTORE, 7))
				add(JumpInsnNode(GOTO, switch15))
			},
			InsnList().apply {
				add(switch20)
				add(VarInsnNode(ALOAD, 4))
				add(POP)//add(MONITORENTER)
				add(JumpInsnNode(GOTO, switch18))
			},
			InsnList().apply {
				add(switch15)
				add(VarInsnNode(ALOAD, 4)) // Enc CLass Name
				add(ldcInt(random.nextInt(7)))
				add(MethodInsnNode(INVOKEVIRTUAL, String::class.internalName, "substring", "(I)Ljava/lang/String;"))
				add(DUP)
				add(POP)//add(MONITORENTER)
				add(VarInsnNode(ASTORE, 4)) // CLass Name
				add(ldcInt((10 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				// ========= Decrypt Names =========
				add(switch7)
				add(VarInsnNode(ALOAD, 4)) // Enc CLass Name
				add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
				add(DUP)
				add(VarInsnNode(ASTORE, 4)) // CLass Name
				add(MethodInsnNode(INVOKESTATIC, Class::class.internalName, "forName", "(Ljava/lang/String;)Ljava/lang/Class;"))
				add(VarInsnNode(ASTORE, 7))// Class
				add(ldcInt((10 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch10)
				add(VarInsnNode(ALOAD, 5)) // Enc Method Name
				add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
				add(VarInsnNode(ASTORE, 5)) // Method Name
				add(ldcInt((1 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch1)
				add(switch2)
				add(VarInsnNode(ALOAD, 6)) // Enc Method Sig
				add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
				add(VarInsnNode(ASTORE, 6)) // Method Sig
				add(ldcInt((6 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
				// ========= Decrypt Names =========
			},
			InsnList().apply {
				// ========= Get Method Type =========
				add(switch13)
				//add(ACONST_NULL)
				//add(JumpInsnNode(GOTO, catch))
				add(switch6)
				add(VarInsnNode(ALOAD, 6)) // Method Sig
				add(LdcInsnNode(Type.getType("L$className;")))
				add(MethodInsnNode(INVOKEVIRTUAL, Class::class.internalName, "getClassLoader", "()Ljava/lang/ClassLoader;")) // Class Loader
				add(MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;"))
				add(VarInsnNode(ASTORE, 8)) // Real Method Type
				add(ldcInt((0 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
				// ========= Get Method Type =========
			},
			InsnList().apply {
				// ========= If Statement =========
				add(switch0)
				add(VarInsnNode(ILOAD, 3)) // Opcode
				add(ldcInt(INVOKESTATIC))
				add(JumpInsnNode(IF_ICMPEQ, lStatic))
				add(ldcInt((3 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch3)
				add(VarInsnNode(ILOAD, 3)) // Opcode
				add(ldcInt(INVOKEVIRTUAL))
				add(JumpInsnNode(IF_ICMPEQ, lVirtual))
				add(ldcInt((8 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch8)
				add(VarInsnNode(ILOAD, 3)) // Opcode
				add(ldcInt(INVOKEINTERFACE))
				add(JumpInsnNode(IF_ICMPEQ, lInterface))
				add(ldcInt((11 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch11)
				add(ACONST_NULL)
				add(ATHROW)
				// ========= If Statement =========
			},
			InsnList().apply {
				// ========= If Static =========
				add(lStatic)
				add(ldcInt((9 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
				add(switch9)
				add(VarInsnNode(ALOAD, 0))
				add(VarInsnNode(ALOAD, 7))
				add(VarInsnNode(ALOAD, 5))
				add(VarInsnNode(ALOAD, 8))
				add(MethodInsnNode(INVOKEVIRTUAL, MethodHandles.Lookup::class.internalName, "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
				add(JumpInsnNode(GOTO, retConstant))
				// ========= If Static =========
			},
			InsnList().apply {
				// ========= If Virtual/Interface =========
				add(lVirtual)
				add(lInterface)
				add(VarInsnNode(ALOAD, 0))
				add(VarInsnNode(ALOAD, 7))
				add(VarInsnNode(ALOAD, 5))
				add(VarInsnNode(ALOAD, 8))
				add(MethodInsnNode(INVOKEVIRTUAL, MethodHandles.Lookup::class.internalName, "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
				add(VarInsnNode(ASTORE, 10))
				add(ldcInt((14 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
				// ========= If Virtual/Interface =========
			},
			InsnList().apply {
				add(switch14)
				add(VarInsnNode(ALOAD, 10))
				add(retConstant)
				//add(DUP)
				//add(JumpInsnNode(IFNULL, catch))
				add(VarInsnNode(ALOAD, 2))
				add(MethodInsnNode(INVOKEVIRTUAL, MethodHandle::class.internalName, "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
				add(VarInsnNode(ASTORE, 10))
				add(ldcInt((12 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch12)
				add(TypeInsnNode(NEW, ConstantCallSite::class.internalName))
				add(DUP)
				add(VarInsnNode(ALOAD, 10))
				add(MethodInsnNode(INVOKESPECIAL, ConstantCallSite::class.internalName, "<init>", "(Ljava/lang/invoke/MethodHandle;)V"))
				
				add(InsnNode(ARETURN))
			},
			InsnList().apply {
				add(switch5)
				add(ACONST_NULL)
				add(DUP)
				add(POP)//add(MONITORENTER)
				add(catch)
				add(DUP)
				add(DUP)
				add(JumpInsnNode(IFNULL, catchNullCheck))
				add(POP)//add(MONITORENTER)
				add(MethodInsnNode(INVOKESTATIC, String::class.internalName, "null", "(Ljava/lang/Object;)I"))
				add(DUP)
				add(VarInsnNode(ISTORE, 9))
				add(POP)//add(MONITOREXIT)
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(catchNullCheck)
				add(POP)//add(MONITOREXIT)
				add(POP)//add(MONITORENTER)
				add(ldcInt((4 + minVal) xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch4)
				add(ACONST_NULL)
				add(ATHROW)
			}
		)//.shuffled(random)
		
		for (block in blocks) {
			add(block)
		}
		add(methodEnd)
		
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
			9 = Switch Control Variable
			10 = MethodHandle
		 
		methodNode.localVariables = arrayListOf(
			LocalVariableNode("a", "Ljava/lang/invoke/MethodHandles\$Lookup;", null, methodStart, methodEnd, 0),
			LocalVariableNode("b", "Ljava/lang/String;", null, methodStart, methodEnd, 1),
			LocalVariableNode("c", "Ljava/lang/invoke/MethodType;", null, methodStart, methodEnd, 2),
			LocalVariableNode("d", "I", null, methodStart, methodEnd, 3),
			LocalVariableNode("e", "Ljava/lang/String;", null, methodStart, methodEnd, 4),
			LocalVariableNode("f", "Ljava/lang/String;", null, methodStart, methodEnd, 5),
			LocalVariableNode("g", "Ljava/lang/String;", null, methodStart, methodEnd, 6),
			LocalVariableNode("h", "Ljava/lang/Class;", null, methodStart, methodEnd, 7),
			LocalVariableNode("i", "Ljava/lang/invoke/MethodType;", null, methodStart, methodEnd, 8),
			LocalVariableNode("j", "I", null, methodStart, methodEnd, 9),
			LocalVariableNode("k", "\"Ljava/lang/invoke/MethodHandle", null, methodStart, methodEnd, 10)
		)*/
	}
}

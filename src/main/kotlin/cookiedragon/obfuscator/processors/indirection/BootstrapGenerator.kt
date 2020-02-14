package cookiedragon.obfuscator.processors.indirection

import cookiedragon.obfuscator.CObfuscator.random
import cookiedragon.obfuscator.kotlin.add
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Label
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
		val retConstant = LabelNode(Label())
		val lStatic = LabelNode(Label())
		val lVirtual = LabelNode(Label())
		val lInterface = LabelNode(Label())
		val catchNullCheck = LabelNode(Label())
		val catch = LabelNode(Label())
		
		val switchKey = random.nextInt(50)
		val switchNode = LabelNode(Label())
		val switch0 = LabelNode(Label())
		val switch1 = LabelNode(Label())
		val switch3 = LabelNode(Label())
		val switch4 = LabelNode(Label())
		val switch5 = LabelNode(Label())
		val switch6 = LabelNode(Label())
		val switch7 = LabelNode(Label())
		val switch8 = LabelNode(Label())
		val switch9 = LabelNode(Label())
		val switch10 = LabelNode(Label())
		val switch11 = LabelNode(Label())
		val switch12 = LabelNode(Label())
		val switch13 = LabelNode(Label())
		val switch14 = LabelNode(Label())
		
		val switch15 = LabelNode(Label())
		val switch16 = LabelNode(Label())
		val switch17 = LabelNode(Label())
		val switch18 = LabelNode(Label())
		val switch19 = LabelNode(Label())
		val switch20 = LabelNode(Label())
		
		// ========= Force global var scope =========
		add(ACONST_NULL)
		add(VarInsnNode(ASTORE, 7))
		add(ACONST_NULL)
		add(VarInsnNode(ASTORE, 8))
		add(ACONST_NULL)
		add(VarInsnNode(ASTORE, 10))
		add(ldcInt(7 xor switchKey))
		add(VarInsnNode(ISTORE, 9))
		add(JumpInsnNode(GOTO, switchNode))
		// ========= Force global var scope =========
		
		val blocks = arrayListOf(
			InsnList().apply {
				add(switchNode)
				add(VarInsnNode(ILOAD, 9))
				add(ldcInt(switchKey))
				add(IXOR)
				add(TableSwitchInsnNode(
					0,
					20,
					switch4,
					switch0, switch1, switch4, switch3, switch4, switch5, switch6, switch7, switch8, switch9, switch10, switch11, switch12, switch13, switch14,
					switch15, switch16, switch17, switch18, switch19, switch20
				))
			},
			InsnList().apply {
				add(switch16)
				add(LdcInsnNode("sun.misc.Unsafe"))
				add(MethodInsnNode(INVOKESTATIC, Class::class.internalName, "forName", "(Ljava/lang/String;)Ljava/lang/Class;"))
				add(VarInsnNode(ASTORE, 7))// Class
				add(ldcInt(3 xor switchKey))
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
			},
			InsnList().apply {
				add(switch19)
				add(VarInsnNode(ALOAD, 7))// Class
				add(MethodInsnNode(INVOKEVIRTUAL, Class::class.internalName, "instanceof", "()V"))
				
				add(switch18)
				add(VarInsnNode(ILOAD, 9))
				add(MethodInsnNode(INVOKESTATIC, Class::class.internalName, "forName", "(I)Ljava/lang/Class;"))
				add(DUP)
				add(MONITOREXIT)
				add(VarInsnNode(ASTORE, 7))
			},
			InsnList().apply {
				add(switch20)
				add(VarInsnNode(ALOAD, 4))
				add(MONITORENTER)
				add(JumpInsnNode(GOTO, switch18))
			},
			InsnList().apply {
				add(switch15)
				add(VarInsnNode(ALOAD, 4)) // Enc CLass Name
				add(ldcInt(random.nextInt(7)))
				add(MethodInsnNode(INVOKEVIRTUAL, String::class.internalName, "substring", "(I)Ljava/lang/String;"))
				add(DUP)
				add(MONITORENTER)
				add(VarInsnNode(ASTORE, 4)) // CLass Name
				add(ldcInt(10 xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				// ========= Decrypt Names =========
				add(switch7)
				add(VarInsnNode(ALOAD, 4)) // Enc CLass Name
				add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
				add(VarInsnNode(ASTORE, 4)) // CLass Name
				add(ldcInt(10 xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch10)
				add(VarInsnNode(ALOAD, 5)) // Enc Method Name
				add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
				add(VarInsnNode(ASTORE, 5)) // Method Name
				add(ldcInt(1 xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch1)
				add(VarInsnNode(ALOAD, 6)) // Enc Method Sig
				add(MethodInsnNode(INVOKESTATIC, className, strDecryptNode.name, strDecryptNode.desc))
				add(VarInsnNode(ASTORE, 6)) // Method Sig
				add(ldcInt(6 xor switchKey))
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
				add(ldcInt(0 xor switchKey))
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
				add(ldcInt(3 xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch3)
				add(VarInsnNode(ILOAD, 3)) // Opcode
				add(ldcInt(INVOKEVIRTUAL))
				add(JumpInsnNode(IF_ICMPEQ, lVirtual))
				add(ldcInt(8 xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch8)
				add(VarInsnNode(ILOAD, 3)) // Opcode
				add(ldcInt(INVOKEINTERFACE))
				add(JumpInsnNode(IF_ICMPEQ, lInterface))
				add(ldcInt(11 xor switchKey))
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
				add(ldcInt(9 xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
				add(switch9)
				add(VarInsnNode(ALOAD, 0))
				add(VarInsnNode(ALOAD, 7))
				add(VarInsnNode(ALOAD, 5))
				add(VarInsnNode(ALOAD, 8))
				add(MethodInsnNode(INVOKEVIRTUAL, MethodHandles.Lookup::class.internalName, "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
				add(VarInsnNode(ASTORE, 10))
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
				add(ldcInt(14 xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
				// ========= If Virtual/Interface =========
			},
			InsnList().apply {
				add(switch14)
				add(retConstant)
				add(VarInsnNode(ALOAD, 10))
				add(DUP)
				add(JumpInsnNode(IFNULL, catch))
				add(VarInsnNode(ALOAD, 2))
				add(MethodInsnNode(INVOKEVIRTUAL, MethodHandle::class.internalName, "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"))
				add(VarInsnNode(ASTORE, 10))
				add(ldcInt(12 xor switchKey))
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
				add(catch)
				add(DUP)
				add(JumpInsnNode(IFNULL, catchNullCheck))
				add(MethodInsnNode(INVOKESTATIC, String::class.internalName, "null", "(Ljava/lang/Object;)I"))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(catchNullCheck)
				add(POP)
				add(ldcInt(4 xor switchKey))
				add(VarInsnNode(ISTORE, 9))
				add(JumpInsnNode(GOTO, switchNode))
			},
			InsnList().apply {
				add(switch4)
				add(ACONST_NULL)
				add(ATHROW)
			}
		).shuffled(random)
		
		for (block in blocks) {
			add(block)
		}
	}
}

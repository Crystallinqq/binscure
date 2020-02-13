package cookiedragon.obfuscator.processors.indirection

import cookiedragon.obfuscator.CObfuscator.random
import cookiedragon.obfuscator.kotlin.add
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.utils.ldcInt
import cookiedragon.obfuscator.utils.randomBranch
import cookiedragon.obfuscator.utils.randomThrowable
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 12/Feb/2020
 */

fun generateDecryptorMethod(classNode: ClassNode, methodNode: MethodNode) {
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
		val catch = LabelNode(Label())
		val catch2 = LabelNode(Label())
		val start = LabelNode(Label())
		val end = LabelNode(Label())
		val pre15 = LabelNode(Label())
		
		add(Opcodes.ACONST_NULL)
		add(VarInsnNode(Opcodes.ASTORE, 1))
		add(Opcodes.ACONST_NULL)
		add(VarInsnNode(Opcodes.ASTORE, 2))
		add(Opcodes.ACONST_NULL)
		add(VarInsnNode(Opcodes.ASTORE, 3))
		add(Opcodes.ACONST_NULL)
		add(VarInsnNode(Opcodes.ASTORE, 4))
		add(Opcodes.ICONST_M1)
		add(VarInsnNode(Opcodes.ISTORE, 5))
		add(Opcodes.ICONST_M1)
		add(VarInsnNode(Opcodes.ISTORE, 6))
		add(Opcodes.ICONST_M1)
		add(VarInsnNode(Opcodes.ISTORE, 7))
		
		
		
		val rootSwitch = LabelNode(Label())
		
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
		
		add(JumpInsnNode(Opcodes.GOTO, start))
		
		val jumpLabels = mutableSetOf<LabelNode>()
		
		val blocks = arrayListOf(
			InsnList().apply {
				add(start.also{jumpLabels.add(it)})
				add(ldcInt(7))
				add(rootSwitch)
				add(
					TableSwitchInsnNode(
						0, // Min
						18, // Max
						throwNull, // Default
						tble0, tble1, tble2, tble3, tble4, switch5, switch6, switch7, switch8, switch9, switch10, switch11, switch12, switch13,
						switch14, switch15, switch16, switch17, switch18
					)
				)
			},
			InsnList().apply {
				add(switch7.also{jumpLabels.add(it)})
				// First we need to decrypt the method description stored at local var 1
				// We will turn it into a char array
				add(VarInsnNode(Opcodes.ALOAD, 0))
				add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false))
				add(VarInsnNode(Opcodes.ASTORE, 1))
				add(ldcInt(9))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch9.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 1))
				// Find the array length and create our decrypted char array (store in slot 4)
				add(Opcodes.ARRAYLENGTH)
				add(IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_CHAR))
				add(VarInsnNode(Opcodes.ASTORE, 2))
				add(ldcInt(6))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch6.also{jumpLabels.add(it)})
				// Get the class and method hash
				add(getThread)
				add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false))
				add(VarInsnNode(Opcodes.ASTORE, 3)) // Stored in var 7
				add(ldcInt(8))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch8.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 3))
				add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false))
				add(ldcInt(6))
				add(Opcodes.AALOAD)
				add(VarInsnNode(Opcodes.ASTORE, 4))
				add(ldcInt(5))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch5.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 4))
				add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, StackTraceElement::class.internalName, "getClassName", "()Ljava/lang/String;", false))
				add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
				add(VarInsnNode(Opcodes.ISTORE, 5))
				add(ldcInt(11))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch11.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 4))
				add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, StackTraceElement::class.internalName, "getMethodName", "()Ljava/lang/String;", false))
				add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false))
				add(VarInsnNode(Opcodes.ISTORE, 6))
				add(ldcInt(10))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				// Now loop over our new array
				add(switch10.also{jumpLabels.add(it)})
				add(ldcInt(0))
				add(VarInsnNode(Opcodes.ISTORE, 7))
				add(ldcInt(12))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch12.also{jumpLabels.add(it)})
				add(loopStart)
				add(VarInsnNode(Opcodes.ILOAD, 7))
				add(VarInsnNode(Opcodes.ALOAD, 2))
				add(Opcodes.ARRAYLENGTH)
				add(JumpInsnNode(Opcodes.IF_ICMPGE, exitLoop)) // If at the end of the loop go to exit
				add(ldcInt(13))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch13.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ILOAD, 7))
				add(ldcInt(5))
				add(Opcodes.IREM)
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(tble0.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 1)) // Encrypted Char Array
				add(VarInsnNode(Opcodes.ILOAD, 7)) // index
				add(Opcodes.CALOAD)
				add(ldcInt(2))
				add(Opcodes.IXOR)
				add(JumpInsnNode(Opcodes.GOTO, setCharArrVal))
			},
			InsnList().apply {
				add(tble1.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 1)) // Encrypted Char Array
				add(VarInsnNode(Opcodes.ILOAD, 7)) // index
				add(Opcodes.CALOAD)
				add(VarInsnNode(Opcodes.ILOAD, 5)) // Class Hash
				add(Opcodes.IXOR)
				add(JumpInsnNode(Opcodes.GOTO, setCharArrVal))
			},
			InsnList().apply {
				add(tble2.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 1)) // Encrypted Char Array
				add(VarInsnNode(Opcodes.ILOAD, 7)) // index
				add(Opcodes.CALOAD)
				add(VarInsnNode(Opcodes.ILOAD, 6)) // method Hash
				add(Opcodes.IXOR)
				add(JumpInsnNode(Opcodes.GOTO, setCharArrVal))
			},
			InsnList().apply {
				add(tble3.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 1)) // Encrypted Char Array
				add(VarInsnNode(Opcodes.ILOAD, 7)) // index
				add(Opcodes.CALOAD)
				add(VarInsnNode(Opcodes.ILOAD, 5)) // Class Hash
				add(VarInsnNode(Opcodes.ILOAD, 6)) // method Hash
				add(Opcodes.IADD)
				add(Opcodes.IXOR)
				add(JumpInsnNode(Opcodes.GOTO, setCharArrVal))
			},
			InsnList().apply {
				add(tble4.also{jumpLabels.add(it)})
				add(VarInsnNode(Opcodes.ALOAD, 1)) // Encrypted Char Array
				add(VarInsnNode(Opcodes.ILOAD, 7)) // index
				add(Opcodes.CALOAD)
				add(VarInsnNode(Opcodes.ILOAD, 7)) // index
				add(Opcodes.IXOR)
				add(JumpInsnNode(Opcodes.GOTO, setCharArrVal))
			},
			InsnList().apply {
				add(setCharArrVal)
				add(Opcodes.I2C)
				add(VarInsnNode(Opcodes.ALOAD, 1)) // Decrypted Char Array
				add(Opcodes.SWAP)
				add(VarInsnNode(Opcodes.ILOAD, 7)) // Index
				add(Opcodes.SWAP)
				add(Opcodes.CASTORE)
				add(ldcInt(16))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch16.also{jumpLabels.add(it)})
				add(ldcInt(18))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch18.also{jumpLabels.add(it)})
				add(ldcInt(14))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch14.also{jumpLabels.add(it)})
				add(ldcInt(17))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(switch17.also{jumpLabels.add(it)})
				// Increment and go to top of loop
				add(IincInsnNode(7, 1))
				add(JumpInsnNode(Opcodes.GOTO, loopStart))
			},
			InsnList().apply {
				// If we are here then we have a decrypted char array in slot 4
				add(exitLoop.also{jumpLabels.add(it)})
				add(TypeInsnNode(Opcodes.NEW, "java/lang/String"))
				add(Opcodes.DUP)
				add(VarInsnNode(Opcodes.ALOAD, 1)) // Decrypted Char Array
				add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"))
				add(Opcodes.ARETURN)
			},
			InsnList().apply {
				add(throwNull.also{jumpLabels.add(it)})
				add(ldcInt(15))
				add(JumpInsnNode(Opcodes.GOTO, rootSwitch))
			},
			InsnList().apply {
				add(catch.also{jumpLabels.add(it)})
				add(Opcodes.DUP)
				add(JumpInsnNode(Opcodes.IFNULL, pre15))
				add(Opcodes.ATHROW)
			},
			InsnList().apply {
				add(catch2.also{jumpLabels.add(it)})
				add(Opcodes.DUP)
				add(JumpInsnNode(Opcodes.IFNONNULL, pre15))
				add(Opcodes.ATHROW)
			},
			InsnList().apply {
				add(pre15.also{jumpLabels.add(it)})
				add(Opcodes.POP)
				add(switch15)
				add(Opcodes.ACONST_NULL)
				add(Opcodes.ATHROW)
				add(end)
			}
		).shuffled(random)
		
		for (block in blocks) {
			add(block)
		}
		
		val labels = arrayListOf<LabelNode>()
		
		for (insn in this) {
			if (insn is LabelNode) {
				labels.add(insn)
			}
		}
		
		classNode.methods.add(methodNode)
		
		if (labels.size > 3) {
			do {
				methodNode.tryCatchBlocks.clear()
				for (i in 0 until labels.size / 2) {
					var randIndex: Int
					var nextIndex: Int
					var startLabel: LabelNode
					var endLabel: LabelNode
					do {
						randIndex = random.nextInt(labels.size - 1)
						nextIndex = randIndex + random.nextInt(labels.size - randIndex)
						
						startLabel = labels[randIndex]
						endLabel = labels[nextIndex]
					} while (nextIndex - randIndex < labels.size / 7)// && jumpLabels.contains(startLabel) && jumpLabels.contains(endLabel))
					
					randomBranch(
						random, {
							methodNode.tryCatchBlocks.add(TryCatchBlockNode(startLabel, endLabel, catch, randomThrowable()))
						}, {
							methodNode.tryCatchBlocks.add(TryCatchBlockNode(startLabel, endLabel, pre15, randomThrowable()))
						}, {
							methodNode.tryCatchBlocks.add(TryCatchBlockNode(startLabel, endLabel, catch2, randomThrowable()))
						}
					)
				}
			} while (!verifyNode(classNode))
		}
	}
}

private fun verifyNode(classNode: ClassNode): Boolean {
	try {
		val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
		classNode.accept(writer)
		val reader = ClassReader(writer.toByteArray()).accept(ClassNode(), ClassReader.EXPAND_FRAMES)
		return true
	} catch (e: Exception) {
		e.printStackTrace()
		return false
	}
}

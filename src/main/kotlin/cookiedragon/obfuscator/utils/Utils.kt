package cookiedragon.obfuscator.utils

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.kotlin.add
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.kotlin.random
import cookiedragon.obfuscator.kotlin.toInsn
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.io.PrintStream
import java.security.SecureRandom


/**
 * @author cookiedragon234 21/Jan/2020
 */
fun ldcInt(int: Int): AbstractInsnNode {
	return if (int == -1) {
		InsnNode(ICONST_M1)
	} else if (int == 0) {
		InsnNode(ICONST_0)
	} else if (int == 1) {
		InsnNode(ICONST_1)
	} else if (int == 2) {
		InsnNode(ICONST_2)
	} else if (int == 3) {
		InsnNode(ICONST_3)
	} else if (int == 4) {
		InsnNode(ICONST_4)
	} else if (int == 5) {
		InsnNode(ICONST_5)
	} else if (int >= -128 && int <= 127) {
		IntInsnNode(BIPUSH, int)
	} else if (int >= -32768 && int <= 32767) {
		IntInsnNode(SIPUSH, int)
	} else {
		LdcInsnNode(int)
	}
}

fun ldcLong(long: Long): AbstractInsnNode {
	return when (long) {
		0L -> InsnNode(LCONST_0)
		1L -> InsnNode(LCONST_1)
		else -> LdcInsnNode(long)
	}
}

fun ldcDouble(double: Double): AbstractInsnNode {
	return when (double) {
		0.0 -> InsnNode(DCONST_0)
		1.0 -> InsnNode(DCONST_1)
		else -> LdcInsnNode(double)
	}
}

fun ldcFloat(float: Float): AbstractInsnNode {
	return when (float) {
		0f -> InsnNode(FCONST_0)
		1f -> InsnNode(FCONST_1)
		2f -> InsnNode(FCONST_2)
		else -> LdcInsnNode(float)
	}
}

val throwables = arrayOf(
	RuntimeException::class.internalName,
	ArithmeticException::class.internalName,
	ArrayIndexOutOfBoundsException::class.internalName,
	ClassCastException::class.internalName,
	EnumConstantNotPresentException::class.internalName,
	IllegalArgumentException::class.internalName,
	NegativeArraySizeException::class.internalName,
	StringIndexOutOfBoundsException::class.internalName,
	IllegalStateException::class.internalName,
	IndexOutOfBoundsException::class.internalName,
	UnsupportedOperationException::class.internalName,
	NumberFormatException::class.internalName,
	NullPointerException::class.internalName,
	AssertionError::class.internalName,
	NoSuchElementException::class.internalName,
	ConcurrentModificationException::class.internalName
)

fun randomThrowable(nonNull: Boolean = false): String? = if (CObfuscator.random.nextBoolean() || nonNull) throwables.random(CObfuscator.random) else null

val throwableActions = arrayOf(
	InsnList().apply {
		add(InsnNode(ATHROW))
	},
	InsnList().apply {
		add(InsnNode(ACONST_NULL))
		add(InsnNode(ATHROW))
	},
	InsnList().apply {
		add(InsnNode(ACONST_NULL))
		add(InsnNode(ATHROW))
	}
)

val numOps = mapOf<Int, Number>(
	ICONST_M1 to -1,
	ICONST_0 to 0,
	ICONST_1 to 1,
	ICONST_2 to 2,
	ICONST_3 to 3,
	ICONST_4 to 4,
	ICONST_5 to 5,
	
	LCONST_0 to 0L,
	LCONST_1 to 1L,
	
	FCONST_0 to 0f,
	FCONST_1 to 1f,
	FCONST_2 to 2f,
	
	DCONST_0 to 0.0,
	DCONST_1 to 1.0,
	
	BIPUSH to -1,
	SIPUSH to -1
)

fun isNumberLdc(insn: AbstractInsnNode): Boolean = numOps.contains(insn.opcode) || (insn is LdcInsnNode && insn.cst is Number)

fun getNumFromLdc(insn: AbstractInsnNode): Number {
	return if (insn is LdcInsnNode && insn.cst is Number) {
		insn.cst as Number
	} else if (insn is IntInsnNode) {
		insn.operand
	} else {
		numOps[insn.opcode] ?: throw IllegalStateException()
	}
}

fun randomBranch(random: SecureRandom, vararg blocks: Int.() -> Unit) {
	val choice = random.nextInt(blocks.size)
	blocks.elementAt(choice).invoke(choice)
}

fun randomBranchExcluding(random: SecureRandom, exclusion: MutableInteger, vararg blocks: Int.() -> Unit) {
	var choice: Int
	do {
		choice = random.nextInt(blocks.size)
	} while (exclusion.equals(choice))
	exclusion.value = choice
	blocks.elementAt(choice).invoke(choice)
}

data class MutableInteger(var value: Int) {
	fun equals(v2: Int) = value == v2
}

fun printlnAsm(): InsnList {
	return InsnList().apply {
		add(FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
		add(SWAP)
		add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/String;)V"))
	}
}

fun printlnAsm(text: String): InsnList {
	return InsnList().also {
		it.add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "Ljava/io/PrintStream;"))
		it.add(LdcInsnNode(text))
		it.add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/String;)V"))
	}
}

fun printlnIntAsm(): InsnList {
	return InsnList().also {
		it.add(FieldInsnNode(GETSTATIC, System::class.internalName, "out", "Ljava/io/PrintStream;"))
		it.add(InsnNode(SWAP))
		it.add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(I)V"))
	}
}

val staticInvokes = arrayOf(
	Handle(INVOKESTATIC, Runtime::class.internalName, "getRuntime", "()Ljava/lang/Runtime;"),
	Handle(INVOKESTATIC, Thread::class.internalName, "currentThread", "()Ljava/lang/Thread;"),
	Handle(INVOKESTATIC, System::class.internalName, "console", "()Ljava/io/Console;"),
	Handle(INVOKESTATIC, System::class.internalName, "lineSeparator", "()Ljava/lang/String;"),
	Handle(INVOKESTATIC, System::class.internalName, "lineSeparator", "()Ljava/lang/String;")
)

fun randomStaticInvoke(): MethodInsnNode = staticInvokes.random(CObfuscator.random).toInsn()

fun insnListOf(vararg insns: AbstractInsnNode) = InsnList().also {
	for (insn in insns) {
		it.add(insn)
	}
}

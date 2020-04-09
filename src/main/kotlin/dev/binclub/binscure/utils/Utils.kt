package dev.binclub.binscure.utils

import dev.binclub.binscure.CObfuscator.random
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.EXPAND_FRAMES
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.util.CheckClassAdapter
import java.io.PrintStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
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

fun randomThrowable(nonNull: Boolean = false): String? =
	if (random.nextBoolean() || nonNull) throwables.random(
		random
	) else null

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

fun isNumberLdc(insn: AbstractInsnNode): Boolean =
	numOps.contains(insn.opcode) || (insn is LdcInsnNode && insn.cst is Number)

fun getNumFromLdc(insn: AbstractInsnNode): Number {
	return if (insn is LdcInsnNode && insn.cst is Number) {
		insn.cst as Number
	} else if (insn is IntInsnNode) {
		insn.operand
	} else {
		numOps[insn.opcode] ?: throw IllegalStateException()
	}
}

fun <T> randomBranch(random: SecureRandom, vararg blocks: (Int) -> T): T {
	val choice = random.nextInt(blocks.size)
	return blocks.elementAt(choice).invoke(choice)
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
		add(MethodInsnNode(INVOKEVIRTUAL, PrintStream::class.internalName, "println", "(Ljava/lang/Object;)V"))
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

fun randomStaticInvoke(): MethodInsnNode = staticInvokes.random(random).toInsn()

fun getRetForType(type: Type): Int {
	return when (type) {
		Type.VOID_TYPE -> RETURN
		Type.BOOLEAN_TYPE, Type.CHAR_TYPE, Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE -> IRETURN
		Type.FLOAT_TYPE -> FRETURN
		Type.LONG_TYPE -> LRETURN
		Type.DOUBLE_TYPE -> DRETURN
		else -> ARETURN
	}
}

fun getLoadForType(type: Type): Int {
	return when (type) {
		Type.VOID_TYPE -> throw IllegalArgumentException("Cannot load Void $type")
		Type.BOOLEAN_TYPE, Type.CHAR_TYPE, Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE -> ILOAD
		Type.FLOAT_TYPE -> FLOAD
		Type.LONG_TYPE -> LLOAD
		Type.DOUBLE_TYPE -> DLOAD
		else -> ALOAD
	}
}

fun verifyClass(classNode: ClassNode) {
	val writer = ClassWriter(COMPUTE_FRAMES)
	classNode.accept(writer)
	val bytes = writer.toByteArray()
	val reader = ClassReader(bytes)
	reader.accept(CheckClassAdapter(EmptyClassVisitor), EXPAND_FRAMES)
}

fun verifyMethodNode(methodNode: MethodNode) {
	val writer = ClassWriter(COMPUTE_FRAMES)
	val classNode = ClassNode().apply {
		version = V1_8
		access = ACC_PUBLIC
		name = "a"
		signature = null
		superName = "java/lang/Object"
		interfaces = arrayListOf()
	}
	classNode.accept(writer)
	val bytes = writer.toByteArray()
	val reader = ClassReader(bytes)
	reader.accept(CheckClassAdapter(EmptyClassVisitor), EXPAND_FRAMES)
}

fun constructTableSwitch(
	baseNumber: Int,
	defaultLabel: LabelNode,
	vararg targetLabels: LabelNode
): TableSwitchInsnNode {
	return TableSwitchInsnNode(
		baseNumber,
		baseNumber + targetLabels.size - 1,
		defaultLabel,
		*targetLabels
	)
}

fun constructLookupSwitch(
	defaultLabel: LabelNode,
	lookup: Array<Pair<Int, LabelNode>>
): LookupSwitchInsnNode {
	lookup.sortWith(Comparator { a, b -> a.first.compareTo(b.first) })
	
	val keys = lookup.map {
		it.first
	}.toIntArray()
	
	val values = lookup.map {
		it.second
	}.toTypedArray()
	
	return LookupSwitchInsnNode(
		defaultLabel,
		keys,
		values
	)
}

fun newLabel(): LabelNode = BlameableLabelNode()

fun randomInt() = if (random.nextBoolean()) random.nextInt(Integer.MAX_VALUE) else -random.nextInt(Integer.MAX_VALUE)
fun randomLong() = if (random.nextBoolean()) random.nextLong() else -random.nextLong()

fun getClinit(classNode: ClassNode): MethodNode {
	for (method in classNode.methods) {
		if (method.name == "<clinit>" && method.desc == "()V") {
			return method
		}
	}
	
	return MethodNode(ACC_STATIC, "<clinit>", "()V", null, null).apply {
		classNode.methods.add(this)
		instructions = InsnList().apply {
			add(RETURN)
		}
	}
}

infix fun Float.xor(b: Float) =
	java.lang.Float.intBitsToFloat(java.lang.Float.floatToIntBits(this) xor java.lang.Float.floatToIntBits(b))

infix fun Double.xor(b: Double) =
	java.lang.Double.longBitsToDouble(java.lang.Double.doubleToLongBits(this) xor java.lang.Double.doubleToLongBits(b))

fun genericType(type: Type): Type {
	return when (type.sort) {
		Type.OBJECT -> Type.getType(Any::class.java)
		else -> type
	}
}

@Throws(Exception::class)
fun Field.setFinalStatic(newValue: Any?) {
	isAccessible = true
	val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
	modifiersField.isAccessible = true
	modifiersField.setInt(this, modifiers and Modifier.FINAL.inv())
	this.set(null, newValue)
}

fun insnListOf(vararg insns: AbstractInsnNode) = InsnList().apply {
	for (insn in insns) add(insn)
}

package cookiedragon.obfuscator.utils

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.kotlin.random
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode


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

val throwables = arrayOf(
	RuntimeException::class.internalName,
	ArithmeticException::class.internalName,
	ArrayIndexOutOfBoundsException::class.internalName,
	ClassCastException::class.internalName,
	EnumConstantNotPresentException::class.internalName,
	IllegalArgumentException::class.internalName,
	NegativeArraySizeException::class.internalName,
	StringIndexOutOfBoundsException::class.internalName,
	null,
	null,
	null,
	null,
	null
)

fun randomThrowable(): String? = throwables.random(CObfuscator.random)

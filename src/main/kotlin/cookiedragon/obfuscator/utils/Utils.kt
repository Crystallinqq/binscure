package cookiedragon.obfuscator.utils

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.Opcodes.*
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

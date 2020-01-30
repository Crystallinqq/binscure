package cookiedragon.obfuscator.processors.constants

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode

/**
 * @author cookiedragon234 30/Jan/2020
 */
object NumberObfuscation: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in CObfuscator.getProgressBar("Obfuscating Numbers").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method))
					continue
				
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (isNumberLdc(insn)) {
						val num = getNumFromLdc(insn)
						println("Found num ${classNode.name}.${method.name} : $num")
						
						if (num is Int) {
							val randNumbers = randomIntArray()
							val list = InsnList()
							var newNum = num as Int
							for (randNumber in randNumbers) {
								newNum = newNum xor randNumber
								list.add(ldcInt(randNumber))
								list.add(InsnNode(IXOR))
							}
							list.insert(ldcInt(newNum))
							modifier.replace(insn, list)
						} else if (num is Long) {
							val randNumbers = randomLongArray()
							val list = InsnList()
							var newNum = num as Long
							for (randNumber in randNumbers) {
								newNum = newNum xor randNumber
								list.add(ldcLong(randNumber))
								list.add(InsnNode(LXOR))
							}
							list.insert(ldcLong(newNum))
							modifier.replace(insn, list)
						} else if (num is Double) {
							val randNumbers = randomDoubleArray()
							val list = InsnList()
							var newNum = num as Double
							for (randNumber in randNumbers) {
								if (getRandom().nextBoolean()) {
									newNum += randNumber
									list.add(ldcDouble(randNumber))
									list.add(InsnNode(DSUB))
								} else {
									newNum -= randNumber
									list.add(ldcDouble(randNumber))
									list.add(InsnNode(DADD))
								}
							}
							list.insert(ldcDouble(newNum))
							modifier.replace(insn, list)
						} else if (num is Float) {
							val randNumbers = randomFloatArray()
							val list = InsnList()
							var newNum = num as Float
							for (randNumber in randNumbers) {
								if (getRandom().nextBoolean()) {
									newNum += randNumber
									list.add(ldcFloat(randNumber))
									list.add(InsnNode(FSUB))
								} else {
									newNum -= randNumber
									list.add(ldcFloat(randNumber))
									list.add(InsnNode(FADD))
								}
							}
							list.insert(ldcFloat(newNum))
							modifier.replace(insn, list)
						}
					}
				}
				modifier.apply(method)
			}
		}
	}
	
	fun randomIntArray(): IntArray = IntArray( getRandom().nextInt(7)).apply {
		for (i in 0 until size) {
			this[i] = getRandom().nextInt(Integer.MAX_VALUE)
			if (getRandom().nextBoolean()) {
				this[i] = -this[i]
			}
		}
	}
	fun randomLongArray(): LongArray = LongArray( getRandom().nextInt(7)).apply {
		for (i in 0 until size) {
			this[i] = getRandom().nextLong()
		}
	}
	fun randomDoubleArray(): DoubleArray = DoubleArray( getRandom().nextInt(7)).apply {
		for (i in 0 until size) {
			this[i] = getRandom().nextDouble()
		}
	}
	fun randomFloatArray(): FloatArray = FloatArray( getRandom().nextInt(7)).apply {
		for (i in 0 until size) {
			this[i] = getRandom().nextFloat()
		}
	}
}

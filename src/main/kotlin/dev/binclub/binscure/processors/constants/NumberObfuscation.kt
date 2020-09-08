package dev.binclub.binscure.processors.constants

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.add
import dev.binclub.binscure.utils.internalName
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @author cookiedragon234 30/Jan/2020
 */
object NumberObfuscation: IClassProcessor {
	override val progressDescription: String
		get() = "Obfuscating number constants"
	override val config: TransformerConfiguration
		get() = rootConfig.numberObfuscation
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.enabled)
			return
		
		for (classNode in classes) {
			if (isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (isExcluded(classNode, method) || method.instructions == null)
					continue
				
				val modifier = InstructionModifier()
				for (insn in method.instructions) {
					if (isNumberLdc(insn)) {
						val num = getNumFromLdc(insn)
						
						when (num) {
							is Int -> obfInt(classNode, modifier, insn, num as Int)
							is Long -> obfLong(classNode, modifier, insn, num as Long)
							is Double -> obfDouble(classNode, modifier, insn, num as Double)
							is Float -> obfFloat(classNode, modifier, insn, num as Float)
						}
					}
				}
				modifier.apply(method)
			}
		}
	}
	
	private fun obfFloat(classNode: ClassNode, modifier: InstructionModifier, insn: AbstractInsnNode, num: Float) {
		val firstRand = random.nextFloat() * Float.MAX_VALUE
		val numAsInt = java.lang.Float.floatToIntBits(firstRand)
		val list = InsnList().apply {
			add(ldcFloat(firstRand xor num))
			add(MethodInsnNode(INVOKESTATIC, java.lang.Float::class.internalName, "floatToIntBits", "(F)I"))
			add(ldcInt(numAsInt))
			add(IXOR)
			add(MethodInsnNode(INVOKESTATIC, java.lang.Float::class.internalName, "intBitsToFloat", "(I)F"))
		}
		modifier.replace(insn, list)
	}
	
	private fun obfDouble(classNode: ClassNode, modifier: InstructionModifier, insn: AbstractInsnNode, num: Double) {
		val firstRand = random.nextDouble() * Double.MAX_VALUE
		val numAsLong = java.lang.Double.doubleToLongBits(firstRand)
		val list = InsnList().apply {
			add(ldcDouble(firstRand xor num))
			add(MethodInsnNode(INVOKESTATIC, java.lang.Double::class.internalName, "doubleToLongBits", "(D)J"))
			add(ldcLong(numAsLong))
			add(LXOR)
			add(MethodInsnNode(INVOKESTATIC, java.lang.Double::class.internalName, "longBitsToDouble", "(J)D"))
		}
		modifier.replace(insn, list)
	}
	
	private fun obfInt(classNode: ClassNode, modifier: InstructionModifier, insn: AbstractInsnNode, num: Int) {
		val firstRand = randomInt()
		val list = InsnList().apply {
			add(ldcLong(firstRand.toLong()))
			add(L2I)
			add(ldcInt(firstRand xor num))
			add(IXOR)
		}
		modifier.replace(insn, list)
	}
	
	private fun obfLong(classNode: ClassNode, modifier: InstructionModifier, insn: AbstractInsnNode, num: Long) {
		val firstRand = randomInt()
		val list = InsnList().apply {
			add(ldcInt(firstRand))
			add(I2L)
			add(ldcLong(firstRand.toLong() xor num))
			add(LXOR)
		}
		modifier.replace(insn, list)
	}
	
	private fun randomDoubleArray(): DoubleArray = DoubleArray( random.nextInt(7)).apply {
		for (i in 0 until size) this[i] = random.nextDouble()
	}
	private fun randomFloatArray(): FloatArray = FloatArray( random.nextInt(7)).apply {
		for (i in 0 until size) this[i] = random.nextFloat() * 100
	}
}

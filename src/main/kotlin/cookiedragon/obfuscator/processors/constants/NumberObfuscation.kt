package cookiedragon.obfuscator.processors.constants

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
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
		val randNumbers = randomFloatArray()
		val list = InsnList()
		var newNum = random.nextFloat() * 100
		list.insert(ldcFloat(newNum))
		
		val lastOp = MutableInteger(-1)
		for (randNumber in randNumbers) {
			randomBranchExcluding(random, lastOp, {
				newNum += randNumber
				list.add(ldcFloat(randNumber))
				list.add(InsnNode(FADD))
				
			}, {
				newNum -= randNumber
				list.add(ldcFloat(randNumber))
				list.add(InsnNode(FSUB))
			})
		}
		list.add(ldcFloat(num - newNum))
		list.add(InsnNode(FADD))
		modifier.replace(insn, list)
	}
	
	private fun obfDouble(classNode: ClassNode, modifier: InstructionModifier, insn: AbstractInsnNode, num: Double) {
		val randNumbers = randomDoubleArray()
		val list = InsnList()
		var newNum = random.nextDouble()
		list.insert(ldcDouble(newNum))
		
		val lastOp = MutableInteger(-1)
		for (randNumber in randNumbers) {
			randomBranchExcluding(random, lastOp, {
				newNum += randNumber
				list.add(ldcDouble(randNumber))
				list.add(InsnNode(DADD))
			}, {
				newNum -= randNumber
				list.add(ldcDouble(randNumber))
				list.add(InsnNode(DSUB))
			})
		}
		list.add(ldcDouble(num - newNum))
		list.add(InsnNode(DADD))
		modifier.replace(insn, list)
	}
	
	private fun obfInt(classNode: ClassNode, modifier: InstructionModifier, insn: AbstractInsnNode, num: Int) {
		val randNumbers = randomIntArray()
		val list = InsnList()
		var newNum = random.nextInt(Integer.MAX_VALUE)
		list.insert(ldcInt(newNum))
		
		val lastOp = MutableInteger(-1)
		for (randNumber in randNumbers) {
			randomBranchExcluding(random, lastOp, {
				newNum = newNum xor randNumber
				list.add(ldcInt(randNumber))
				list.add(InsnNode(IXOR))
			}, {
				newNum = newNum shl randNumber
				list.add(ldcInt(randNumber))
				list.add(InsnNode(ISHL))
			}, {
				newNum = newNum shr randNumber
				list.add(ldcInt(randNumber))
				list.add(InsnNode(ISHR))
			}, {
				newNum = newNum ushr randNumber
				list.add(ldcInt(randNumber))
				list.add(InsnNode(IUSHR))
			}, {
				newNum += randNumber
				list.add(ldcInt(randNumber))
				list.add(InsnNode(IADD))
			}, {
				newNum -= randNumber
				list.add(ldcInt(randNumber))
				list.add(InsnNode(ISUB))
			}, {
				newNum = newNum and randNumber
				list.add(ldcInt(randNumber))
				list.add(InsnNode(IAND))
			}, {
				newNum = newNum or randNumber
				list.add(ldcInt(randNumber))
				list.add(InsnNode(IOR))
			}, {
				newNum = -newNum
				list.add(InsnNode(INEG))
			})
		}
		list.add(ldcInt(num xor newNum))
		list.add(InsnNode(IXOR))
		modifier.replace(insn, list)
	}
	
	private fun obfLong(classNode: ClassNode, modifier: InstructionModifier, insn: AbstractInsnNode, num: Long) {
		val randNumbers = randomLongArray()
		val list = InsnList()
		var newNum = random.nextLong()
		list.insert(ldcLong(newNum))
		
		val lastOp = MutableInteger(-1)
		for (randNumber in randNumbers) {
			randomBranchExcluding(random, lastOp, {
				newNum = newNum xor randNumber
				list.add(ldcLong(randNumber))
				list.add(InsnNode(LXOR))
			}, {
				val randInt = random.nextInt(Integer.MAX_VALUE)
				newNum = newNum shl randInt
				list.add(ldcInt(randInt))
				list.add(InsnNode(LSHL))
			}, {
				val randInt = random.nextInt(Integer.MAX_VALUE)
				newNum = newNum shr randInt
				list.add(ldcInt(randInt))
				list.add(InsnNode(LSHR))
			}, {
				val randInt = random.nextInt(Integer.MAX_VALUE)
				newNum = newNum ushr randInt
				list.add(ldcInt(randInt))
				list.add(InsnNode(LUSHR))
			}, {
				newNum += randNumber
				list.add(ldcLong(randNumber))
				list.add(InsnNode(LSUB))
			}, {
				newNum -= randNumber
				list.add(ldcLong(randNumber))
				list.add(InsnNode(LADD))
			}, {
				newNum = newNum and randNumber
				list.add(ldcLong(randNumber))
				list.add(InsnNode(LAND))
			}, {
				newNum = newNum or randNumber
				list.add(ldcLong(randNumber))
				list.add(InsnNode(LOR))
			}, {
				newNum = -newNum
				list.add(InsnNode(LNEG))
			})
		}
		list.add(ldcLong(num xor newNum))
		list.add(InsnNode(IXOR))
		modifier.replace(insn, list)
	}
	
	private fun randomIntArray(): IntArray = IntArray( random.nextInt(7)).apply {
		for (i in 0 until size) this[i] = random.nextInt(Integer.MAX_VALUE)
	}
	private fun randomLongArray(): LongArray = LongArray( random.nextInt(7)).apply {
		for (i in 0 until size) this[i] = random.nextLong()
	}
	private fun randomDoubleArray(): DoubleArray = DoubleArray( random.nextInt(7)).apply {
		for (i in 0 until size) this[i] = random.nextDouble()
	}
	private fun randomFloatArray(): FloatArray = FloatArray( random.nextInt(7)).apply {
		for (i in 0 until size) this[i] = random.nextFloat() * 100
	}
}

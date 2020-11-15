package dev.binclub.binscure.processors.flow.loop

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.toOpcodeStrings
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.*
import kotlin.math.max
import kotlin.math.min

/**
 * @author cook 14/Nov/2020
 */
object LoopUnroller: IClassProcessor {
	override val progressDescription: String = "Unrolling loops"
	override val config = rootConfig//.flowObfuscation TODO remember to re enable this
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		println("\r")
		val inOut = InOutMap<LabelNode>()
		forClass(classes) { cn ->
			forMethod(cn) { mn ->
				val instructions = mn.instructions
				val modifier = InstructionModifier()
				insnLoop@for (insn in instructions) {
					if (insn is JumpInsnNode && insn.opcode != GOTO) {
						val jump = insn
						val label = jump.label
						val labelIndex = instructions.indexOf(label)
						val jumpIndex = instructions.indexOf(jump)
						val startNode = if (jumpIndex > labelIndex) label else jump
						val start = min(labelIndex, jumpIndex) + 1
						val end = max(labelIndex, jumpIndex) - 1
						val distance = end - start
						
						// perhaps 5-50 instructions for a large enough sample to confuse stuff but small enough to
						// not bloat too much
						if (distance > 10) {
							val copy = InsnList()
							val oldCache = instructions.checkCache()
							
							for (i in start until end) {
								if (oldCache[i] is LabelNode) {
									// we cant duplicate regions that contain a label
									//continue@insnLoop
								}
							}
							
							for (i in start until end) {
								val nsn = oldCache[i]
								if (nsn !is LabelNode && nsn !is LineNumberNode) {
									copy.add(nsn.clone(inOut))
								}
							}
							
							if (cn.name == "com/binclub/StaticMethodTest") println(copy.toOpcodeStrings())
							
							modifier.append(startNode, copy)
							
							println("Added to $cn.$mn ($jumpIndex -> $labelIndex) ${copy.size()}")
						}
					}
				}
				modifier.apply(mn)
			}
		}
	}
	
	class InOutMap<T>: Map<T, T> {
		override val entries: Set<Map.Entry<T, T>>
			get() = TODO("Not yet implemented")
		override val keys: Set<T>
			get() = TODO("Not yet implemented")
		override val size: Int
			get() = TODO("Not yet implemented")
		override val values: Collection<T>
			get() = TODO("Not yet implemented")
		
		override fun containsKey(key: T): Boolean {
			TODO("Not yet implemented")
		}
		
		override fun containsValue(value: T): Boolean {
			TODO("Not yet implemented")
		}
		
		override fun get(key: T): T? = key
		
		override fun isEmpty(): Boolean {
			TODO("Not yet implemented")
		}
	}
}

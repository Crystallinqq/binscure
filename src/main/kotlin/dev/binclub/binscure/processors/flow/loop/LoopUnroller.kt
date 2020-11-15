package dev.binclub.binscure.processors.flow.loop

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.toOpcodeStrings
import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.*

/**
 * @author cook 14/Nov/2020
 */
object LoopUnroller: IClassProcessor {
    override val progressDescription: String = "Unrolling loops"
    override val config = rootConfig//.flowObfuscation TODO remember to re enable this

    override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
        val inOut = InOutMap<LabelNode>()
        forClass(classes) { cn ->
            forMethod(cn) { mn ->
                val instructions = mn.instructions
                val modifier = InstructionModifier()
                insnLoop@for (insn in instructions) {
                    if (insn is JumpInsnNode && insn.opcode == GOTO) {
                        val jump = insn
                        val label = jump.label
                        val labelIndex = instructions.indexOf(label)
                        val jumpIndex = instructions.indexOf(jump)
                        val distance = jumpIndex - labelIndex

                        println("Distance $distance")

                        // perhaps 5-50 instructions for a large enough sample to confuse stuff but small enough to
                        // not bloat too much

                        if (distance > 0) {
                            val copy = InsnList()
                            val oldCache = instructions.checkCache()

                            for (i in (labelIndex) until (labelIndex - 1)) {
                                if (oldCache[i] is LabelNode) {
                                    // we cant duplicate regions that contain a label
                                    println("Skipped")
                                    continue@insnLoop
                                }
                                println("-")
                            }
                            println("-")

                            for (i in jumpIndex until labelIndex) {
                                copy.add(oldCache[i].clone(inOut))
                            }
                            println("-")

                            if (cn.name == "com/binclub/EnumTest") println(copy.toOpcodeStrings())

                            modifier.prepend(jump, copy)

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

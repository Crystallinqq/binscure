package dev.binclub.binscure.processors.arithmetic

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.insnBuilder
import dev.binclub.binscure.utils.randomBranch
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 02/Aug/2020
 */
object ArithmeticSubstitutionTransformer: IClassProcessor {
	override val progressDescription: String
		get() = "Substituting arithmetic operations"
	override val config = rootConfig.flowObfuscation
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.enabled || !config.arithmetic)
			return
		
		forClass(classes) {
			forMethod(it) { method ->
				val modifier = InstructionModifier()
				
				for (insn in method.instructions) {
					if (!CObfuscator.randomWeight(config.severity))
						continue
					
					when (insn.opcode) {
						INEG -> {
							randomBranch(random, {
								modifier.replace(insn, insnBuilder {
									iconst_m1()
									imul()
								})
							}, {
								modifier.replace(insn, insnBuilder {
									iconst_m1()
									ixor()
									iconst_m1()
									isub()
								})
							}, {
								modifier.replace(insn, insnBuilder {
									iconst_m1()
									ixor()
									iconst_1()
									iadd()
								})
							}, {
								modifier.replace(insn, insnBuilder {
									iconst_1()
									isub()
									iconst_m1()
									ixor()
								})
							}, {
								modifier.replace(insn, insnBuilder {
									iconst_m1()
									iadd()
									iconst_m1()
									ixor()
								})
							})
						}
						IADD -> {
							randomBranch(random, {
								modifier.replace(insn, insnBuilder {
									ineg()
									isub()
								})
							}, {
								modifier.replace(insn, insnBuilder {
									// (a + b) = (a | b) + (a & b)
									// x = a | b
									// y = a & b
									// out = x + y
									dup2() // [a, b, a, b]
									ior() // [x, a, b]
									dup_x2() // [x, a, b, x]
									pop() // [a, b, x]
									iand() // [y, x]
									iadd() // [out]
								})
							})
						}
						ISUB -> {
							randomBranch(random, {
								modifier.replace(insn, insnBuilder {
									ineg()
									iadd()
								})
							}, {
								modifier.replace(insn, insnBuilder {
									// (a - b) = (-a | b) + (-b & a)
									// x = -a | b
									// y = -b & a
									// out = x + y
									dup2() // [a, b, a, b]
									ineg() // [-a, b, a, b]
									ior() // [x, a, b]
									dup_x2() // [x, a, b, x]
									pop() // [a, b, x]
									swap() // [b, a, x]
									ineg() // [-b, a, x]
									iand() // [y, x]
									iadd() // [out]
								})
							})
						}
					}
				}
				
				modifier.apply(method)
			}
		}
	}
}

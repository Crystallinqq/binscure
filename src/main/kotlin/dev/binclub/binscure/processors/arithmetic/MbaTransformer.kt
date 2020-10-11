package dev.binclub.binscure.processors.arithmetic

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.insnBuilder
import dev.binclub.binscure.utils.randomBranch
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import java.lang.NullPointerException

/**
 * Mixed boolean arithmetic is the process of converting a classical arithmetic operation (e.g. add, sub, mul...) into
 * a combination of both classical and boolean arithmetic operations
 *
 * We do this by using a list of known boolean identities and repeatedly applying them
 *
 * We can even apply identities to existing identities making the calculation recursively larger
 *
 * @author cook 09/Oct/2020
 */
object MbaTransformer: IClassProcessor {
	override val progressDescription: String = "Applying mixed boolean arithmetic"
	override val config = rootConfig.arithmetic
	
	private const val maxMethodSize = 65535
	private const val maxInsns = maxMethodSize / 3 // about 2 bytes per instruction so make conservative estimate
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.enabled) return
		
		forClass(classes) { cn ->
			forMethod(cn) { mn ->
				try {
					val list = mn.instructions
					if (list != null && list.size() > 0) {
						for (i in 0 until config.repeat) {
							val modifier = InstructionModifier()
							var skip = 0
							for (op in list) {
								if (skip > 0) {
									skip -= 1
									continue
								}
								skip += substitute(op, modifier)
							}
							if (modifier.isEmpty()) break
							if (!modifier.apply(list)) {
								println("\rWarning: Stopped MBA for method ${cn.getOriginalName()}.${mn.name}${mn.desc} ($maxInsns instructions)")
								break
							}
						}
					}
				} catch (n: NullPointerException) {
					n.printStackTrace()
				}
			}
		}
	}
	
	fun substitute(op: AbstractInsnNode, modifier: InstructionModifier): Int {
		var skip = 0
		when {
			op.opcode == ICONST_M1 && op.next?.opcode == IXOR && op.next.next?.opcode == IAND -> {
				modifier.remove(op.next.next)
				modifier.remove(op.next)
				val sub = randomBranch(random, {
					// x & ~y == (x | y) - y
					insnBuilder {
						dup_x1()
						ior()
						swap()
						isub()
					}
				}, {
					// x & ~y == x - (x & y)
					insnBuilder {
						swap()
						dup_x1()
						iand()
						isub()
					}
				})
				modifier.replace(op, sub)
				skip = 2
			}
			op.opcode == ISUB && op.next?.opcode == ICONST_M1 && op.next.next?.opcode == IXOR -> {
				modifier.remove(op.next.next)
				modifier.remove(op.next)
				val sub = randomBranch(random, {
					// ~(x - y) == y - x - 1
					insnBuilder {
						swap()
						isub()
						iconst_1()
						isub()
					}
				}, {
					// ~(x - y) == ~x + y
					insnBuilder {
						swap()
						iconst_m1()
						ixor()
						iadd()
					}
				})
				modifier.replace(op, sub)
				skip = 2
			}
			op.opcode == ICONST_M1 && op.next?.opcode == IXOR -> {
				// ~x == (-x)-1
				modifier.remove(op.next)
				modifier.replace(op, insnBuilder {
					ineg()
					iconst_m1()
					iadd()
				})
				skip = 1
			}
			op.opcode == INEG -> {
				val sub = randomBranch(random, {
					// -x == (~x)+1
					insnBuilder {
						iconst_m1()
						ixor()
						iconst_1()
						iadd()
					}
				}, {
					// ~x == ~(x-1)
					insnBuilder {
						iconst_1()
						isub()
						iconst_m1()
						ixor()
					}
				})
				modifier.replace(op, sub)
			}
			op.opcode == IADD -> {
				val sub = randomBranch(random, {
					// x + y == (x - ~y) - 1
					insnBuilder {
						iconst_m1()
						ixor()
						isub()
						iconst_1()
						isub()
					}
				}, {
					// x + y == (x ^ y) + (2 * (x & y))
					insnBuilder {
						dup2() // [x, y, x, y]
						ixor() // [x^y, x, y]
						dup_x2() // [x^y, x, y, x^y]
						pop() // [x, y, x^y]
						iand() // [x&y, x^y]
						iconst_2() // [2, (x&y), x^y]
						imul() // [2*(x&y), x^y]
						iadd()
					}
				}, {
					// x + y == (x | y) + (x & y)
					insnBuilder {
						dup2()
						ior()
						dup_x2()
						pop()
						iand()
						iadd()
					}
				}, {
					// x + y == (2*(x|y)) - (x^y)
					insnBuilder {
						dup2() // [x, y, x, y]
						ior() // [x|y, x, y]
						iconst_2() // [2, x|y, x, y]
						imul() // [2*(x|y), x, y]
						dup_x2() // [x|y, x, y, x|y]
						pop() // [x, y, x|y]
						ixor() // [x^y, x|y]
						isub()
					}
				})
				modifier.replace(op, sub)
			}
			op.opcode == ISUB -> {
				val sub = randomBranch(random, {
					// x - y == x + ~y + 1
					insnBuilder {
						iconst_m1()
						ixor()
						iadd()
						iconst_1()
						iadd()
					}
				}, {
					// x - y == (x^y) - 2(~x & y)
					insnBuilder {
						dup2()
						ixor()
						dup_x2()
						pop()
						swap()
						iconst_m1()
						ixor()
						iand()
						iconst_2()
						imul()
						isub()
					}
				}, {
					// x - y == (x & ~y) - (~x & y)
					insnBuilder {
						dup2()
						iconst_m1()
						ixor()
						iand()
						dup_x2()
						pop()
						swap()
						iconst_m1()
						ixor()
						iand()
						isub()
					}
				}, {
					// x - y == 2(x & ~y) - (x ^ y)
					insnBuilder {
						dup2()
						iconst_m1()
						ixor()
						iand()
						iconst_2()
						imul()
						dup_x2()
						pop()
						ixor()
						isub()
					}
				})
				modifier.replace(op, sub)
			}
			op.opcode == IXOR -> {
				val sub = randomBranch(random, {
					// x ^ y == (x | y) - (x & y)
					insnBuilder {
						dup2()
						ior()
						dup_x2()
						pop()
						iand()
						isub()
					}
				}, {
					// x ^ y == (x & ~y) | (~x & y)
					insnBuilder {
						dup2()
						iconst_m1()
						ixor()
						iand()
						dup_x2()
						pop()
						swap()
						iconst_m1()
						ixor()
						iand()
						ior()
					}
				}, {
					// x ^ y == (x | y) & (~x | ~y)
					insnBuilder {
						dup2()
						ior()
						dup_x2()
						pop()
						iconst_m1()
						ixor()
						swap()
						iconst_m1()
						ixor()
						ior()
						iand()
					}
				}, {
					// x ^ y == (x | y) & ~(x & y)
					insnBuilder {
						dup2()
						ior()
						dup_x2()
						pop()
						iand()
						iconst_m1()
						ixor()
						iand()
					}
				})
				modifier.replace(op, sub)
			}
			op.opcode == IOR -> {
				// x | y == (x & ~y) + y
				modifier.replace(op, insnBuilder {
					dup_x1()
					iconst_m1()
					ixor()
					iand()
					iadd()
				})
			}
			op.opcode == IAND -> {
				// x & y == (~x | y) - ~x
				modifier.replace(op, insnBuilder {
					swap()
					dup_x1()
					iconst_m1()
					ixor()
					ior()
					swap()
					iconst_m1()
					ixor()
					isub()
				})
			}
		}
		return skip
	}
}

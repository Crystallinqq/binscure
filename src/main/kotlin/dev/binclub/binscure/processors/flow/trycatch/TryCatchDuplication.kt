package dev.binclub.binscure.processors.flow.trycatch

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.processors.flow.MethodParameterObfuscator
import dev.binclub.binscure.processors.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.util.*

/**
 * @author cookiedragon234 24/Sep/2020
 */
object TryCatchDuplication: IClassProcessor {
	override val progressDescription: String = "Inserting interlocking try catch blocks"
	override val config = rootConfig.flowObfuscation
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.noverify) {
			return
		}
		
		forClass(classes) { cn ->
			if (cn.versionAtLeast(Opcodes.V9) && config.java8) {
				error("Cannot use Java8 tactics on Java+9 class file [${cn.name}]")
			}
			if (cn.superName == "java/lang/Object") {
				var injected = false
				forMethod(cn) { mn ->
					try {
						if (!mn.name.startsWith("<")) {
							mn.instructions?.let { insns ->
								if (insns.size() > 30) {
									val stackHeights = StackHeightCalculatorLite.calculate(mn)
									
									val blocks = arrayListOf<InsnList>()
									val tryCatchBlocks: List<TryCatchBlockNode> =
										mn.tryCatchBlocks ?: Collections.emptyList()
									
									var currentBlock = InsnList()
									blocks += currentBlock
									var skip = 6
									var tcbs = 0
									insns.iterator().forEach { insn ->
										val height = stackHeights[insn]
										if (insn is LabelNode) {
											tryCatchBlocks.forEach { tcb ->
												if (tcb.start == insn) {
													tcbs += 1
												} else if (tcb.end == insn) {
													tcbs -= 1
												}
											}
										} else {
											if (skip > 0) {
												skip -= 1
											} else {
												if (tcbs <= 0 && height == 0 && currentBlock.size() > 4) {
													skip = 4
													
													currentBlock = InsnList()
													blocks += currentBlock
												}
											}
										}
										currentBlock.add(insn)
									}
									
									val key = random.nextInt()
									
									if (blocks.size > 2) {
										val switch = newLabel()
										
										val labels = Array(blocks.size) { i ->
											newLabel().also {
												val block = blocks[i]
												block.insert(it)
												block.add(insnBuilder {
													ldc((i + 1) xor key)
													goto(switch)
												})
											}
										}
										val handlerLabel = newLabel()
										
										mn.instructions = insnBuilder {
											+blocks.removeAt(0)
											blocks.shuffle(random)
											blocks.forEach {
												+it
											}
											val thrwLbl = newLabel()
											+thrwLbl
											aconst_null()
											athrow()
											
											val dflt = newLabel()
											+dflt
											iconst_0()
											
											+switch
											ldc(key)
											ixor()
											tableswitch(0, dflt, *labels)
											
											+handlerLabel
											val fakeJmpLbl = newLabel()
											+randomOpaqueJump(fakeJmpLbl, false, mnStr = MethodParameterObfuscator.mnToStr(cn, mn))
											athrow()
											+fakeJmpLbl
											aconst_null()
											athrow()
										}
										
										mn.tryCatchBlocks = mn.tryCatchBlocks ?: arrayListOf()
										
										val minTcbLength = 7
										
										val addTcbs = mn.instructions.size() / 10
										for (i in 0 until addTcbs) {
											val startLabel = newLabel()
											val endLabel = newLabel()
											
											val startIndex = random.nextInt(mn.instructions.size() - 1 - minTcbLength)
											mn.instructions.insert(mn.instructions.get(startIndex), startLabel)
											
											val length = random.nextInt(mn.instructions.size() - 1 - startIndex)
												.coerceAtLeast(minTcbLength)
											val endIndex = startIndex + length
											mn.instructions.insert(mn.instructions.get(endIndex), endLabel)
											
											if (endIndex <= startIndex) error("")
											
											val tcb =
												TryCatchBlockNode(startLabel, endLabel, handlerLabel, randomThrowable())
											mn.tryCatchBlocks.add(tcb)
										}
										
										mn.localVariables = null
										verifyMethodNode(mn)
										injected = true
									}
								}
							}
						}
					} catch (t: Throwable) {
						Exception("Error duplicating try catch nodes for ${cn.getOriginalName()}.${mn.name}${mn.desc}", t).printStackTrace()
					}
				}
				if (injected && config.java8) {
					cn.superName = "sun/reflect/MethodAccessorImpl"
					cn.verify = false
				}
			}
		}
	}
}

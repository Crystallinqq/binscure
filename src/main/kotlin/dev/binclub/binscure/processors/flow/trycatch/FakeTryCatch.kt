package dev.binclub.binscure.processors.flow.trycatch

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.processors.flow.MethodParameterObfuscator
import dev.binclub.binscure.utils.add
import dev.binclub.binscure.processors.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.newLabel
import dev.binclub.binscure.utils.randomThrowable
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 27/Jan/2020
 */
object FakeTryCatch: IClassProcessor {
	override val progressDescription: String
		get() = "Adding fake try catch blocks"
	override val config = rootConfig.flowObfuscation
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.enabled) {
			return
		}
		
		for (classNode in classes) {
			if (!isExcluded(classNode)) {
				for (method in classNode.methods) {
					if (method.name.startsWith('<') || isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
						continue
					
					addFakeTryCatches(classNode, method)
				}
			}
		}
	}
	
	private fun addFakeTryCatches(cn: ClassNode, methodNode: MethodNode) {
		methodNode.tryCatchBlocks = methodNode.tryCatchBlocks ?: arrayListOf()
		methodNode.tryCatchBlocks.addAll(
			addFakeTryCatches(cn, methodNode, methodNode.instructions)
		)
	}
	
	private fun addFakeTryCatches(cn: ClassNode, mn: MethodNode, insnList: InsnList): Array<TryCatchBlockNode> {
		val switchStart = newLabel()
		val fakeEnd = newLabel()
		val start = newLabel()
		val handler = newLabel()
		val end = newLabel()
		val secondCatch = newLabel()
		val dead = newLabel()
		val dead2 = newLabel()
		
		val list = if (rootConfig.flowObfuscation.severity >= 5) {
			InsnList()
				.apply {
					add(start)
					add(ACONST_NULL)
					add(randomOpaqueJump(handler, false, mnStr = MethodParameterObfuscator.mnToStr(cn, mn)))
					if (rootConfig.crasher.enabled) {
						add(TypeInsnNode(CHECKCAST, "a"))
					}
					add(end)
					add(POP)
				} to InsnList().apply {
					add(handler)
					add(DUP)
					add(JumpInsnNode(IFNULL, end))
					add(ATHROW)
					add(secondCatch)
				}
		} else {
			InsnList().apply {
				add(switchStart)
				add(start)
				add(InsnNode(ACONST_NULL))
				add(randomOpaqueJump(secondCatch, false, mnStr = MethodParameterObfuscator.mnToStr(cn, mn)))
				add(InsnNode(POP))
				add(InsnNode(ACONST_NULL))
				if (rootConfig.crasher.enabled) {
					add(TypeInsnNode(CHECKCAST, "give up"))
				}
				add(JumpInsnNode(GOTO, handler))
				add(fakeEnd)
				add(TypeInsnNode(CHECKCAST, "java/lang/Throwable"))
				add(InsnNode(ATHROW))
				add(end)
			} to InsnList().apply {
				add(dead)
				add(POP)
				add(JumpInsnNode(GOTO, end))
				add(dead2)
				add(POP)
				add(ACONST_NULL)
				add(JumpInsnNode(GOTO, dead))
				add(handler)
				add(DUP)
				add(JumpInsnNode(IFNULL, dead))
				add(TypeInsnNode(CHECKCAST, "java/lang/Throwable"))
				add(InsnNode(ATHROW))
				add(secondCatch)
				add(DUP)
				add(JumpInsnNode(IFNULL, dead2))
				add(TypeInsnNode(CHECKCAST, "java/lang/Throwable"))
				add(InsnNode(ATHROW))
			}
		}
		
		insnList.insert(list.first)
		insnList.add(list.second)
		return arrayOf(
			TryCatchBlockNode(start, end, handler, randomThrowable()),
			TryCatchBlockNode(handler, secondCatch, handler, randomThrowable())
			//TryCatchBlockNode(start, end, handler, randomThrowable()),
			//TryCatchBlockNode(fakeEnd, end, secondCatch, randomThrowable()),
			//TryCatchBlockNode(handler, secondCatch, handler, randomThrowable()),
			//TryCatchBlockNode(start, fakeEnd, secondCatch, randomThrowable())
		
		)
	}
}

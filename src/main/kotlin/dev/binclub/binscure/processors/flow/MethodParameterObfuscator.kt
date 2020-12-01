@file:Suppress("NOTHING_TO_INLINE")

package dev.binclub.binscure.processors.flow

import arrow.core.Tuple3
import arrow.core.Tuple4
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.utils.InstructionModifier
import dev.binclub.binscure.utils.doubleSize
import dev.binclub.binscure.utils.insnBuilder
import dev.binclub.binscure.utils.randomInt
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.*


object MethodParameterObfuscator : IClassProcessor {
	override val progressDescription: String = "Obfuscating method parameters"
	override val config = rootConfig.methodParameter
	
	private val META_FACTORY = Handle(
		H_INVOKESTATIC,
		"java/lang/invoke/LambdaMetafactory",
		"metafactory",
		"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
		false
	)
	private val ALT_META_FACTORY = Handle(
		H_INVOKESTATIC,
		"java/lang/invoke/LambdaMetafactory",
		"altMetafactory",
		"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
		false
	)
	
	
	val methodSecrets: MutableMap<String, Pair<Int, Int>> = HashMap()
	
	inline fun mnToStr(cn: ClassNode, mn: MethodNode) = cn.name + "." + mn.name + mn.desc
	inline fun mnToStr(insn: MethodInsnNode, add: Boolean = false) =
		insn.owner + "." + insn.name + if (!add) insn.desc else insn.desc.replace(")", "I)")
	
	inline fun mnToStr(hn: Handle, add: Boolean = false) =
		hn.owner + "." + hn.name + if (!add) hn.desc else hn.desc.replace(")", "I)")
	
	private fun secretIndex(desc: String): Int {
		val args = Type.getArgumentTypes(desc)
		var ourParamIndex = -1 // negate final param append, we need index of it
		for (arg in args) {
			ourParamIndex += if (arg.doubleSize) 2 else 1
		}
		return ourParamIndex
	}
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!config.enabled)
			return
		
		if (rootConfig.indirection.enabled) {
			println("\rWARNING: Method parameter obfuscation cannot be applied on top of indirection")
			return
		}
		
		val methodNodesToRemap = ArrayList<Tuple4<Int?, Int?, InsnList, MethodInsnNode>>()
		classes.forEach { cn ->
			cn.methods.forEach { mn ->
				// this methods secret
				val (thisSecret, secretIndex) = methodSecrets[mnToStr(cn, mn)].let {
					(it?.first to it?.second)
				}
				
				mn.instructions?.let { list ->
					for (insn in list) {
						when (insn) {
							// Exclude methods that are referenced by indys
							is InvokeDynamicInsnNode -> {
								val bsm = insn.bsm
								if (bsm == META_FACTORY || bsm == ALT_META_FACTORY) {
									val handle = insn.bsmArgs[1] as Handle
									methodSecrets[mnToStr(handle, true)] = (-1) to (-1)
								}
							}
							is MethodInsnNode -> {
								if (
									insn.opcode == INVOKESTATIC
									&&
									insn.name[0] != '<'
									&&
									insn.name != "main"
								) {
									methodNodesToRemap.add(Tuple4(thisSecret, secretIndex, list, insn))
								}
							}
						}
					}
				}
			}
		}
		
		forClass(classes) { cn ->
			if ((cn.access and ACC_ENUM) != 0)
				return@forClass
			
			forMethod(cn) { mn ->
				if (mn.name[0] == '<' || mn.name == "main")
					return@forMethod
				if ((mn.access and (ACC_NATIVE or ACC_ABSTRACT)) != 0)
					return@forMethod
				if ((mn.access and ACC_STATIC) == 0) // so that we dont have to bother with method inheritance
					return@forMethod
				
				val newDesc = mn.desc.replace(")", "I)")
				
				// if any of the classes other methods occupy the new desc then cancel, we cant do it
				if (cn.methods.any { it.name == mn.name && it.desc == newDesc })
					return@forMethod
				
				val mnStr = mnToStr(cn, mn)
				if (methodSecrets.containsKey(mnStr)) {
					// skip, possibly excluded by above loop
					println("\rSkipped $mnStr as its referenced by an indy")
				}
				
				// take the secret as a parameter
				mn.desc = newDesc
				
				
				// find the local variable table index that we will insert the final parameter into
				val ourParamIndex = secretIndex(mn.desc)
				
				methodSecrets[mnToStr(cn, mn)] = randomInt() to ourParamIndex
				
				// since we've added a new parameter we need to move all local variables up
				
				// modify local variable table
				mn.localVariables?.forEach { lv ->
					if (lv.index >= ourParamIndex) {
						lv.index += 1
					}
				}
				
				// increment local variable references
				for (insn in mn.instructions) {
					when (insn) {
						is VarInsnNode -> {
							if (insn.`var` >= ourParamIndex) {
								insn.`var` += 1
							}
						}
						is IincInsnNode -> {
							if (insn.`var` >= ourParamIndex) {
								insn.`var` += 1
							}
						}
					}
				}
			}
		}
		
		methodNodesToRemap.forEach {
			val thisSecret = it.a
			val secretIndex = it.b
			val list = it.c
			val insn = it.d
			
			methodSecrets[mnToStr(insn, true)]?.let { (otherSecret, _) ->
				// need to add the parameter
				insn.desc = insn.desc.replace(")", "I)")
				
				// if this method also takes in a secret we can use it to derive the other secret
				val prepend = if (thisSecret != null) {
					insnBuilder {
						iload(secretIndex!!)
						ldc(thisSecret xor otherSecret)
						ixor()
					}
				} else {
					insnBuilder {
						ldc(otherSecret)
					}
				}
				
				list.insertBefore(insn, prepend)
			}
		}
	}
}

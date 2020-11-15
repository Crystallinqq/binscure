@file:Suppress("NOTHING_TO_INLINE")

package dev.binclub.binscure.processors.flow

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.*


object MethodParameterObfuscator : IClassProcessor {
	override val progressDescription: String = "Obfuscating method parameters"
	override val config = rootConfig.methodParameter
	
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
		
		classes.forEach { cn ->
			cn.methods.forEach { mn ->
				// this methods secret
                val (thisSecret, secretIndex) = methodSecrets[mnToStr(cn, mn)].let {
                    (it?.first to it?.second)
                }
				
				// if this method calls any methods with secret parameters we need to pass that parameter to it
				
				mn.instructions?.let { list ->
					val mod = InstructionModifier()
					for (insn in list) {
						when (insn) {
                            is MethodInsnNode -> {
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
                                    mod.prepend(insn, prepend)
                                }
                            }
                            is InvokeDynamicInsnNode -> {
                                val bsm = insn.bsm
                                if (bsm.owner == "java/lang/invoke/LambdaMetafactory" && bsm.name == "metafactory") {
                                    val handle = insn.bsmArgs[1] as Handle
                                    methodSecrets[mnToStr(handle, true)]?.let { _ ->
                                        println("\rWARNING: ${handle.owner}.${handle.name} is referenced by an indy, please exclude this method from the method parameter obfuscator")
                                    }
                                }
                            }
						}
					}
					mod.apply(list)
				}
			}
		}
	}
}

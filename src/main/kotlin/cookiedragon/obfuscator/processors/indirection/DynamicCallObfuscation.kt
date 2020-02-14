package cookiedragon.obfuscator.processors.indirection

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.kotlin.internalName
import cookiedragon.obfuscator.kotlin.wrap
import cookiedragon.obfuscator.kotlin.xor
import cookiedragon.obfuscator.processors.renaming.impl.ClassRenamer
import cookiedragon.obfuscator.utils.InstructionModifier
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 22/Jan/2020
 */
object DynamicCallObfuscation: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		//if (!ConfigurationManager.rootConfig.indirection.enabled)
		//	return
		
		val methodCalls = mutableSetOf<MethodCall>()
		for (classNode in CObfuscator.getProgressBar("Indirecting method calls").wrap(classes)) {
			if (CObfuscator.isExcluded(classNode))
				continue
			
			for (method in classNode.methods) {
				if (CObfuscator.isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
					continue
				
				for (insn in method.instructions) {
					if (insn is MethodInsnNode) {
						when (insn.opcode) {
							INVOKESTATIC, INVOKEVIRTUAL, INVOKEINTERFACE -> methodCalls.add(MethodCall(classNode, method, insn))
						}
					}
				}
			}
		}
		
		if (methodCalls.isNotEmpty()) {
			val decryptNode = ClassNode().apply {
				access = ACC_PUBLIC + ACC_FINAL
				version = methodCalls.first().classNode.version
				name = ClassRenamer.namer.uniqueRandomString()
				signature = null
				superName = "java/lang/Object"
				classes.add(this)
				ClassPath.classes[this.name] = this
				ClassPath.classPath[this.name] = this
			}
			
			val stringDecryptMethod = MethodNode(
				ACC_PRIVATE + ACC_FINAL + ACC_STATIC,
				"continue",
				"(Ljava/lang/String;)Ljava/lang/String;",
				null,
				null
			).apply {
				generateDecryptorMethod(decryptNode, this)
			}
			
			val bootStrapMethod = MethodNode(
				ACC_PUBLIC + ACC_FINAL + ACC_STATIC,
				"break",
				"(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;",
				null,
				null
			).apply {
				generateBootstrapMethod(decryptNode.name, stringDecryptMethod, this)
				decryptNode.methods.add(this)
			}
			
			val handler = Handle(H_INVOKESTATIC, decryptNode.name, bootStrapMethod.name, bootStrapMethod.desc, false)
			
			for (methodCall in methodCalls) {
				val insn = methodCall.insnNode
				
				var newDesc = insn.desc
				if (insn.opcode != INVOKESTATIC) {
					newDesc = newDesc.replace("(", "(L${insn.owner};")
				}
				val returnType = Type.getReturnType(newDesc)
				
				// Downcast types to java/lang/Object
				val args = Type.getArgumentTypes(newDesc)
				for (i in args.indices) {
					args[i] = genericType(args[i])
				}
				
				newDesc = Type.getMethodDescriptor(genericType(returnType), *args)
				
				val modifier = InstructionModifier()
				val list = InsnList().apply {
					val indyNode: InvokeDynamicInsnNode
					add(
						InvokeDynamicInsnNode(
							"",
							newDesc,
							handler,
							insn.opcode,
							encryptName(methodCall.classNode, methodCall.methodNode, insn.owner.replace('/', '.')),
							encryptName(methodCall.classNode, methodCall.methodNode, insn.name),
							encryptName(methodCall.classNode, methodCall.methodNode, insn.desc)
						).also { indyNode = it }
					)
					if (returnType.sort == Type.ARRAY) {
						add(TypeInsnNode(CHECKCAST, returnType.internalName))
					} else if (returnType.sort == Type.OBJECT) {
						if (insn.next is MethodInsnNode) {
							val next = insn.next as MethodInsnNode
							val params = Type.getArgumentTypes(next.desc)
							if (params.isEmpty()) {
								if (insn.next.opcode != INVOKESTATIC) {
									add(TypeInsnNode(CHECKCAST, next.owner))
								}
							} else {
								add(TypeInsnNode(CHECKCAST, params.last().internalName))
							}
						} else if (insn.next?.opcode == IFNULL) {
							//
						} else if (insn.next?.opcode == IFNONNULL) {
							//
						} else {
							add(TypeInsnNode(CHECKCAST, returnType.internalName))
						}
					}
					if (indyNode.next != null && indyNode.next.opcode == CHECKCAST) {
						val checkcast = indyNode.next as TypeInsnNode
						if (checkcast.desc == Any::class.internalName) {
							remove(checkcast)
						}
					}
				}
				modifier.replace(insn, list)
				modifier.apply(methodCall.methodNode)
			}
		}
	}
	
	private fun genericType(type: Type): Type {
		return when (type.sort) {
			Type.OBJECT -> Type.getType(Any::class.java)
			else -> type
		}
	}
	
	private fun encryptName(classNode: ClassNode, methodNode: MethodNode, originalStr: String): String {
		val classHash = classNode.name.replace('/', '.').hashCode()
		val methodHash = methodNode.name.replace('/', '.').hashCode()
		
		val original = originalStr.toCharArray()
		val new = CharArray(original.size)
		
		for (i in original.indices) {
			val char = original[i]
			new[i] = when (i % 5) {
				0 -> char xor 2
				1 -> char xor classHash
				2 -> char xor methodHash
				3 -> char xor (classHash + methodHash)
				4 -> char xor i
				else -> throw IllegalStateException("Illegal ${i % 6}")
			}
		}
		return String(new)
	}
	
	data class MethodCall(val classNode: ClassNode, val methodNode: MethodNode, val insnNode: MethodInsnNode)
}

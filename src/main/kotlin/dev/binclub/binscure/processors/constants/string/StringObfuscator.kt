package dev.binclub.binscure.processors.constants.string

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.transformers.StringObfuscationConfiguration
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.classpath.CustomClassWriter
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.processors.exploit.BadAttributeExploit
import dev.binclub.binscure.processors.exploit.BadClinitExploit
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.processors.runtime.OpaqueRuntimeManager
import dev.binclub.binscure.processors.runtime.randomOpaqueJump
import dev.binclub.binscure.utils.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.util.*
import kotlin.properties.Delegates

/**
 * @author cookiedragon234 20/Jan/2020
 */
object StringObfuscator: IClassProcessor {
	val key = random.nextInt(Int.MAX_VALUE)
	val keys = Array(random.nextInt(400).coerceAtLeast(20)) {
		random.nextInt(Int.MAX_VALUE)
	}
	
	var decryptNode: ClassNode? = null
	var proxyNode: ClassNode? = null
	var decryptMethod: MethodNode by Delegates.notNull()
	var fastDecryptMethod: MethodNode by Delegates.notNull()
	var keysField: FieldNode by Delegates.notNull()
	override val progressDescription: String
		get() = "Obfuscating string constants"
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!rootConfig.stringObfuscation.enabled) {
			return
		}
		
		val stringInsns = arrayListOf<EncryptedString>()
		for (classNode in classes) {
			if (isExcluded(classNode))
				continue
			if (!classNode.versionAtLeast(V1_7))
				continue
			
			for (method in classNode.methods) {
				if (isExcluded(classNode, method) || CObfuscator.noMethodInsns(method))
					continue
				
				for (insn in method.instructions) {
					if (insn is LdcInsnNode && insn.cst is String) {
						val cst = insn.cst as String
						if (cst.length > 1) {
							val encryptedString =
								encryptString(
									cst,
									key,
									classNode,
									method,
									insn
								)
							stringInsns.add(encryptedString)
						}
					}
				}
			}
		}
		if (stringInsns.size > 0) {
			val decryptNode = ClassNode()
				.apply {
					this.access = ACC_PUBLIC + ACC_FINAL
					this.version = V1_8
					this.name = ClassRenamer.namer.uniqueUntakenClass()
					this.signature = null
					this.superName = "java/lang/Object"
					this.sourceFile = "a"
					this.sourceDebug = "hello"
					//ClassPath.classes[this.name] = this
					ClassPath.classPath[this.name] = this
					if (rootConfig.crasher.enabled && rootConfig.crasher.antiAsm) {
						BadAttributeExploit.process(Collections.singleton(this), Collections.emptyMap())
					}
					//BadClinitExploit.process(Collections.singleton(this), Collections.emptyMap())
				}
			this.decryptNode = decryptNode
			val storageField = FieldNode(
				ACC_STATIC,
				"0",
				"L${OpaqueRuntimeManager.classNode.name};",
				null,
				null
			)
			decryptNode.fields.add(storageField)
			
			keysField = FieldNode(
				ACC_STATIC,
				"aiooi1iojionlknzjsdnfdas",
				"[I",
				null,
				null
			)
			decryptNode.fields.add(keysField)
			
			decryptNode.fields.add(FieldNode(
				ACC_PRIVATE,
				"ojasidfjoasdifjoqioqhweioqhlaksdf",
				"[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[I",
				null,
				null
			))
			
			val decryptorMethod =
				StringDecryptGenerator.generateDecrypterMethod(
					decryptNode,
					storageField,
					stringInsns
				)
			this.decryptMethod = decryptorMethod
			
			val simpleDecryptMethod = MethodNode(ACC_PUBLIC + ACC_STATIC, "2", "(Ljava/lang/String;)Ljava/lang/String;", null, null).apply {
				instructions.apply {
					add(VarInsnNode(ALOAD, 0))
					add(ldcInt(3))
					add(MethodInsnNode(INVOKESTATIC, decryptNode.name, decryptorMethod.name, decryptorMethod.desc))
					add(ARETURN)
				}
			}
			this.fastDecryptMethod = simpleDecryptMethod
			decryptNode.methods.add(simpleDecryptMethod)
			
			generateStaticBlock(
				decryptNode,
				storageField
			)
			generateInitFunc(
				decryptNode,
				storageField
			)
			
			val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)//CustomClassWriter(ClassWriter.COMPUTE_MAXS, verify = false)
			decryptNode.accept(writer)
			val resourceName = "${decryptNode.name}.class"
			ClassPath.passThrough[resourceName] = writer.toByteArray()
			
			val proxyNode = StringProxyGenerator.generateStringProxy(decryptNode, decryptorMethod, simpleDecryptMethod, resourceName)
			this.proxyNode = proxyNode
			
			for ((index, string) in stringInsns.withIndex()) {
				
				val modifier = InstructionModifier()
				
				string.insn.cst = string.encrypted
				val list = InsnList().apply {
					add(MethodInsnNode(
						INVOKESTATIC,
						decryptNode.name,
						simpleDecryptMethod.name,
						simpleDecryptMethod.desc,
						false
					))
				}
				
				modifier.append(string.insn, list)
				modifier.apply(string.methodNode)
			}
		}
	}
	
	private fun generateInitFunc(classNode: ClassNode, storageField: FieldNode) {
		classNode.methods.add(MethodNode(0, "<init>", "()V", null, null).apply {
			instructions.apply {
				add(VarInsnNode(ALOAD, 0))
				add(MethodInsnNode(INVOKESPECIAL, classNode.superName, "<init>", "()V"))
				add(RETURN)
			}
		})
	}
	
	private fun generateStaticBlock(classNode: ClassNode, storageField: FieldNode): MethodNode {
		val staticInit =
			classNode.methods.firstOrNull { it.name == "<clinit>" && it.desc == "()V" }
			?: MethodNode(ACC_STATIC, "<clinit>", "()V", null, null).also { mn ->
				mn.instructions.apply {
					val tc1S = newLabel()
					val tc1E = newLabel()
					val tc1H = newLabel()
					val tc2S = newLabel()
					val tc2E = newLabel()
					val tc2H = newLabel()
					
					mn.tryCatchBlocks.add(TryCatchBlockNode(tc2S, tc2E, tc2H, "java/lang/IllegalMonitorStateException"))
					mn.tryCatchBlocks.add(TryCatchBlockNode(tc1S, tc1E, tc1H, "java/lang/RuntimeException"))
					
					val loopStart = newLabel()
					val loopEnd = newLabel()
					val setArr = newLabel()
					val inc = newLabel()
					
					add(ACONST_NULL) // [a]
					add(DUP)
					add(VarInsnNode(ASTORE, 2)) // [a]
					add(DUP) // [a a]
					add(ICONST_M1)  // [a a i]
					add(VarInsnNode(ISTORE, 4)) // [a a]
					add(VarInsnNode(ASTORE, 3)) // [a]
					add(TypeInsnNode(NEW, "java/util/Random")) // [a u]
					add(DUP) // [a u u]
					add(ldcInt(keys.size xor key)) // [a u u i]
					add(ldcInt(key)) // [a u u i i]
					add(IXOR) // [a u u i]
					add(DUP) // [a u u i i]
					add(IntInsnNode(NEWARRAY, T_INT)) // [a u u i a]
					add(DUP) // [a u u i a a]
					add(VarInsnNode(ASTORE, 1)) // [a u u i a]
					add(FieldInsnNode(PUTSTATIC, classNode.name, keysField.name, keysField.desc)) // [a u u i]
					add(VarInsnNode(ISTORE, 0)) // [a u u]
					add(tc1S)
					add(MethodInsnNode(INVOKESPECIAL, "java/util/Random", "<init>", "()V")) // [a a]
					add(VarInsnNode(ASTORE, 2)) // [a]
					add(DUP) // [a a]
					add(VarInsnNode(ASTORE, 3)) // [a]
					add(MONITOREXIT) // []
					add(RETURN)
					
					add(tc1H) // [a]
					add(POP) // []
					
					add(loopStart)
					add(VarInsnNode(ILOAD, 0)) // [i]
					add(DUP) // [i i]
					add(JumpInsnNode(IFLT, loopEnd)) // [i]
					add(tc2S)
					
					val labels = Array(keys.size) {
						newLabel()
					}
					val default = newLabel()
					add(TableSwitchInsnNode(
						0,
						keys.size - 1,
						default,
						*labels
					)) // []
					
					for ((i, label) in labels.withIndex().shuffled(random)) {
						add(label) // []
						add(ldcInt(keys[i] xor key)) // [i]
						add(VarInsnNode(ISTORE, 4)) // []
						add(VarInsnNode(ALOAD, 2)) // [a]
						add(MONITOREXIT) // []
						//add(RETURN)
						//add(JumpInsnNode(GOTO, setArr))
					}
					add(RETURN)
					
					add(tc2H) // [a]
					add(POP) // []
					add(setArr)
					add(VarInsnNode(ILOAD, 4)) // [i]
					add(VarInsnNode(ALOAD, 1)) // [a]
					add(DUP_X1)
					add(POP)
					add(ldcInt(key))
					add(IXOR)
					add(VarInsnNode(ILOAD, 0))
					add(SWAP)
					add(IASTORE)
					
					add(inc)
					add(VarInsnNode(ILOAD, 0))
					add(ldcInt(-1))
					add(IADD)
					add(VarInsnNode(ISTORE, 0))
					add(VarInsnNode(ALOAD, 3))
					add(MONITOREXIT)
					add(JumpInsnNode(GOTO, labels.random(random)))
					add(tc1E)
					
					add(loopEnd)
					add(InsnNode(RETURN))
					add(default)
					//add(POP)
					add(JumpInsnNode(GOTO, inc))
					add(tc2E)
				}
				classNode.methods.add(mn)
			}
		
		staticInit.instructions.insert(InsnList().apply {
			add(TypeInsnNode(NEW, OpaqueRuntimeManager.classNode.name))
			add(InsnNode(DUP))
			add(MethodInsnNode(INVOKESPECIAL, OpaqueRuntimeManager.classNode.name, "<init>", "()V"))
			add(FieldInsnNode(PUTSTATIC, classNode.name, storageField.name, storageField.desc))
		})
		
		return staticInit
	}
	
	data class EncryptedString(
		val original: String,
		val encrypted: String,
		val key: Int,
		val classNode: ClassNode,
		val methodNode: MethodNode,
		val insn: LdcInsnNode
	)
	
	private fun encryptString(original: String, key: Int, classNode: ClassNode, methodNode: MethodNode, insn: LdcInsnNode): EncryptedString {
		val classHash = classNode.name.replace('/', '.').hashCode()
		val methodHash = methodNode.name.replace('/', '.').hashCode()
		
		val old = original.toCharArray()
		val new = CharArray(original.length)
		
		for ((index, char) in old.withIndex()) {
			new[index] = when (index % 5) {
				0 -> char xor (4 + classHash)
				1 -> char xor (key)
				2 -> char xor (classHash)
				3 -> char xor (methodHash)
				4 -> char xor (methodHash + classHash)
				5 -> char xor (index + methodHash)
				else -> throw IllegalStateException("Impossible Value ($index % 6 = ${index % 6})")
			} xor keys[index % keys.size]
		}
		
		val asString = String(new)
		val decryptedAgain =
			decryptString(
				asString,
				key,
				classHash,
				methodHash
			)
		if (decryptedAgain != original) {
			throw IllegalStateException("Enc did not match {$asString} -> {$decryptedAgain}")
		}
		
		return EncryptedString(
			original,
			asString,
			key,
			classNode,
			methodNode,
			insn
		)
	}
	
	private fun decryptString(first: String, key: Int, classHash: Int, methodHash: Int): String {
		val old = first.toCharArray()
		val new = CharArray(first.length)
		
		for (i in 0 until (old.size)) {
			new[i] = when (i % 5) {
				0 -> old[i] xor (4 + classHash)
				1 -> old[i] xor key
				2 -> old[i] xor classHash
				3 -> old[i] xor methodHash
				4 -> old[i] xor (methodHash + classHash)
				5 -> old[i] xor (i + methodHash)
				else -> error("Invalid index $i")
			} xor keys[i % keys.size]
		}
		return String(new)
	}
	
	override val config: StringObfuscationConfiguration = rootConfig.stringObfuscation
}

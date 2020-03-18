package dev.binclub.binscure.runtime

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.CObfuscator.random
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.kotlin.add
import dev.binclub.binscure.kotlin.random
import dev.binclub.binscure.processors.renaming.generation.NameGenerator
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import kotlin.math.max

/**
 * @author cookiedragon234 11/Feb/2020
 */
object OpaqueRuntimeManager: IClassProcessor {
	lateinit var classes: MutableCollection<ClassNode>
	lateinit var classNode: ClassNode
	lateinit var consumeMethodName: String
	
	private val clinit by lazy {
		MethodNode(ACC_STATIC, "<clinit>", "()V", null, null).also {
			it.instructions.add(InsnNode(RETURN))
			classNode.methods.add(it)
		}
	}
	val fields by lazy {
		val num = max(classes.size / 2, 5)
		Array(num) { generateField() }
	}
	
	private val namer = NameGenerator()
	
	private fun generateField(): FieldInfo {
		val fieldNode = FieldNode(
			ACC_PUBLIC + ACC_STATIC,
			namer.uniqueRandomString(),
			"I",
			null,
			random.nextInt(Integer.MAX_VALUE)
		).also {
			classNode.fields.add(it)
		}
		return when (random.nextInt(6)) {
			0 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(0))
				FieldInfo(fieldNode, IFEQ, IFGT)
			}
			1 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(1))
				FieldInfo(fieldNode, IFGE, IFLT)
			}
			2 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(1))
				FieldInfo(fieldNode, IFGT, IFEQ)
			}
			3 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(-1))
				FieldInfo(fieldNode, IFLT, IFGE)
			}
			4 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(-1))
				FieldInfo(fieldNode, IFLE, IFGT)
			}
			5 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(-1))
				FieldInfo(fieldNode, IFNE, IFEQ)
			}
			else -> throw IllegalStateException()
		}
	}
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		this.classes = classes
		classNode = ClassNode().apply {
			this.access = ACC_PUBLIC
			this.version = OpaqueRuntimeManager.classes.first().version
			this.name = ClassRenamer.namer.uniqueRandomString()
			this.signature = null
			this.superName = "java/util/concurrent/ConcurrentHashMap"
			
			consumeMethodName = namer.uniqueRandomString()
			methods.add(makeConsumeMethod(consumeMethodName, "Z"))
			methods.add(makeConsumeMethod(consumeMethodName, "C"))
			methods.add(makeConsumeMethod(consumeMethodName, "B"))
			methods.add(makeConsumeMethod(consumeMethodName, "S"))
			methods.add(makeConsumeMethod(consumeMethodName, "I"))
			methods.add(makeConsumeMethod(consumeMethodName, "F"))
			methods.add(makeConsumeMethod(consumeMethodName, "J"))
			methods.add(makeConsumeMethod(consumeMethodName, "D"))
			methods.add(makeConsumeMethod(consumeMethodName, "Ljava/lang/Object;"))
		}
	}
	
	data class FieldInfo(val fieldNode: FieldNode, val trueOpcode: Int, val falseOpcode: Int)
	
	private fun makeConsumeMethod(name: String, type: String): MethodNode = MethodNode(
		ACC_PUBLIC + ACC_STATIC,
		name,
		"($type)V",
		null,
		null
	).apply {
		instructions = InsnList().apply {
			add(RETURN)
		}
	}
}

fun randomOpaqueJump(target: LabelNode, jumpOver: Boolean = true): InsnList {
	val field = OpaqueRuntimeManager.fields.random(CObfuscator.random)
	return InsnList().apply {
		add(FieldInsnNode(GETSTATIC, OpaqueRuntimeManager.classNode.name, field.fieldNode.name, field.fieldNode.desc))
		add(JumpInsnNode(
			if (jumpOver) field.trueOpcode else field.falseOpcode,
			target
		))
	}
}

fun opaqueSwitchJump(jumpSupplier: (LabelNode) -> InsnList = {
	randomOpaqueJump(it)
}): Pair<InsnList, InsnList> {
	val trueNum = randomInt()
	val falseNum = randomInt()
	val key = randomInt()
	val switch = newLabel()
	val trueLabel = newLabel()
	val falseLabel = newLabel()
	val deadLabel = newLabel()
	val list = InsnList().apply {
		val rand = randomBranch(
			random, {
				var dummyNum: Int
				do {
					dummyNum = randomInt()
				} while (dummyNum == trueNum || dummyNum == falseNum)
				
				add(jumpSupplier(falseLabel))
				add(ldcInt(trueNum xor key))
				add(JumpInsnNode(GOTO, switch))
				add(falseLabel)
				add(ldcInt(dummyNum xor key))
				add(switch)
				add(ldcInt(key))
				add(IXOR)
				add(constructLookupSwitch(
					trueLabel, if(random.nextBoolean()) arrayOf(
						trueNum to deadLabel, falseNum to falseLabel
					) else arrayOf(
						falseNum to falseLabel, trueNum to deadLabel
					)
				))
				add(trueLabel)
			}, {
				add(jumpSupplier(falseLabel))
				add(ldcInt(falseNum xor key))
				add(JumpInsnNode(GOTO, switch))
				add(falseLabel)
				add(ldcInt(trueNum xor key))
				add(switch)
				add(ldcInt(key))
				add(IXOR)
				add(constructLookupSwitch(
					deadLabel, if(random.nextBoolean()) arrayOf(
						trueNum to trueLabel, falseNum to falseLabel
					) else arrayOf(
						falseNum to falseLabel, trueNum to trueLabel
					)
				))
				add(trueLabel)
			}
		)
	}
	
	val otherList = InsnList().apply {
		add(deadLabel)
		add(ACONST_NULL)
		add(ATHROW)
	}
	
	return Pair(list, otherList)
}

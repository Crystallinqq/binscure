package cookiedragon.obfuscator.runtime

import cookiedragon.obfuscator.CObfuscator
import cookiedragon.obfuscator.IClassProcessor
import cookiedragon.obfuscator.classpath.ClassPath
import cookiedragon.obfuscator.kotlin.random
import cookiedragon.obfuscator.processors.renaming.generation.NameGenerator
import cookiedragon.obfuscator.processors.renaming.impl.ClassRenamer
import cookiedragon.obfuscator.utils.ldcInt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 11/Feb/2020
 */
object OpaqueRuntimeManager: IClassProcessor {
	lateinit var classes: MutableCollection<ClassNode>
	val classNode by lazy {
		ClassNode().apply {
			this.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
			this.version = classes.first().version
			this.name = ClassRenamer.namer.uniqueRandomString()
			this.signature = null
			this.superName = "java/lang/Object"
			classes.add(this)
			ClassPath.classes[this.name] = this
			ClassPath.classPath[this.name] = this
		}
	}
	val clinit by lazy {
		MethodNode(ACC_STATIC, "<clinit>", "()V", null, null).also {
			it.instructions.add(InsnNode(RETURN))
			classNode.methods.add(it)
		}
	}
	val fields by lazy {
		arrayOf(generateField(), generateField(), generateField())
	}
	
	val namer = NameGenerator()
	
	fun generateField(): FieldInfo {
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
				FieldInfo(fieldNode, IFEQ)
			}
			1 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(1))
				FieldInfo(fieldNode, IFGE)
			}
			2 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(1))
				FieldInfo(fieldNode, IFGT)
			}
			3 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(-1))
				FieldInfo(fieldNode, IFLT)
			}
			4 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(-1))
				FieldInfo(fieldNode, IFLE)
			}
			5 -> {
				clinit.instructions.insert(FieldInsnNode(PUTSTATIC, classNode.name, fieldNode.name, fieldNode.desc))
				clinit.instructions.insert(ldcInt(-1))
				FieldInfo(fieldNode, IFNE)
			}
			else -> throw IllegalStateException()
		}
	}
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		this.classes = classes
	}
	
	data class FieldInfo(val fieldNode: FieldNode, val trueOpcode: Int)
}

fun randomOpaqueJump(target: LabelNode): InsnList {
	val field = OpaqueRuntimeManager.fields.random(CObfuscator.random)
	return InsnList().apply {
		add(FieldInsnNode(GETSTATIC, OpaqueRuntimeManager.classNode.name, field.fieldNode.name, field.fieldNode.desc))
		add(JumpInsnNode(field.trueOpcode, target))
	}
}

package dev.binclub.binscure.processors.constants

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.utils.hasAccess
import dev.binclub.binscure.utils.getClinit
import dev.binclub.binscure.utils.ldcDouble
import dev.binclub.binscure.utils.ldcInt
import dev.binclub.binscure.utils.ldcLong
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

/**
 * This transformer removes the default values from fields and instead assigns their value inside the static initializer
 *
 * The following code:
 * ```Java
 * int i = 0;
 * ```
 *
 * becomes:
 * ```Java
 * int i;
 *
 * static {
 *  i = 0;
 * }
 * ```
 *
 * @author cookiedragon234 07/Mar/2020
 */
object FieldInitialiser: IClassProcessor {
	override val progressDescription: String
		get() = "Moving field constants to the static initializer"
	override val config: TransformerConfiguration
		get() = rootConfig
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		for (classNode in classes) {
			if (isExcluded(classNode))
				continue
			
			val staticFields = arrayListOf<FieldNode>()
			val instanceFields = arrayListOf<FieldNode>()
			for (field in classNode.fields) {
				if (isExcluded(classNode, field) || field.value == null)
					continue
				
				if (field.access.hasAccess(ACC_STATIC)) {
					staticFields
				} else {
					instanceFields
				}.add(field)
			}
			
			if (staticFields.isNotEmpty()) {
				val clinit = getClinit(classNode)
				clinit.instructions.apply {
					for (field in staticFields) {
						insert(FieldInsnNode(PUTSTATIC, classNode.name, field.name, field.desc))
						insert(when (field.value) {
							is Int -> ldcInt(field.value as Int)
							is Double -> ldcDouble(field.value as Double)
							is Long -> ldcLong(field.value as Long)
							else -> LdcInsnNode(field.value)
						})
						
						field.value = null
					}
				}
			}
			
			if (instanceFields.isNotEmpty()) {
				val list = InsnList().apply {
					for (field in instanceFields) {
						add(VarInsnNode(ALOAD, 0))
						add(when (field.value) {
							is Int -> ldcInt(field.value as Int)
							is Double -> ldcDouble(field.value as Double)
							is Long -> ldcLong(field.value as Long)
							else -> LdcInsnNode(field.value)
						})
						add(FieldInsnNode(Opcodes.PUTFIELD, classNode.name, field.name, field.desc))
						
						field.value = null
					}
				}
				
				for (method in classNode.methods) {
					if (method.name == "<init>") {
						method.instructions?.insert(list)
					}
				}
			}
		}
	}
}

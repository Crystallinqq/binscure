package dev.binclub.binscure.utils

import dev.binclub.binscure.kotlin.internalName
import dev.binclub.binscure.processors.renaming.utils.CustomRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

/**
 * @author cookiedragon234 24/Mar/2020
 */
object AnnotationFieldRemapper {
	fun remap(classNode: ClassNode, remapper: Remapper) {
		classNode.visibleAnnotations?.let {
			for (ann in it) {
				remap(ann, remapper)
			}
		}
		classNode.invisibleAnnotations?.let {
			for (ann in it) {
				remap(ann, remapper)
			}
		}
		classNode.visibleTypeAnnotations?.let {
			for (ann in it) {
				remap(ann, remapper)
			}
		}
		classNode.invisibleTypeAnnotations?.let {
			for (ann in it) {
				remap(ann, remapper)
			}
		}
	}
	
	fun remap(annotation: AnnotationNode, remapper: Remapper) {
		val values = annotation.values ?: return
		for (i in values.indices step 2) {
			val name = values[i] as String
			val value = values[i + 1]
			
			values[i] = remapNameValue(annotation, name, value, remapper)
		}
	}
	
	private fun remapNameValue(annotation: AnnotationNode, name: String, value: Any, remapper: Remapper): String {
		val desc = when (value) {
			is Array<*> -> {
				when {
					value.isArrayOf<String>() -> {
						value[0]
					}
					else -> throw IllegalStateException(value::class.internalName)
				}
			}
			is AnnotationNode -> {
				remap(value, remapper)
				value.desc
			}
			is List<*> -> {
				"L${value.firstOrNull()?.let {
					it::class.internalName
				} ?: value::class.internalName};"
			}
			else -> "L${value::class.internalName};"
		}.let { "()$it" }
		
		val owner = annotation.desc.removePrefix("L").removeSuffix(";")
		return remapper.mapMethodName(owner, name, desc)
	}
}

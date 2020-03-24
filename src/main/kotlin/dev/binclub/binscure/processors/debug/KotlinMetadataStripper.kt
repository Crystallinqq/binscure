package dev.binclub.binscure.processors.debug

import dev.binclub.binscure.CObfuscator
import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import org.objectweb.asm.tree.ClassNode
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.objectweb.asm.tree.AnnotationNode
import java.util.stream.Collectors

/**
 * @author cookiedragon234 22/Jan/2020
 */
object KotlinMetadataStripper: IClassProcessor {
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		if (!rootConfig.kotlinMetadata.enabled)
			return
		
		for (classNode in classes) {
			classNode.visibleAnnotations =
				classNode.visibleAnnotations
					?.stream()
					?.filter {it.desc != "Lkotlin/Metadata;"}
					?.collect(Collectors.toList())
			/*for (annotation in classNode.visibleAnnotations) {
				if (annotation.desc == "Lkotlin/Metadata;") {
					val header = createHeader(annotation)
					val metadata = KotlinClassMetadata.read(header)
					if (metadata is KotlinClassMetadata.Class) {
						val kClass = metadata.toKmClass()
					}
				}
			}*/
		}
	}
	
	private fun createHeader(node: AnnotationNode): KotlinClassHeader {
		var kind: Int? = null
		var metadataVersion: IntArray? = null
		var bytecodeVersion: IntArray? = null
		var data1: Array<String>? = null
		var data2: Array<String>? = null
		var extraString: String? = null
		var packageName: String? = null
		var extraInt: Int? = null
		
		val it = node.values.iterator()
		while (it.hasNext()) {
			val name = it.next() as String
			val value = it.next()
			
			when (name) {
				"k" -> kind = value as Int
				"mv" -> metadataVersion = listToIntArray(value)
				"bv" -> bytecodeVersion = listToIntArray(value)
				"d1" -> data1 = listToStringArray(value)
				"d2" -> data2 = listToStringArray(value)
				"xs" -> extraString = value as String
				"pn" -> packageName = value as String
				"xi" -> extraInt = value as Int
			}
		}
		
		return KotlinClassHeader(
			kind, metadataVersion, bytecodeVersion, data1, data2, extraString, packageName, extraInt
		)
	}
	@Suppress("UNCHECKED_CAST")
	private fun listToIntArray(list: Any): IntArray {
		return (list as List<Int>).stream().mapToInt { it }.toArray()
	}
	@Suppress("UNCHECKED_CAST")
	private fun listToStringArray(list: Any): Array<String> {
		return (list as List<String>).toTypedArray()
	}
}

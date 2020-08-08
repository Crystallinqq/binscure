package dev.binclub.binscure.classpath

import dev.binclub.binscure.classpath.tree.ClassTreeEntry
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import org.objectweb.asm.ByteVector
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Symbol
import java.lang.reflect.Modifier
import java.util.*

/**
 * @author cookiedragon234 23/Jan/2020
 */
class CustomClassWriter(flags: Int, verify: Boolean = true): ClassWriter(flags, verify) {
	init {
		if (rootConfig.watermark) {
			this.newUTF8("Protected by binclub.dev/binscure")
		}
	}
	
	
	override fun getCommonSuperClass(type1: String, type2: String): String {
		try {
			return super.getCommonSuperClass(type1, type2)
		} catch (ignored: Throwable){}
		
		return getCommonSuperClass1(type1, type2)
	}
	
	private fun getCommonSuperClass1(type1: String, type2: String): String {
		if (type1 == "java/lang/Object" || type2 == "java/lang/Object") {
			return "java/lang/Object"
		}
		val a = getCommonSuperClass0(type1, type2)
		val b = getCommonSuperClass0(type2, type1)
		if (a != "java/lang/Object") {
			return a
		}
		if (b != "java/lang/Object") {
			return b
		}
		val first = ClassPath.getHierarchy(type1)?.thisClass ?: return "java/lang/Object"
		val second = ClassPath.getHierarchy(type2)?.thisClass ?: return "java/lang/Object"
		return getCommonSuperClass(first.getDirectSuper() ?: return "java/lang/Object", second.getDirectSuper() ?: return "java/lang/Object")
	}
	
	private fun getCommonSuperClass0(type1_: String, type2: String): String {
		var type1 = type1_
		var first = ClassPath.getHierarchy(type1)?.thisClass ?: return "java/lang/Object"
		val second = ClassPath.getHierarchy(type2)?.thisClass ?: return "java/lang/Object"
		if (isAssignableFrom(type1, type2)) {
			return type1
		} else if (isAssignableFrom(type2, type1)) {
			return type2
		} else if (Modifier.isInterface(first.getAccess()) || Modifier.isInterface(second.getAccess())) {
			return "java/lang/Object"
		} else {
			do {
				type1 = first.getDirectSuper() ?: return "java/lang/Object"
				first = ClassPath.getHierarchy(type1)?.thisClass ?: return "java/lang/Object"
			} while (!isAssignableFrom(type1, type2))
			return type1
		}
	}
	
	private fun isAssignableFrom(type1: String, type2: String): Boolean {
		if (type1 == "java/lang/Object")
			return true
		if (type1 == type2)
			return true
		
		val firstTree = ClassPath.getHierarchy(type1) ?: return false
		val allChildren = mutableSetOf<String>()
		val toProcess = Stack<ClassTreeEntry>()
		toProcess.addAll(firstTree.children)
		while (!toProcess.isEmpty()) {
			val child = toProcess.pop()
			if (allChildren.add(child.getName())) {
				val tempTree = ClassPath.hierachy[child.getName()]
				if (tempTree != null) {
					toProcess.addAll(tempTree.children)
				}
			}
		}
		return allChildren.contains(type2)
	}
}

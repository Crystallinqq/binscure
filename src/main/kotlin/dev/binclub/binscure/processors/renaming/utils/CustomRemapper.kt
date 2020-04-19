package dev.binclub.binscure.processors.renaming.utils

import org.objectweb.asm.commons.Remapper

/**
 * @author Unix on 21.09.2019.
 */
class CustomRemapper : Remapper() {
	private val map = hashMapOf<String, String>()
	private val reversedMap = hashMapOf<String, String>()
	private val packageMap = hashMapOf<String, String>()
	private val reversedPackageMap = hashMapOf<String, String>()
	private val fieldMap = hashMapOf<String, MutableMap<String, String>>()
	private val reversedFieldMap = hashMapOf<String, MutableMap<String, String>>()
	private val methodMap = hashMapOf<String, MutableMap<String, String>>()
	private val reversedMethodMap = hashMapOf<String, MutableMap<String, String>>()
	
	override fun mapMethodName(owner: String, name: String, desc: String): String {
		val map = this.methodMap[this.map(owner)]
		
		if (map != null) {
			val data = map[name + this.mapDesc(desc)]
			
			if (data != null) {
				return data
			}
		}
		
		return name
	}
	
	fun mapMethodName(owner: String, oldName: String, oldDesc: String, newName: String, force: Boolean): Boolean {
		var methods: MutableMap<String, String>? = methodMap[this.map(owner)]
		var methodsRev: MutableMap<String, String>? = reversedMethodMap[this.map(owner)]
		
		if (methods == null) {
			methods = hashMapOf()
			methodMap[this.map(owner)] = methods
		}
		
		if (methodsRev == null) {
			methodsRev = hashMapOf()
			reversedMethodMap[this.map(owner)] = methodsRev
		}
		
		if (methodsRev.containsKey(newName + this.mapDesc(oldDesc)) && !force) {
			return false
		}
		
		methods[oldName + this.mapDesc(oldDesc)] = newName
		methodsRev[newName + this.mapDesc(oldDesc)] = oldName + this.mapDesc(oldDesc)
		return true
	}
	
	fun methodMappingExists(owner: String, oldName: String, oldDesc: String): Boolean {
		return methodMap[this.map(owner)]?.containsKey(oldName + this.mapDesc(oldDesc)) ?: false
	}
	
	fun newMethodMappingExists(owner: String, newName: String, oldDesc: String): Boolean {
		return reversedMethodMap[this.map(owner)]?.containsKey(newName + this.mapDesc(oldDesc)) ?: false
	}
	
	override fun mapInvokeDynamicMethodName(name: String, desc: String?): String {
		return name
	}
	
	override fun mapFieldName(owner: String, name: String, desc: String): String {
		val map = this.fieldMap[this.map(owner)]
		return map?.get(name + this.mapDesc(desc)) ?: name
	}
	
	fun mapFieldName(owner: String, oldName: String, oldDesc: String, newName: String, force: Boolean): Boolean {
		var fields: MutableMap<String, String>? = fieldMap[this.map(owner)]
		var fieldsRev: MutableMap<String, String>? = reversedFieldMap[this.map(owner)]
		
		if (fields == null) {
			fields = mutableMapOf()
			this.fieldMap[this.map(owner)] = fields
		}
		
		if (fieldsRev == null) {
			fieldsRev = mutableMapOf()
			this.reversedFieldMap[this.map(owner)] = fieldsRev
		}
		
		if (fieldsRev.containsKey(newName + this.mapDesc(oldDesc)) && !force) {
			return false
		}
		
		fields[oldName + this.mapDesc(oldDesc)] = newName
		fieldsRev[newName + this.mapDesc(oldDesc)] = oldName + this.mapDesc(oldDesc)
		return true
	}
	
	fun fieldMappingExists(owner: String, oldName: String, oldDesc: String): Boolean {
		return fieldMap[this.map(owner)]?.containsKey(oldName + this.mapDesc(oldDesc)) ?: false
	}
	
	fun newFieldMappingExists(owner: String, newName: String, oldDesc: String): Boolean {
		return reversedFieldMap[this.map(owner)]?.containsKey(newName + this.mapDesc(oldDesc)) ?: false
	}
	
	override fun map(name: String): String {
		return map.getOrDefault(name, name)
	}
	
	fun mapPackage(`in`: String): String {
		return this.packageMap.getOrDefault(`in`, `in`)
	}
	
	fun mapPackage(oldPackage: String, newPackage: String): Boolean {
		return if (!this.reversedPackageMap.containsKey(newPackage) && !this.packageMap.containsKey(oldPackage)) {
			this.reversedPackageMap[newPackage] = oldPackage
			this.packageMap[oldPackage] = newPackage
			true
		} else {
			false
		}
	}
	
	fun map(old: String, newName: String): Boolean {
		if (this.reversedMap.containsKey(newName)) {
			return false
		}
		
		this.map[old] = newName
		this.reversedMap[newName] = old
		return true
	}
	
	fun unmap(ref: String): String {
		return reversedMap[ref] ?: ref
	}
	
	fun dumpMappings(): Map<String, String> {
		val out = mutableMapOf<String, String>()
		
		out.putAll(map)
		out.putAll(packageMap)
		for ((className, fields) in fieldMap) {
			for (field in fields) {
				out[className + "." + field.key] = field.value
			}
		}
		for ((className, methods) in methodMap) {
			for (method in methods) {
				out[className + "." + method.key] = method.value
			}
		}
		
		return out
	}
}

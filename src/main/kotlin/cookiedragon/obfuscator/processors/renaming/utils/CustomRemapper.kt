package cookiedragon.obfuscator.processors.renaming.utils

import org.objectweb.asm.commons.Remapper

/**
 * @author Unix on 21.09.2019.
 */
class CustomRemapper : Remapper() {
	
	val map = mutableMapOf<String, String>()
	val mapReversed = mutableMapOf<String, String>()
	val packageMap = mutableMapOf<String, String>()
	val packageMapReversed = mutableMapOf<String, String>()
	val mapField = mutableMapOf<String, MutableMap<String, String>>()
	val mapFieldReversed = mutableMapOf<String, MutableMap<String, String>>()
	val mapMethod = mutableMapOf<String, MutableMap<String, String>>()
	val mapMethodReversed = mutableMapOf<String, MutableMap<String, String>>()
	
	override fun mapMethodName(owner: String, name: String, desc: String): String {
		val map = this.mapMethod[this.map(owner)]
		
		if (map != null) {
			val data = map[name + this.mapDesc(desc)]
			
			if (data != null) {
				return data
			}
		}
		
		return name
	}
	
	fun mapMethodName(owner: String, oldName: String, oldDesc: String, newName: String, force: Boolean): Boolean {
		var methods: MutableMap<String, String>? = mapMethod[this.map(owner)]
		var methodsRev: MutableMap<String, String>? = mapMethodReversed[this.map(owner)]
		
		if (methods == null) {
			methods = HashMap()
			mapMethod[this.map(owner)] = methods
		}
		
		if (methodsRev == null) {
			methodsRev = HashMap()
			mapMethodReversed[this.map(owner)] = methodsRev
		}
		
		if (methodsRev.containsKey(newName + this.mapDesc(oldDesc)) && !force) {
			return false
		}
		
		methods[oldName + this.mapDesc(oldDesc)] = newName
		methodsRev[newName + this.mapDesc(oldDesc)] = oldName + this.mapDesc(oldDesc)
		return true
	}
	
	fun methodMappingExists(owner: String, oldName: String, oldDesc: String): Boolean {
		return mapMethod[this.map(owner)]?.containsKey(oldName + this.mapDesc(oldDesc)) ?: false
	}
	
	fun newMethodMappingExists(owner: String, newName: String, oldDesc: String): Boolean {
		return mapMethodReversed[this.map(owner)]?.containsKey(newName + this.mapDesc(oldDesc)) ?: false
	}
	
	override fun mapInvokeDynamicMethodName(name: String, desc: String?): String {
		return name
	}
	
	override fun mapFieldName(owner: String, name: String, desc: String): String {
		val map = this.mapField[this.map(owner)]
		return map?.get(name + this.mapDesc(desc)) ?: name
	}
	
	fun mapFieldName(owner: String, oldName: String, oldDesc: String, newName: String, force: Boolean): Boolean {
		var fields: MutableMap<String, String>? = mapField[this.map(owner)]
		var fieldsRev: MutableMap<String, String>? = mapFieldReversed[this.map(owner)]
		
		if (fields == null) {
			fields = mutableMapOf()
			this.mapField[this.map(owner)] = fields
		}
		
		if (fieldsRev == null) {
			fieldsRev = mutableMapOf()
			this.mapFieldReversed[this.map(owner)] = fieldsRev
		}
		
		if (fieldsRev.containsKey(newName + this.mapDesc(oldDesc)) && !force) {
			return false
		}
		
		fields[oldName + this.mapDesc(oldDesc)] = newName
		fieldsRev[newName + this.mapDesc(oldDesc)] = oldName + this.mapDesc(oldDesc)
		return true
	}
	
	fun fieldMappingExists(owner: String, oldName: String, oldDesc: String): Boolean {
		return mapField[this.map(owner)]?.containsKey(oldName + this.mapDesc(oldDesc)) ?: false
	}
	
	fun newFieldMappingExists(owner: String, newName: String, oldDesc: String): Boolean {
		return mapFieldReversed[this.map(owner)]?.containsKey(newName + this.mapDesc(oldDesc)) ?: false
	}
	
	override fun map(name: String): String {
		return map.getOrDefault(name, name)
	}
	
	fun mapPackage(`in`: String): String {
		return this.packageMap.getOrDefault(`in`, `in`)
	}
	
	fun mapPackage(oldPackage: String, newPackage: String): Boolean {
		return if (!this.packageMapReversed.containsKey(newPackage) && !this.packageMap.containsKey(oldPackage)) {
			this.packageMapReversed[newPackage] = oldPackage
			this.packageMap[oldPackage] = newPackage
			true
		} else {
			false
		}
	}
	
	fun map(old: String, newName: String): Boolean {
		if (this.mapReversed.containsKey(newName)) {
			return false
		}
		
		this.map[old] = newName
		this.mapReversed[newName] = old
		return true
	}
	
	fun unmap(ref: String): String {
		return mapReversed[ref] ?: ref
	}
	
	fun dumpMappings(): Map<String, String> {
		val out = mutableMapOf<String, String>()
		
		out.putAll(map)
		out.putAll(packageMap)
		for ((className, fields) in mapField) {
			for (field in fields) {
				out[className + "." + field.key] = field.value
			}
		}
		for ((className, methods) in mapMethod) {
			for (method in methods) {
				out[className + "." + method.key] = method.value
			}
		}
		
		return out
	}
}

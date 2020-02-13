package org.objectweb.asm

/**
 * @author cookiedragon234 13/Feb/2020
 */
object SymbolTableAccessor {
	internal fun symbolTableFromClassWriter(classWriter: ClassWriter): SymbolTable {
		return with(ClassWriter::class.java.getDeclaredField("symbolTable")) {
			isAccessible = true
			get(classWriter) as SymbolTable
		}
	}
	
	internal fun getConstantPool(symbolTable: SymbolTable): ByteVector {
		return with(SymbolTable::class.java.getDeclaredField("constantPool")) {
			isAccessible = true
			get(symbolTable) as ByteVector
		}
	}
	
	internal fun enlarge(byteVector: ByteVector, size: Int) {
		with(ByteVector::class.java.getDeclaredMethod("enlarge")) {
			isAccessible = true
			invoke(byteVector, size)
		}
	}
	
	internal fun writeBadUtf(classWriter: ClassWriter) {
		val symbolTable = symbolTableFromClassWriter(classWriter)
		val constantPool = getConstantPool(symbolTable)
		
	}
	
	fun writeUtf(byteVector: ByteVector) {
		var strLength: Long = Integer.MAX_VALUE + 5L
		val charLength: Long = strLength
		var currentLength = strLength.toInt()
		if (currentLength + 2 + charLength > byteVector.data.size) {
			enlarge(byteVector, Integer.MAX_VALUE)
			enlarge(byteVector, 5 + 2)
		}
		val currentData: ByteArray = byteVector.data
		currentData[currentLength++] = (charLength ushr 8).toByte()
		currentData[currentLength++] = charLength.toByte()
		for (i in 0 until charLength) {
			val charValue: Char = 'a'
			currentData[currentLength++] = charValue.toByte()
		}
		byteVector.length = currentLength
	}
}

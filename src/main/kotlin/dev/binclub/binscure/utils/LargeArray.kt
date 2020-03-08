package dev.binclub.binscure.utils

import kotlin.math.floor

/**
 * @author cookiedragon234 25/Feb/2020
 */
class LargeArray {
	private var arrays: Array<Array<Byte?>> = Array(1) { arrayOfNulls<Byte>(1) }
	
	operator fun get(index: Int): Byte {
		val topLevel = floor(index / Integer.MAX_VALUE.toDouble()).toInt()
		val secondLevel = (index - (Integer.MAX_VALUE.toLong() * topLevel)).toInt()
		return arrays[topLevel][secondLevel] ?: throw ArrayIndexOutOfBoundsException("$topLevel -> $secondLevel")
	}
	
	operator fun set(index: Int, value: Byte) {
		val topLevel = floor(index / Integer.MAX_VALUE.toDouble()).toInt()
		val secondLevel = (index - (Integer.MAX_VALUE.toLong() * topLevel)).toInt()
		arrays[topLevel][secondLevel] = value
	}
}

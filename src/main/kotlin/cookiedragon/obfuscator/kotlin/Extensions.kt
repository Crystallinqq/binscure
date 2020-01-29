package cookiedragon.obfuscator.kotlin

import cookiedragon.obfuscator.classpath.ClassPath
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarIterable
import me.tongfei.progressbar.wrapped.ProgressBarWrappedIterator
import me.tongfei.progressbar.wrapped.ProgressBarWrappedSpliterator
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import java.security.SecureRandom
import java.util.*
import kotlin.reflect.KClass

/**
 * @author cookiedragon234 20/Jan/2020
 */
fun String.replaceLast(oldChar: Char, newChar: Char): String {
	return this.replaceLast(oldChar, newChar.toString())
}

fun String.replaceLast(oldChar: Char, newString: String): String {
	val index = lastIndexOf(oldChar)
	return when (index) {
		-1 -> this
		else -> this.replaceRange(index, index + 1, newString)
	}
}

infix fun Char.xor(int: Int): Char {
	return (this.toInt() xor int).toChar()
}

fun KClass<*>.getDescriptor(): String = Type.getDescriptor(this.java)

fun CharArray.random(random: SecureRandom): Char {
	if (isEmpty())
		throw NoSuchElementException("Array is empty.")
	return get(random.nextInt(size))
}

fun <K, V> MutableMap<K, V>.getOrPut(key: K, default: V): V {
	var out = this[key]
	if (out == null) {
		this[key] = default!!
		out = default
	}
	return out
}

fun <K, V> MutableMap<K, V>.getOrPutLazy(key: K, default: () -> V): V {
	var out = this[key]
	if (out == null) {
		out = default()!!
		this[key] = out
	}
	return out
}

fun <T> ProgressBar.wrap(sp: Spliterator<T>): Spliterator<T> {
	val size = sp.exactSizeIfKnown
	if (size != -1L)
		this.maxHint(size)
	return ProgressBarWrappedSpliterator(sp, this)
}

fun <T> ProgressBar.wrap(it: Iterator<T>): Iterator<T> {
	return ProgressBarWrappedIterator<T>(it, this)
}

fun <T> ProgressBar.wrap(it: Iterable<T>): Iterable<T> {
	return ProgressBarIterable(it, this)
}

fun <K, V> ProgressBar.wrap(it: Map<K, V>): Iterator<Map.Entry<K, V>> {
	return ProgressBarWrappedIterator(it.iterator(), this)
}

fun String.clampStart(length: Int, padding: Char = ' '): String {
	return when {
		this.length == length -> this
		this.length > length -> this.substring(0, length)
		else -> this.padStart(length, padding)
	}
}

fun String.clampEnd(length: Int, padding: Char = ' '): String {
	return when {
		this.length == length -> this
		this.length > length -> this.substring(0, length)
		else -> this.padEnd(length, padding)
	}
}

fun <K, V> Map<K, V>.toPrettyString(): String {
	val sb = StringBuilder("Map(${this.size}):\n")
	for (entry in this) {
		sb.append('\t')
		sb.append(entry.key)
		sb.append("=")
		if (entry.value is Map<*, *>) {
			sb.append((entry.value as Map<*, *>).toPrettyString().prependIndent("\t"))
		} else {
			sb.append(entry.value)
		}
		sb.append('\n')
	}
	return sb.removeSuffix("\n").toString()
}

val ClassNode.originalName: String?
	get() = ClassPath.originalNames[this]

fun <T> Collection<T>.random(random: SecureRandom): T {
	if (isEmpty())
		throw NoSuchElementException("Collection is empty.")
	return elementAt(random.nextInt(size))
}

fun <T> Array<T>.random(random: SecureRandom): T {
	if (isEmpty())
		throw NoSuchElementException("Collection is empty.")
	return elementAt(random.nextInt(size))
}

val <T: Any> KClass<T>.internalName: String
	get() = Type.getInternalName(this.java)

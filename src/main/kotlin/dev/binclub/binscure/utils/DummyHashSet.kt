package dev.binclub.binscure.utils

/**
 * @author cookiedragon234 08/Mar/2020
 */
class DummyHashSet<T> : HashSet<T>() {
	override fun add(element: T): Boolean = true
}

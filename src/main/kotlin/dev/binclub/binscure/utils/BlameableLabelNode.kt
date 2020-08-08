package dev.binclub.binscure.utils

import org.objectweb.asm.Label
import org.objectweb.asm.tree.LabelNode

/**
 * @author cookiedragon234 24/Feb/2020
 */
private const val DEBUG = false

class BlameableLabelNode(label: Label) : LabelNode(label) {
	constructor() : this(Label())
	
	private val stackTrace: Array<StackTraceElement>? = if (DEBUG) Exception().stackTrace else null
	
	fun print(msg: String = "") {
		System.err.println("Label Blame: $msg")
		for (stackTraceElement in stackTrace!!) {
			System.err.println("\t$stackTraceElement")
		}
	}
	
	override fun toString(): String {
		return "BLabelNode ${stackTrace!![2].lineNumber}"
	}
}

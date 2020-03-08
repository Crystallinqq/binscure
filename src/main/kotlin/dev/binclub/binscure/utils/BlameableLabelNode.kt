package dev.binclub.binscure.utils

import org.objectweb.asm.Label
import org.objectweb.asm.tree.LabelNode
import java.lang.Exception
import java.lang.RuntimeException

/**
 * @author cookiedragon234 24/Feb/2020
 */
class BlameableLabelNode(label: Label): LabelNode(label) {
	constructor(): this(Label())
	
	val stackTrace = Exception().stackTrace
	
	fun print(msg: String = "") {
		System.err.println("Label Blame: $msg")
		for (stackTraceElement in stackTrace) {
			System.err.println("\t${stackTraceElement.toString()}")
		}
	}
	
	override fun toString(): String {
		return "BLabelNode ${stackTrace[2].lineNumber}"
	}
}

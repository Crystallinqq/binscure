package dev.binclub.binscure.utils

import org.objectweb.asm.tree.*

/**
 * @author cookiedragon234 24/Feb/2020
 */
object ClassVerifier {
	fun verifyMethod(methodNode: MethodNode) {
		val errors = hashSetOf<String>()
		val encounteredLabels = hashSetOf<LabelNode>()
		val requiredLabels = hashMapOf<LabelNode, String>()
		
		methodNode.instructions?.let {
			for ((i, insn) in it.withIndex()) {
				when (insn) {
					is LabelNode, is BlameableLabelNode -> {
						if (!encounteredLabels.add(insn as LabelNode)) {
							errors.add("Encountered LabelNode twice (Index $i)")
						}
						requiredLabels.remove(insn)
					}
					is JumpInsnNode -> {
						val lbl = insn.label
						
						if (!encounteredLabels.contains(lbl)) {
							requiredLabels[lbl] = "Referenced by JumpInsnNode at $i"
						}
					}
					is TableSwitchInsnNode -> {
						for ((lblI, lbl) in (insn.labels + insn.dflt).withIndex()) {
							if (!encounteredLabels.contains(lbl)) {
								requiredLabels[lbl] = "Referenced by TableSwitchInsnNode at $i slot $lblI"
							}
						}
						
						val diff = insn.max - insn.min + 1
						
						if (diff < 0) {
							errors.add("Bad Table Switch (Negative size) $diff max: ${insn.max} min: ${insn.min} at $i")
						}
						
						if (insn.labels.size != diff) {
							errors.add("Bad Table Switch (Size mismatch) Diff: $diff, size ${insn.labels.size} at $i")
						}
					}
					is LookupSwitchInsnNode -> {
						for (lbl in insn.labels + insn.dflt) {
							if (!encounteredLabels.contains(lbl)) {
								requiredLabels[lbl] = "Referenced by LookupSwitchInsnNode at $i"
							}
						}
						
						if (insn.labels.size != insn.keys.size) {
							errors.add("Bad Lookup Switch (Unmatching sizes) at $i")
						}
					}
					is LineNumberNode -> {
						val lbl = insn.start
						if (!encounteredLabels.contains(lbl)) {
							requiredLabels[lbl] = "Referenced by LineNumberNode at $i"
						}
					}
				}
			}
		}
		
		methodNode.localVariables?.let {
			for ((i, localVar) in it.withIndex()) {
				if (!encounteredLabels.contains(localVar.start)) {
					requiredLabels[localVar.start] = "Referenced by LocalVariable start at $i"
				}
				if (!encounteredLabels.contains(localVar.end)) {
					requiredLabels[localVar.end] = "Referenced by LocalVariable end at $i"
				}
			}
		}
		
		methodNode.tryCatchBlocks?.let {
			for ((i, tryCatch) in it.withIndex()) {
				if (!encounteredLabels.contains(tryCatch.start)) {
					requiredLabels[tryCatch.start] = "Referenced by TryCatchBlock start at $i"
				}
				if (!encounteredLabels.contains(tryCatch.handler)) {
					requiredLabels[tryCatch.handler] = "Referenced by TryCatchBlock handler at $i"
				}
				if (!encounteredLabels.contains(tryCatch.end)) {
					requiredLabels[tryCatch.end] = "Referenced by TryCatchBlock end at $i"
				}
			}
		}
		
		for ((label, msg) in requiredLabels) {
			errors.add("Couldnt find label, $msg")
			if (label is BlameableLabelNode) {
				label.print(msg)
			}
		}
		for (lbl in encounteredLabels) {
			println("\t-" + lbl.toString())
		}
		
		if (errors.isNotEmpty()) {
			println(methodNode.instructions?.toOpcodeStrings())
			for (error in errors) {
				println(error)
			}
			println("${errors.size} errors in ${methodNode.name}, Encountered ${encounteredLabels.size} labels, required ${requiredLabels.size} labels")
		} else {
			println("No errors in ${methodNode.name}, Encountered ${encounteredLabels.size} labels, required ${requiredLabels.size} labels")
		}
	}
	
	fun verifyClass(classNode: ClassNode) {
		for (method in classNode.methods) {
			verifyMethod(method)
		}
		println("Verified ${classNode.name}")
	}
}

private fun InsnList.withIndex(): Iterator<IndexedValue<AbstractInsnNode>> {
	return this.iterator().withIndex()
}

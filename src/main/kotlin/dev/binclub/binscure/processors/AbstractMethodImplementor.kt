package dev.binclub.binscure.processors

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.utils.hasAccess
import dev.binclub.binscure.utils.insnBuilder
import dev.binclub.binscure.utils.insnListOf
import dev.binclub.binscure.utils.removeAccess
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.UnsupportedOperationException

/**
 * @author cookiedragon234 26/Jul/2020
 */
object AbstractMethodImplementor: IClassProcessor {
	override val progressDescription: String
		get() = "Generating implementations for abstract methods"
	override val config: TransformerConfiguration = rootConfig
	
	override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
		forClass(classes) { cn ->
			if (!cn.access.hasAccess(ACC_ANNOTATION)) {
				if (cn.access.hasAccess(ACC_ABSTRACT) && !cn.access.hasAccess(ACC_INTERFACE))
					cn.access = cn.access.removeAccess(ACC_ABSTRACT)
				
				forMethod(cn) { method ->
					if (method.access.hasAccess(ACC_ABSTRACT)) {
						method.access = method.access.removeAccess(ACC_ABSTRACT)
						
						if (method.instructions == null || method.instructions.size() <= 0) {
							method.tryCatchBlocks = null
							method.instructions = insnBuilder {
								+TypeInsnNode(NEW, "java/lang/UnsupportedOperationException")
								+InsnNode(DUP)
								+LdcInsnNode("Please report this to the binscure obfuscator developers")
								+MethodInsnNode(
									INVOKESPECIAL,
									"java/lang/UnsupportedOperationException",
									"<init>",
									"(Ljava/lang/String;)V"
								)
								+InsnNode(ATHROW)
							}
						}
					}
				}
			}
		}
	}
}

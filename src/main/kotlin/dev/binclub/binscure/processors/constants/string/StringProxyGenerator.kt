package dev.binclub.binscure.processors.constants.string

import dev.binclub.binscure.classpath.ClassPath
import dev.binclub.binscure.configuration.ConfigurationManager
import dev.binclub.binscure.processors.exploit.BadAttributeExploit
import dev.binclub.binscure.processors.exploit.BadClinitExploit
import dev.binclub.binscure.processors.renaming.impl.ClassRenamer
import dev.binclub.binscure.processors.runtime.OpaqueRuntimeManager
import dev.binclub.binscure.utils.hasAccess
import dev.binclub.binscure.utils.insnBuilder
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * @author cookiedragon234 07/Aug/2020
 */
object StringProxyGenerator {
	var classNode: ClassNode? = null
	
	fun generateStringProxy(actual: ClassNode, decryptMethod: MethodNode, simpleDecryptMethod: MethodNode, resourceName: String): ClassNode {
		classNode?.let {
			return it
		}
		
		val classNode = ClassNode()
			.apply {
				this.access = ACC_PUBLIC + ACC_FINAL
				this.version = V1_8
				this.name = ClassRenamer.namer.uniqueUntakenClass()
				this.signature = null
				this.superName = "java/lang/Object"
				ClassPath.classes[this.name] = this
				ClassPath.classPath[this.name] = this
				if (ConfigurationManager.rootConfig.crasher.enabled && ConfigurationManager.rootConfig.crasher.antiAsm) {
					BadAttributeExploit.process(Collections.singleton(this), Collections.emptyMap())
				}
				//BadClinitExploit.process(Collections.singleton(this), Collections.emptyMap())
			}
		
		classNode.methods.add(MethodNode(
			decryptMethod.access,
			decryptMethod.name,
			decryptMethod.desc,
			decryptMethod.signature,
			null
		).apply {
			instructions = insnBuilder {
				aload(0)
				iload(1)
				iconst_1()
				iadd()
				invokestatic(
					actual.name,
					decryptMethod.name,
					decryptMethod.desc
				)
				areturn()
			}
		})
		
		classNode.methods.add(MethodNode(
			simpleDecryptMethod.access,
			simpleDecryptMethod.name,
			simpleDecryptMethod.desc,
			simpleDecryptMethod.signature,
			null
		).apply {
			instructions = insnBuilder {
				aload(0)
				iconst_3()
				invokestatic(
					actual.name,
					decryptMethod.name,
					decryptMethod.desc
				)
				areturn()
			}
		})
		
		genClinit(classNode, actual, simpleDecryptMethod, resourceName)
		
		this.classNode = classNode
		return classNode
	}
	
	fun genClinit(proxy: ClassNode, actual: ClassNode, simpleDecryptMethod: MethodNode, resourceName: String) {
		val mn = MethodNode(
			ACC_STATIC,
			"<clinit>",
			"()V",
			null,
			null
		)
		
		mn.instructions = insnBuilder {
			_return()
		}
		
		proxy.methods.add(mn)
	}
	
	fun addToBootstrapClassLoader() {
		val name = "dev/binclub/example"
		val resource = StringProxyGenerator::class.java.classLoader.getResourceAsStream("$name.class")!!
		
		val bos = ByteArrayOutputStream()
		val data = ByteArray(16384)
		
		while (true) {
			val read = resource.read(data)
			if (read == -1) {
				break
			}
			bos.write(data, 0, read)
		}
		
		val bytes = bos.toByteArray()
		
		val defineClassMethod = ClassLoader::class.java.getDeclaredMethod("defineClass", ByteArray::class.java, Int::class.java, Int::class.java)
		defineClassMethod.isAccessible = true
		
		val sysCl = ClassLoader.getSystemClassLoader()
		
		defineClassMethod.invoke(sysCl, bytes, 0, bytes.size)
	}
}

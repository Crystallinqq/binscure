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
				this.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
				this.version = Opcodes.V1_8
				this.name = ClassRenamer.namer.uniqueRandomString()
				this.signature = null
				this.superName = "java/lang/Object"
				ClassPath.classes[this.name] = this
				ClassPath.classPath[this.name] = this
				if (ConfigurationManager.rootConfig.crasher.enabled && ConfigurationManager.rootConfig.crasher.antiAsm) {
					BadAttributeExploit.process(Collections.singleton(this), Collections.emptyMap())
				}
				BadClinitExploit.process(Collections.singleton(this), Collections.emptyMap())
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
		
		/*mn.instructions = insnBuilder {
			ldc("dontinline,${actual.name}.${simpleDecryptMethod.name},${simpleDecryptMethod.desc}")
			dup()
			getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
			swap()
			invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
			invokestatic(
				"java/lang/Compiler",
				"command",
				"(Ljava/lang/Object;)Ljava/lang/Object;"
			)
			getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
			swap()
			invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
			
			
			ldc(Type.getType("L${proxy.name};"))
			invokevirtual(
				"java/lang/Class",
				"getClassLoader",
				"()Ljava/lang/ClassLoader;"
			)
			ldc(resourceName)
			invokevirtual(
				"java/lang/ClassLoader",
				"getResourceAsStream",
				"(Ljava/lang/String;)Ljava/io/InputStream;"
			)
			dup()
			astore(0)
			ldc("Failed to locate a vital binscure class ($resourceName)")
			invokestatic(
				"java/util/Objects",
				"requireNonNull",
				"(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"
			)
			
			new("java/io/ByteArrayOutputStream")
			dup()
			invokespecial(
				"java/io/ByteArrayOutputStream",
				"<init>",
				"()V"
			)
			astore(1)
			
			ldc(16384)
			newbytearray()
			astore(2)
			
			val loop = LabelNode()
			val loopBreak = LabelNode()
			+loop
			
			aload(0) // Input Stream
			aload(2) // Byte Array
			invokevirtual(
				"java/io/InputStream",
				"read",
				"([B)I"
			)
			dup()
			istore(3)
			ldc(-1)
			if_icmpeq(loopBreak)
			
			aload(1) // Byte array output stream
			aload(2) // byte array
			ldc(0) // offset
			iload(3) // size
			invokevirtual(
				"java/io/ByteArrayOutputStream",
				"write",
				"([BII)V"
			)
			
			goto(loop)
			+loopBreak
			
			aload(1) // Byte array output stream
			invokevirtual(
				"java/io/ByteArrayOutputStream",
				"toByteArray",
				"()[B"
			)
			astore(2) // byte array
			
			ldc(Type.getType(ClassLoader::class.java))
			ldc("defineClass")
			
			ldc(3)
			anewarray("java/lang/Class")
			dup()
			
			ldc(0)
			ldc(Type.getType(ByteArray::class.java))
			aastore()
			dup()
			
			ldc(1)
			getstatic(
				"java/lang/Integer",
				"TYPE",
				"Ljava/lang/Class;"
			)
			aastore()
			dup()
			
			ldc(2)
			getstatic(
				"java/lang/Integer",
				"TYPE",
				"Ljava/lang/Class;"
			)
			aastore()
			
			invokevirtual(
				"java/lang/Class",
				"getDeclaredMethod",
				"(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"
			)
			dup()
			astore(0) // define class method
			
			ldc(1)
			invokevirtual(
				"java/lang/reflect/AccessibleObject",
				"setAccessible",
				"(Z)V"
			)
			
			aload(0) // define class method
			invokestatic(
				"java/lang/ClassLoader",
				"getSystemClassLoader",
				"()Ljava/lang/ClassLoader;"
			)
			
			ldc(3)
			anewarray("java/lang/Object")
			dup()
			
			ldc(0)
			aload(2) // byte array
			aastore()
			dup()
			
			ldc(1)
			ldc(0) // offset
			invokestatic(
				"java/lang/Integer",
				"valueOf",
				"(I)Ljava/lang/Integer;"
			)
			aastore()
			dup()
			
			ldc(2)
			aload(2) // byte array
			arraylength()
			invokestatic(
				"java/lang/Integer",
				"valueOf",
				"(I)Ljava/lang/Integer;"
			)
			aastore()
			
			invokevirtual(
				"java/lang/reflect/Method",
				"invoke",
				"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"
			)
			
			_return()
		}*/
		
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

import dev.binclub.binscure.processors.exploit.BadAttributeExploit
import dev.binclub.binscure.utils.insnBuilder
import dev.binclub.binscure.utils.printlnAsm
import dev.binclub.binscure.utils.toOpcodeStrings
import org.objectweb.asm.Attribute
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import java.io.File
import java.io.PrintStream
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.random.Random

/**
 * @author cookiedragon234 09/Aug/2020
 */


/*

Input: 64bit unsigned long
2 sec delay on startup to prevent brute forcing


Successful output: "Correct!"
Unsuccessful output: "Incorrect"



 */
fun main() {
	//val main = genMainClass()
	val main = ClassNode().apply {
		name = "Test"
		superName = "java/lang/Object"
		version = V1_8
		
		methods.add(MethodNode().apply {
			access = ACC_STATIC or ACC_PUBLIC
			name = "main"
			desc = "([Ljava/lang/String;)V"
			
			instructions = insnBuilder {
				+printlnAsm("hi")
				_return()
			}
		})
		
		attrs = attrs ?: arrayListOf()
		attrs.add(DummyAttribute("Module"))
	}
	val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
	main.accept(cw)
	val bytes = cw.toByteArray()
	File("Test.class").writeBytes(bytes)
	
	ClassReader(bytes).accept(ClassNode(), ClassReader.SKIP_FRAMES)
}

private class DummyAttribute(name: String, bytes: ByteArray = ByteArray(Random.nextInt(2))): Attribute(name) {
	init {
		content = bytes
	}
}


fun main1() {
	//val main = genMainClass()
	val main = ClassNode().apply {
		name = "Test"
		superName = "sun/reflect/MagicAccessorImpl"
		version = V1_8
		
		methods.add(MethodNode().apply {
			access = ACC_STATIC or ACC_PUBLIC
			name = "main"
			desc = "([Ljava/lang/String;)V"
			
			val ts = LabelNode()
			val te = LabelNode()
			val th = LabelNode()
			instructions = insnBuilder {
				+ts
				ldc("hi")
				athrow()
				+te
				+th
				getstatic(System::class, System::out)
				swap()
				invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
				_return()
			}
			tryCatchBlocks = arrayListOf(TryCatchBlockNode(ts, te, th, "java/lang/String"))
		})
	}
	val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
	main.accept(cw)
	File("Test.class").writeBytes(cw.toByteArray())
	
	val nodes = arrayOf(main)
	
	/*val manifest = Manifest()
	manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
	manifest.mainAttributes[Attributes.Name.MAIN_CLASS] = main.name.replace('/', '.')
	JarOutputStream(File("crackme.jar").outputStream(), manifest).use { jar ->
		
		nodes.forEach { cn ->
			val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
			cn.accept(cw)
			
			jar.putNextEntry(JarEntry("${cn.name}.class"))
			jar.write(cw.toByteArray())
		}
	}*/
}

fun genProcessMethod(mainCn: ClassNode): MethodNode
	= MethodNode(
		ACC_STATIC,
		"0",
		"(Ljava/lang/String;)Ljava/lang/String;",
		null,
		null
	).apply {
		instructions = insnBuilder {
			aconst_null()
			aload(0)
			areturn()
		}
	}

fun genMainClass(): ClassNode {
	val cn = ClassNode().apply {
		name = "dev/binclub/crackme/01/main"
		superName = "java/lang/Object"
		version = V1_8
	}
	
	val process = genProcessMethod(cn)
	cn.methods.add(process)
	
	val main = MethodNode(
		ACC_PUBLIC or ACC_STATIC,
		"main",
		"([Ljava/lang/String;)V",
		null,
		null
	)
	cn.methods.add(main)
	
	main.instructions = insnBuilder {
		val out = LabelNode()
		val start = LabelNode()
		val ret = LabelNode()
		val dflt = LabelNode()
		val labels = arrayOf(LabelNode(), LabelNode())
		
		iconst_0()
		tableswitch(0, dflt, *labels)
		+ret
		aconst_null()
		athrow()
		+dflt
		aload(0)
		ifnull(ret)
		_return()
		+labels[0]
		aload(0)
		ifnonnull(ret)
		_return()
		+labels[1]
		aload(0)
		ifnull(ret)
		_return()
		
		_return()
		/*
		invokestatic(System::class, System::currentTimeMillis)
		i2l()
		istore(1)
		invokestatic(Runtime::class, Runtime::getRuntime)
		astore(2)
		iload(1)
		+start
		lookupswitch(dflt, arrayOf(0 to labels[0], 1 to labels[1]))
		_return()
		+ret
		ifnull(dflt)
		invokestatic(System::class, System::currentTimeMillis)
		i2l()
		istore(1)
		//goto(start)
		_return()
		+dflt
		invokestatic(Runtime::class, Runtime::getRuntime)
		astore(2)
		aload(2)
		ifnull(dflt)
		iload(1)
		goto(start)
		//aload(0)
		//astore(2)
		//goto(start)
		_return()
		+labels[0]
		invokestatic(System::class, System::currentTimeMillis)
		i2l()
		istore(1)
		iload(1)
		aload(2)
		ifnull(start)
		//aload(2)
		//ifnonnull(start)
		//_return()
		+labels[1]
		invokestatic(System::class, System::getSecurityManager)
		astore(2)
		iload(1)
		aload(2)
		ifnull(start)
		//aload(2)
		//ifnull(ret)
		//iconst_m1()
		//istore(1)
		_return()*/
		
		
		/*aload(0)
		arraylength()
		ldc(1) // array.length must eq 1
		val lengthCorrect = LabelNode()
		if_icmpeq(lengthCorrect)
		
		getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
		ldc("Expects an unsigned 64bit long as argument")
		invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
		_return()
		
		+lengthCorrect
		
		aload(0)
		ldc(0)
		aaload()
		invokestatic(
			cn.name,
			process.name,
			process.desc
		)
		astore(2)
		
		ldc("Correct!")
		aload(2)
		invokevirtual("java/lang/Object", "equals", "(Ljava/lang/Object;)Z")
		val falseJmp = LabelNode()
		ifeq(falseJmp)
		
		getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
		aload(2)
		invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
		_return()
		
		+falseJmp
		
		getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
		ldc("Incorrect")
		invokevirtual("java/io/PrintStream", "append", "(Ljava/lang/CharSequence;)Ljava/io/PrintStream;")
		_return()*/
	}
	
	return cn
}

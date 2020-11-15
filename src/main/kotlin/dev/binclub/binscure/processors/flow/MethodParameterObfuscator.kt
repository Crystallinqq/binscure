package dev.binclub.binscure.processors.flow

import dev.binclub.binscure.IClassProcessor
import dev.binclub.binscure.api.TransformerConfiguration
import dev.binclub.binscure.configuration.ConfigurationManager.rootConfig
import dev.binclub.binscure.forClass
import dev.binclub.binscure.forMethod
import dev.binclub.binscure.processors.renaming.generation.NameGenerator
import dev.binclub.binscure.utils.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*
import java.util.*


object MethodParameterObfuscator: IClassProcessor {
    override val progressDescription: String
        get() = "Obfuscating method parameters"
    override val config: TransformerConfiguration
        get() = rootConfig
    private fun check(desc: Type): Boolean {
        val types = desc.argumentTypes

        if (types.size > Int.SIZE_BITS) return false
        return types.any {
            it.sort in Type.BOOLEAN..Type.ARRAY || (it.sort == Type.OBJECT && it.internalName == "java/lang/String")
        }
    }
    private data class Modification(val cn: ClassNode, val mn: MethodNode, val proxy: MethodNode, val md: List<Pair<Int, Int?>? /* 0: Negate (Boolean) [1: Add 2: Subtract 3: Extend(String) + operand]*/>)
    override fun process(classes: MutableCollection<ClassNode>, passThrough: MutableMap<String, ByteArray>) {
        val modifications : MutableList<Modification> = arrayListOf()
        val proxiesSet = hashSetOf<String>()
        forClass(classes) { cn ->
            val proxies: ArrayList<MethodNode> = arrayListOf()
            val namer = NameGenerator()
            forMethod(cn) { mn ->
                val type = Type.getType(mn.desc)
                if (!check(type))
                    return@forMethod
                if (cn.methods.size + proxies.size >= 65535)
                    return@forClass
                if (mn.name[0] == '<')
                    return@forMethod
                if (mn.access and (ACC_NATIVE or ACC_ABSTRACT) != 0)
                    return@forMethod
                val arguments = type.argumentTypes
                val mods = arrayListOf<Pair<Int, Int?>?>()
                arguments.forEach {
                    when (it.sort) {
                        Type.BOOLEAN -> mods.add(0 to null)
                        Type.INT -> mods.add((random.nextInt(1) + 1) to random.nextInt(Int.MAX_VALUE))
                        Type.OBJECT -> {
                            if (it.internalName == "java/lang/String")
                                mods.add(3 to random.nextInt(15))
                            else mods.add(null)
                        }
                        Type.DOUBLE, Type.LONG -> mods.addAll(listOf(null, null)) // Double Sized
                        else -> mods.add(null)
                    }
                }

                val static = mn.access and ACC_STATIC != 0
                val proxy = MethodNode(mn.access, namer.uniqueUntakenMethodName(cn), mn.desc.replace(")", "I)"), null, arrayOf()).apply {
                    instructions = insnBuilder {
                        iload( arguments.sumBy(Type::getSize) + if (static) 0 else 1)
                        var i = 1
                        for ((idx, m) in mods.withIndex()) {
                            if (m == null) {
                                i = i shl 1
                                continue
                            }
                            dup() // x, x
                            val ln = newLabel()
                            ldc(i) // x, x, i
                            iand() // check bitflag
                            ifeq(ln) // jump to end if bitflag is not set
                            val (mType, op) = m
                            if (mType in 0..2)
                                iload(idx + if (static) 0 else 1)
                            else
                                aload(idx + if (static) 0 else 1) // String
                            when (mType) {
                                0 -> {
                                    val lbl1 = newLabel()
                                    val lbl2 = newLabel()
                                    ifne(lbl1)
                                    ldc(1)
                                    goto(lbl2)
                                    +lbl1
                                    ldc(0)
                                    +lbl2
                                }
                                1 -> {
                                    ldc(op as Int)
                                    isub()
                                }
                                2 -> {
                                    ldc(op as Int)
                                    iadd()
                                }
                                3 -> {
                                    dup() // str, str
                                    invokevirtual("java/lang/String", "length", "()I")
                                    iconst_0() // str, strlen, 0
                                    swap() // str, 0, strlen
                                    ldc(op as Int) // str, 0, strlen, op
                                    isub() // str, 0, strlen - op
                                    invokevirtual("java/lang/String", "substring", "(II)Ljava/lang/String;") // substr
                                }
                                else -> error("unreachable")
                            }
                            if (mType in 0..2)
                                istore(idx + if (static) 0 else 1)
                            else
                                astore(idx + if (static) 0 else 1) // String
                            +ln
                            i = i shl 1
                        }
                        pop()
                        if (mn.access and ACC_STATIC != 0) {
                            var ii = 0
                            arguments.forEach { t ->
                                when (t.sort) {
                                    Type.INT, Type.CHAR, Type.BYTE, Type.BOOLEAN, Type.SHORT -> iload(ii)
                                    Type.OBJECT, Type.ARRAY -> aload(ii)
                                    Type.FLOAT -> fload(ii)
                                    Type.DOUBLE -> dload(ii)
                                    Type.LONG -> lload(ii)
                                }
                                ii += t.size
                            }
                            invokestatic(cn.name, mn.name, mn.desc)
                        } else {
                            aload(0)
                            var ii = 0
                            arguments.forEach { t ->
                                when (t.sort) {
                                    Type.INT, Type.CHAR, Type.BYTE, Type.BOOLEAN, Type.SHORT -> iload(ii + 1)
                                    Type.OBJECT, Type.ARRAY -> aload(ii + 1)
                                    Type.FLOAT -> fload(ii + 1)
                                    Type.DOUBLE -> dload(ii + 1)
                                    Type.LONG -> lload(ii + 1)
                                }
                                ii += t.size
                            }
                            invokevirtual(cn.name, mn.name, mn.desc)
                        }
                        when (type.returnType.sort) {
                            Type.INT, Type.CHAR, Type.BYTE, Type.BOOLEAN, Type.SHORT -> ireturn()
                            Type.OBJECT, Type.ARRAY -> areturn()
                            Type.FLOAT -> freturn()
                            Type.DOUBLE -> dreturn()
                            Type.LONG -> lreturn()
                            Type.VOID -> _return()
                        }
                    }
                }
                val modification = Modification(cn, mn, proxy, mods)
                modifications += modification
                proxies += proxy

            }
            cn.methods.addAll(proxies)
            proxiesSet.addAll(proxies.map { "${cn.name} ${it.name} ${it.desc}" })
        }
        val modMap = modifications.map { (cn, mn, proxy, md) ->
            "${cn.name} ${mn.name} ${mn.desc}" to (proxy to md)
        }.toMap(hashMapOf())

        forClass(classes) { cn ->
            forMethod(cn) { mn ->
                if (proxiesSet.contains("${cn.name} ${mn.name} ${mn.desc}"))
                    return@forMethod
                val a = Analyzer(SourceInterpreter())
                val analyzed by lazy {
                    a.analyze(cn.name, mn)
                }
                for ((idx, insn) in mn.instructions.iterator().withIndex()) {
                    if (insn is MethodInsnNode) {
                        val (proxy, md) = modMap["${insn.owner} ${insn.name} ${insn.desc}"] ?: continue
                        val frame = analyzed[idx] ?: continue
                        val methodType = Type.getType(insn.desc)
                        val listArgumentStacks = arrayListOf<SourceValue?>()
                        repeat(methodType.argumentTypes.size) {
                            listArgumentStacks.add(frame.pop())
                        }
                        var flags = 0
                        var i: Int
                        var i2 = 0
                        for (value in listArgumentStacks.reversed()) {
                            i = i2
                            i2 += value?.size ?: 0
                            value ?: continue
                            val constantInsn = value.insns.singleOrNull() ?: continue
                            val (modType, operand) = md[i] ?: continue
                            val actualVal = constantInsn.constantValue() ?: continue

                            val replace = when (modType) {
                                0 -> ldcInt(if (actualVal as Int == 0) 1 else 0)
                                1 -> ldcInt(actualVal as Int + operand!!)
                                2 -> ldcInt(actualVal as Int - operand!!)
                                3 -> LdcInsnNode(actualVal as String + NameGenerator.randomFixedSizeString(operand!!))
                                else -> error("unreachable")
                            }
                            flags = flags or (1 shl i)
                            mn.instructions.insertBefore(constantInsn, replace)
                            mn.instructions.remove(constantInsn)
                        }
                        mn.instructions.insertBefore(insn, ldcInt(flags))
                        insn.name = proxy.name
                        insn.desc = proxy.desc
                    }
                }
            }
        }
    }
}
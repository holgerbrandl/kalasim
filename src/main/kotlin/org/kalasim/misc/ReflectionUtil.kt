package org.kalasim

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

/** Fast check: Sequence<component>, and rejects any projections (out/in). */

fun KType.isSequenceOf(component: KClass<*>, exactNullability: Boolean = false): Boolean {
    val cls = classifier as? KClass<*> ?: return false
    if (cls != Sequence::class) return false

    // Optional: enforce Sequence<...> nullability
    if (exactNullability && isMarkedNullable) return false

    val arg = arguments.singleOrNull()?.type ?: return false
    val argCls = arg.classifier as? KClass<*> ?: return false

    if (argCls != component) return false

    // Optional: enforce Component nullability (Component? vs Component)
    if (exactNullability && arg.isMarkedNullable) return false

    return true
}


private data class ElemKey(
    val exactNullability: Boolean
)

private val seqTypeCacheTL: ThreadLocal<IdentityHashMap<KType, Boolean>> =
    ThreadLocal.withInitial { IdentityHashMap() }

fun returnsSequenceOfCached(returnType: KType): Boolean {
    val perThread = seqTypeCacheTL.get()
    return perThread.getOrPut(returnType) { returnType.isSequenceOf(Component::class) }
}



internal data class OverrideFlags(
    val overridesProcess: Boolean,
    val overridesRepeated: Boolean
)

internal val overrideCacheTL = ThreadLocal.withInitial {
    HashMap<Class<out Component>, OverrideFlags>(64)
}

@Suppress("UNCHECKED_CAST")
internal fun overrideFlagsTL(clazz: Class<out Component>): OverrideFlags {
    val cache = overrideCacheTL.get()
    return cache[clazz] ?: run {

        // Component itself should NOT count as "overriding"
        if (clazz == Component::class.java) {
            val flags = OverrideFlags(overridesProcess = false, overridesRepeated = false)
            cache[clazz] = flags
            return flags
        }

        val declared = clazz.declaredMethods

        fun declaresNoArg(name: String): Boolean =
            declared.any { it.name == name && it.parameterCount == 0 }

        val flags = OverrideFlags(
            overridesProcess = declaresNoArg("process"),
            overridesRepeated = declaresNoArg("repeatedProcess"),
        )

        cache[clazz] = flags
        flags
    }
}
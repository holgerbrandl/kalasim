package org.kalasim

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.util.IdentityHashMap
import kotlin.jvm.internal.CallableReference
import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

internal class KFunctionInvoker(
    private val spreader: MethodHandle,   // expects a single Array<Any?> argument (asSpreader)
    val needsInstance: Boolean,
    private val arity: Int
) {
    private val argsTL = ThreadLocal.withInitial { arrayOfNulls<Any?>(arity) }

    fun call(instance: Any?, args: Array<Any?>): Any? {
        val all = argsTL.get()
        var i = 0

        if (needsInstance) {
            requireNotNull(instance) { "Receiver instance required for non-static method" }
            all[i++] = instance
        }

        val expected = arity - if (needsInstance) 1 else 0
        require(args.size == expected) { "Expected $expected arguments, got ${args.size}" }

        System.arraycopy(args, 0, all, i, args.size)
        return spreader.invoke(all)
    }

    companion object {
        private fun unreflectFast(m: Method): MethodHandle {
            val base = MethodHandles.lookup()
            val lookup = try {
                MethodHandles.privateLookupIn(m.declaringClass, base)
            } catch (_: Throwable) {
                base
            }

            // First try without setAccessible
            return try {
                lookup.unreflect(m)
            } catch (_: IllegalAccessException) {
                // Fallback: one-time setAccessible (ok because we cache the result)
                m.isAccessible = true
                lookup.unreflect(m)
            }
        }

        fun fromJavaMethod(m: Method): KFunctionInvoker {
            val mh = unreflectFast(m)
            val pcount = mh.type().parameterCount()
            val spreader = mh.asSpreader(Array<Any?>::class.java, pcount)
            val needsInstance = !java.lang.reflect.Modifier.isStatic(m.modifiers)
            return KFunctionInvoker(spreader, needsInstance, pcount)
        }
    }
}

// -------- fast, stable key for callable references (no javaMethod / no toString) --------

private fun KFunction<*>.fastCallableKeyOrNull(): String? {
    val cr = this as? CallableReference ?: return null

    val ownerName = when (val o = cr.owner) {
        is ClassBasedDeclarationContainer -> o.jClass.name
        null -> "<no-owner>"
        else -> o.toString()
    }

    // Example: "org.kalasim.Component#process()Lkotlin/sequences/Sequence;"
    return ownerName + "#" + cr.name + cr.signature
}

// -------- caches --------

// Main cache: per-thread, keyed by stable callable signature (dedupes across different instances)
private val invokerCacheBySigTL: ThreadLocal<HashMap<String, KFunctionInvoker>> =
    ThreadLocal.withInitial { HashMap() }

// Fallback cache: per-thread, identity keyed (for KFunctions that aren't CallableReference)
private val invokerCacheByFnTL: ThreadLocal<IdentityHashMap<KFunction<*>, KFunctionInvoker>> =
    ThreadLocal.withInitial { IdentityHashMap() }

internal fun KFunction<*>.cachedInvoker(): KFunctionInvoker {
    // Fast path: callable references (your typical ::process / this::process cases)
    fastCallableKeyOrNull()?.let { key ->
        val cache = invokerCacheBySigTL.get()
        return cache.getOrPut(key) {
            val m = this.javaMethod ?: error("No javaMethod for $this (non-JVM function?)")
            KFunctionInvoker.fromJavaMethod(m)
        }
    }

    // Fallback path
    val cache2 = invokerCacheByFnTL.get()
    return cache2.getOrPut(this) {
        val m = this.javaMethod ?: error("No javaMethod for $this (non-JVM function?)")
        KFunctionInvoker.fromJavaMethod(m)
    }
}

// Optional: clear per-thread caches
internal fun clearInvokerCachesForThread() {
    invokerCacheBySigTL.get().clear()
    invokerCacheByFnTL.get().clear()
}
